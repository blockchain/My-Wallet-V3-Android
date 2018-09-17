package piuk.blockchain.androidcore.data.payments

import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.exceptions.ApiException
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Observable
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.ECKey
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.annotations.WebRequest
import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.util.HashMap

class PaymentService(
    private val environmentSettings: EnvironmentConfig,
    private val payment: Payment
) {

    /**
     * Submits a BTC payment to a specified Bitcoin address and returns the transaction hash if
     * successful
     *
     * @param unspentOutputBundle UTXO object
     * @param keys A List of elliptic curve keys
     * @param toAddress The Bitcoin address to send the funds to
     * @param changeAddress A change address
     * @param bigIntFee The specified fee amount
     * @param bigIntAmount The actual transaction amount
     * @return An [Observable] wrapping a [String] where the String is the transaction hash
     */
    @WebRequest
    internal fun submitPayment(
        unspentOutputBundle: SpendableUnspentOutputs,
        keys: List<ECKey>,
        toAddress: String,
        changeAddress: String,
        bigIntFee: BigInteger,
        bigIntAmount: BigInteger
    ): Observable<String> {

        return Observable.create { observableOnSubscribe ->
            val receivers = HashMap<String, BigInteger>()
            receivers[toAddress] = bigIntAmount

            val tx = payment.makeSimpleTransaction(
                environmentSettings.bitcoinNetworkParameters,
                unspentOutputBundle.spendableOutputs,
                receivers,
                bigIntFee,
                changeAddress
            )

            payment.signSimpleTransaction(environmentSettings.bitcoinNetworkParameters, tx, keys)

            val exe = payment.publishSimpleTransaction(tx).execute()

            if (exe.isSuccessful) {
                if (!observableOnSubscribe.isDisposed) {
                    observableOnSubscribe.onNext(tx.hashAsString)
                    observableOnSubscribe.onComplete()
                }
            } else {
                if (!observableOnSubscribe.isDisposed) {
                    observableOnSubscribe.onError(Throwable("""${exe.code()}: ${exe.errorBody()!!.string()}"""))
                }
            }
        }
    }

    /**
     * Submits a BCH payment to a specified Bitcoin Cash address and returns the transaction hash if
     * successful
     *
     * @param unspentOutputBundle UTXO object
     * @param keys A List of elliptic curve keys
     * @param toAddress The Bitcoin Cash address to send the funds to
     * @param changeAddress A change address
     * @param bigIntFee The specified fee amount
     * @param bigIntAmount The actual transaction amount
     * @return An [Observable] wrapping a [String] where the String is the transaction hash
     */
    @WebRequest
    internal fun submitBchPayment(
        unspentOutputBundle: SpendableUnspentOutputs,
        keys: List<ECKey>,
        toAddress: String,
        changeAddress: String,
        bigIntFee: BigInteger,
        bigIntAmount: BigInteger
    ): Observable<String> {

        return Observable.create { observableOnSubscribe ->
            val receivers = HashMap<String, BigInteger>()
            receivers[toAddress] = bigIntAmount

            val tx = payment.makeSimpleTransaction(
                environmentSettings.bitcoinCashNetworkParameters,
                unspentOutputBundle.spendableOutputs,
                receivers,
                bigIntFee,
                changeAddress
            )

            payment.signBCHTransaction(environmentSettings.bitcoinCashNetworkParameters, tx, keys)

            val exe = payment.publishSimpleBchTransaction(tx).execute()

            if (exe.isSuccessful) {
                if (!observableOnSubscribe.isDisposed) {
                    observableOnSubscribe.onNext(tx.hashAsString)
                    observableOnSubscribe.onComplete()
                }
            } else {
                if (!observableOnSubscribe.isDisposed) {
                    observableOnSubscribe.onError(Throwable("""${exe.code()}: ${exe.errorBody()!!.string()}"""))
                }
            }
        }
    }

    /**
     * Returns an [UnspentOutputs] object containing all the unspent outputs for a given
     * Bitcoin address.
     *
     * @param address The BTC address you wish to query, as a String
     * @return An [Observable] wrapping an [UnspentOutputs] object
     */
    @WebRequest
    internal fun getUnspentOutputs(address: String): Observable<UnspentOutputs> {
        return Observable.fromCallable {
            val response = payment.getUnspentCoins(listOf(address)).execute()

            when {
                response.isSuccessful -> response.body()
                response.code() == 500 -> // If no unspent outputs available server responds with 500
                    UnspentOutputs.fromJson("{\"unspent_outputs\":[]}")
                else -> throw ApiException(response.code().toString())
            }
        }
    }

    /**
     * Returns an [UnspentOutputs] object containing all the unspent outputs for a given
     * Bitcoin Cash address. Please note that this method only accepts a valid Base58 (ie Legacy)
     * BCH address. BECH32 is not accepted by the endpoint.
     *
     * @param address The BCH address you wish to query, as a Base58 address String
     * @return An [Observable] wrapping an [UnspentOutputs] object
     */
    @WebRequest
    internal fun getUnspentBchOutputs(address: String): Observable<UnspentOutputs> {
        return Observable.fromCallable {
            val response = payment.getUnspentBchCoins(listOf(address)).execute()

            when {
                response.isSuccessful -> response.body()
                response.code() == 500 -> // If no unspent outputs available server responds with 500
                    UnspentOutputs.fromJson("{\"unspent_outputs\":[]}")
                else -> throw ApiException(response.code().toString())
            }
        }
    }

    /**
     * Returns a [SpendableUnspentOutputs] object from a given [UnspentOutputs] object,
     * given the payment amount and the current fee per kB. This method selects the minimum number
     * of inputs necessary to allow a successful payment by selecting from the largest inputs
     * first.
     *
     * @param unspentCoins The addresses' [UnspentOutputs]
     * @param paymentAmount The amount you wish to send, as a [BigInteger]
     * @param feePerKb The current fee per kB, as a [BigInteger]
     * @return An [SpendableUnspentOutputs] object, which wraps a list of spendable outputs
     * for the given inputs
     */
    @Throws(UnsupportedEncodingException::class)
    internal fun getSpendableCoins(
        unspentCoins: UnspentOutputs,
        paymentAmount: BigInteger,
        feePerKb: BigInteger
    ): SpendableUnspentOutputs = payment.getSpendableCoins(unspentCoins, paymentAmount, feePerKb)

    /**
     * Calculates the total amount of bitcoin that can be swept from an [UnspentOutputs]
     * object and returns the amount that can be recovered, along with the fee (in absolute terms)
     * necessary to sweep those coins.
     *
     * @param unspentCoins An [UnspentOutputs] object that you wish to sweep
     * @param feePerKb The current fee per kB on the network
     * @return A [Pair] object, where left = the sweepable amount as a [BigInteger],
     * right = the absolute fee needed to sweep those coins, also as a [BigInteger]
     */
    internal fun getMaximumAvailable(
        unspentCoins: UnspentOutputs,
        feePerKb: BigInteger
    ): Pair<BigInteger, BigInteger> = payment.getMaximumAvailable(unspentCoins, feePerKb)

    /**
     * Returns true if the `absoluteFee` is adequate for the number of inputs/outputs in the
     * transaction.
     *
     * @param inputs The number of inputs
     * @param outputs The number of outputs
     * @param absoluteFee The absolute fee as a [BigInteger]
     * @return True if the fee is adequate, false if not
     */
    internal fun isAdequateFee(inputs: Int, outputs: Int, absoluteFee: BigInteger): Boolean =
        payment.isAdequateFee(inputs, outputs, absoluteFee)

    /**
     * Returns the estimated size of the transaction in kB.
     *
     * @param inputs The number of inputs
     * @param outputs The number of outputs
     * @return The estimated size of the transaction in kB
     */
    internal fun estimateSize(inputs: Int, outputs: Int): Int =
        payment.estimatedSize(inputs, outputs)

    /**
     * Returns an estimated absolute fee in satoshis (as a [BigInteger] for a given number of
     * inputs and outputs.
     *
     * @param inputs The number of inputs
     * @param outputs The number of outputs
     * @param feePerKb The current fee per kB om the network
     * @return A [BigInteger] representing the absolute fee
     */
    internal fun estimateFee(inputs: Int, outputs: Int, feePerKb: BigInteger): BigInteger =
        payment.estimatedFee(inputs, outputs, feePerKb)
}

package piuk.blockchain.androidcore.data.payments

import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Observable
import org.apache.commons.lang3.tuple.Pair
import org.bitcoinj.core.ECKey
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.BIP38PrivateKey
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import java.io.UnsupportedEncodingException
import java.math.BigInteger

class SendDataManager(
    private val paymentService: PaymentService,
    rxBus: RxBus
) {

    private val rxPinning: RxPinning = RxPinning(rxBus)

    /**
     * Submits a Bitcoin payment to a specified BTC address and returns the transaction hash if
     * successful
     *
     * @param unspentOutputBundle UTXO object
     * @param keys A List of elliptic curve keys
     * @param toAddress The address to send the funds to
     * @param changeAddress A change address
     * @param bigIntFee The specified fee amount
     * @param bigIntAmount The actual transaction amount
     * @return An [Observable] wrapping a [String] where the String is the transaction hash
     */
    fun submitBtcPayment(
        unspentOutputBundle: SpendableUnspentOutputs,
        keys: List<ECKey>,
        toAddress: String,
        changeAddress: String,
        bigIntFee: BigInteger,
        bigIntAmount: BigInteger
    ): Observable<String> {

        return rxPinning.call<String> {
            paymentService.submitPayment(
                unspentOutputBundle,
                keys,
                toAddress,
                changeAddress,
                bigIntFee,
                bigIntAmount
            )
        }.applySchedulers()
    }

    /**
     * Submits a Bitcoin Cash payment to a specified BCH address and returns the transaction hash if
     * successful
     *
     * @param unspentOutputBundle UTXO object
     * @param keys A List of elliptic curve keys
     * @param toAddress The address to send the funds to
     * @param changeAddress A change address
     * @param bigIntFee The specified fee amount
     * @param bigIntAmount The actual transaction amount
     * @return An [Observable] wrapping a [String] where the String is the transaction hash
     */
    fun submitBchPayment(
        unspentOutputBundle: SpendableUnspentOutputs,
        keys: List<ECKey>,
        toAddress: String,
        changeAddress: String,
        bigIntFee: BigInteger,
        bigIntAmount: BigInteger
    ): Observable<String> {

        return rxPinning.call<String> {
            paymentService.submitBchPayment(
                unspentOutputBundle,
                keys,
                toAddress,
                changeAddress,
                bigIntFee,
                bigIntAmount
            )
        }.applySchedulers()
    }

    /**
     * Returns an Elliptic Curve Key from a BIP38 private key.
     *
     * @param password The password for the BIP-38 encrypted key
     * @param scanData A private key in Base-58
     * @param networkParameters The current Network Parameters
     * @return An [ECKey]
     */
    fun getEcKeyFromBip38(
        password: String,
        scanData: String,
        networkParameters: NetworkParameters
    ): Observable<ECKey> = Observable.fromCallable {
        BIP38PrivateKey.fromBase58(networkParameters, scanData).run { decrypt(password) }
    }.applySchedulers()

    /**
     * Returns an [UnspentOutputs] object containing all the unspent outputs for a given
     * Bitcoin address.
     *
     * @param address The Bitcoin address you wish to query, as a String
     * @return An [Observable] wrapping an [UnspentOutputs] object
     */
    fun getUnspentOutputs(address: String): Observable<UnspentOutputs> =
        rxPinning.call<UnspentOutputs> { paymentService.getUnspentOutputs(address) }
            .applySchedulers()

    /**
     * Returns an [UnspentOutputs] object containing all the unspent outputs for a given
     * Bitcoin Cash address. Please note that this method only accepts a valid Base58 (ie Legacy)
     * BCH address. BECH32 is not accepted by the endpoint.
     *
     * @param address The Bitcoin Cash address you wish to query, as a Base58 address String
     * @return An [Observable] wrapping an [UnspentOutputs] object
     */
    fun getUnspentBchOutputs(address: String): Observable<UnspentOutputs> =
        rxPinning.call<UnspentOutputs> { paymentService.getUnspentBchOutputs(address) }
            .applySchedulers()

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
    fun getSpendableCoins(
        unspentCoins: UnspentOutputs,
        paymentAmount: BigInteger,
        feePerKb: BigInteger
    ): SpendableUnspentOutputs = paymentService.getSpendableCoins(
        unspentCoins,
        paymentAmount,
        feePerKb
    )

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
    fun getMaximumAvailable(
        unspentCoins: UnspentOutputs,
        feePerKb: BigInteger
    ): Pair<BigInteger, BigInteger> = paymentService.getMaximumAvailable(unspentCoins, feePerKb)

    /**
     * Returns true if the `absoluteFee` is adequate for the number of inputs/outputs in the
     * transaction.
     *
     * @param inputs The number of inputs
     * @param outputs The number of outputs
     * @param absoluteFee The absolute fee as a [BigInteger]
     * @return True if the fee is adequate, false if not
     */
    fun isAdequateFee(inputs: Int, outputs: Int, absoluteFee: BigInteger): Boolean =
        paymentService.isAdequateFee(inputs, outputs, absoluteFee)

    /**
     * Returns the estimated size of the transaction in kB.
     *
     * @param inputs The number of inputs
     * @param outputs The number of outputs
     * @return The estimated size of the transaction in kB
     */
    fun estimateSize(inputs: Int, outputs: Int): Int = paymentService.estimateSize(inputs, outputs)

    /**
     * Returns an estimated absolute fee in satoshis (as a [BigInteger] for a given number of
     * inputs and outputs.
     *
     * @param inputs The number of inputs
     * @param outputs The number of outputs
     * @param feePerKb The current fee per kB om the network
     * @return A [BigInteger] representing the absolute fee
     */
    fun estimatedFee(inputs: Int, outputs: Int, feePerKb: BigInteger): BigInteger =
        paymentService.estimateFee(inputs, outputs, feePerKb)
}

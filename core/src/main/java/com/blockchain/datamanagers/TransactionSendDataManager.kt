package com.blockchain.datamanagers

import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.ethereum.EthereumAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.bitcoinj.core.ECKey
import org.web3j.utils.Convert
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.ethereum.EthereumAccountWrapper
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import java.math.BigInteger

class TransactionSendDataManager(
    private val payloadDataManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val sendDataManager: SendDataManager,
    private val ethereumAccountWrapper: EthereumAccountWrapper
) {

    fun executeTransaction(
        amount: CryptoValue,
        destination: String,
        account: JsonSerializableAccount,
        fees: FeeOptions,
        feeType: FeeType = FeeType.Regular
    ): Single<String> = when (amount.currency) {
        CryptoCurrency.BTC -> sendBtcTransaction(
            amount,
            destination,
            account as Account,
            fees.feeForType(feeType)
        )
        CryptoCurrency.ETHER -> sendEthTransaction(
            amount,
            destination,
            account as EthereumAccount,
            fees
        )
        CryptoCurrency.BCH -> sendBchTransaction(
            amount,
            destination,
            account as GenericMetadataAccount,
            fees.feeForType(feeType)
        )
    }

    private fun sendBtcTransaction(
        amount: CryptoValue,
        destination: String,
        account: Account,
        feePerKb: BigInteger
    ): Single<String> = sendBitcoinStyleTransaction(
        amount,
        destination,
        account,
        feePerKb,
        account.getChangeAddress()
    )

    private fun sendBchTransaction(
        amount: CryptoValue,
        destination: String,
        account: GenericMetadataAccount,
        feePerKb: BigInteger
    ): Single<String> = sendBitcoinStyleTransaction(
        amount,
        destination,
        account.getHdAccount(),
        feePerKb,
        account.getChangeAddress()
    )

    private fun sendBitcoinStyleTransaction(
        amount: CryptoValue,
        destination: String,
        account: Account,
        feePerKb: BigInteger,
        changeAddress: Single<String>
    ): Single<String> = getSpendableCoins(account.xpub, amount, feePerKb)
        .flatMap { spendable ->
            getSigningKeys(account, spendable)
                .flatMap { signingKeys ->
                    changeAddress
                        .flatMap {
                            submitBitcoinStylePayment(
                                amount,
                                spendable,
                                signingKeys,
                                destination,
                                it,
                                feePerKb
                            )
                        }
                }
        }

    private fun sendEthTransaction(
        amount: CryptoValue,
        destination: String,
        account: EthereumAccount,
        fees: FeeOptions
    ): Single<String> = ethDataManager.fetchEthAddress()
        .map {
            ethDataManager.createEthTransaction(
                nonce = ethDataManager.getEthResponseModel()!!.getNonce(),
                to = destination,
                gasPrice = fees.regularFee.gasPriceToWei(),
                gasLimit = fees.gasLimit.toBigInteger(),
                weiValue = amount.amount
            )
        }
        .map {
            account.signTransaction(
                it,
                ethereumAccountWrapper.deriveECKey(payloadDataManager.masterKey, 0)
            )
        }
        .flatMap { ethDataManager.pushEthTx(it) }
        .flatMap { ethDataManager.setLastTxHashObservable(it, System.currentTimeMillis()) }
        .subscribeOn(Schedulers.io())
        .singleOrError()

    private fun getSpendableCoins(
        address: String,
        amount: CryptoValue,
        feePerKb: BigInteger
    ): Single<SpendableUnspentOutputs> = getUnspentOutputs(address, amount)
        .subscribeOn(Schedulers.io())
        .map { sendDataManager.getSpendableCoins(it, amount.amount, feePerKb) }

    private fun getUnspentOutputs(address: String, amount: CryptoValue): Single<UnspentOutputs> =
        when (amount.currency) {
            CryptoCurrency.BTC -> sendDataManager.getUnspentOutputs(address)
            CryptoCurrency.BCH -> sendDataManager.getUnspentBchOutputs(address)
            CryptoCurrency.ETHER -> throw IllegalArgumentException("Ether does not have unspent outputs")
        }.subscribeOn(Schedulers.io())
            .singleOrError()

    private fun submitBitcoinStylePayment(
        amount: CryptoValue,
        unspent: SpendableUnspentOutputs,
        signingKeys: List<ECKey>,
        depositAddress: String,
        changeAddress: String,
        feePerKb: BigInteger
    ): Single<String> = when (amount.currency) {
        CryptoCurrency.BTC -> sendDataManager.submitBtcPayment(
            unspent,
            signingKeys,
            depositAddress,
            changeAddress,
            feePerKb,
            amount.amount
        )
        CryptoCurrency.BCH -> sendDataManager.submitBchPayment(
            unspent,
            signingKeys,
            depositAddress,
            changeAddress,
            feePerKb,
            amount.amount
        )
        CryptoCurrency.ETHER -> throw IllegalArgumentException("Ether not supported by this method")
    }.subscribeOn(Schedulers.io())
        .singleOrError()

    private fun getSigningKeys(
        account: Account,
        spendable: SpendableUnspentOutputs
    ): Single<List<ECKey>> =
        Single.just(payloadDataManager.getHDKeysForSigning(account, spendable))

    private fun Account.getChangeAddress(): Single<String> =
        payloadDataManager.getNextChangeAddress(this).singleOrError()

    private fun GenericMetadataAccount.getChangeAddress(): Single<String> {
        val position = bchDataManager.getActiveAccounts()
            .indexOfFirst { it.xpub == this.xpub }
        return bchDataManager.getNextChangeAddress(position).singleOrError()
    }

    private fun FeeOptions.feeForType(feeType: FeeType): BigInteger = when (feeType) {
        FeeType.Regular -> regularFee
        FeeType.Priority -> priorityFee
    }.toBigInteger()

    private fun GenericMetadataAccount.getHdAccount(): Account =
        payloadDataManager.getAccountForXPub(this.xpub)
}

internal fun Long.gasPriceToWei(): BigInteger =
    Convert.toWei(this.toBigDecimal(), Convert.Unit.GWEI).toBigInteger()

sealed class FeeType {
    object Regular : FeeType()
    object Priority : FeeType()
}
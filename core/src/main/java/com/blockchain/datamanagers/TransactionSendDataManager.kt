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
import timber.log.Timber
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

    fun getMaximumSpendable(
        cryptoCurrency: CryptoCurrency,
        account: JsonSerializableAccount,
        fees: FeeOptions,
        feeType: FeeType = FeeType.Regular
    ): Single<CryptoValue> = when (cryptoCurrency) {
        CryptoCurrency.BTC -> getMaxBitcoin(account as Account, fees, feeType)
        CryptoCurrency.BCH -> getMaxBitcoinCash(account as GenericMetadataAccount, fees, feeType)
        CryptoCurrency.ETHER -> getMaxEther(fees)
    }

    fun getFeeForTransaction(
        amount: CryptoValue,
        account: JsonSerializableAccount,
        fees: FeeOptions,
        feeType: FeeType = FeeType.Regular
    ): Single<CryptoValue> = when (amount.currency) {
        CryptoCurrency.BTC -> calculateBtcFee(
            account as Account,
            amount,
            fees.feeForType(feeType)
        )
        CryptoCurrency.BCH -> calculateBchFee(
            account as GenericMetadataAccount,
            amount,
            fees.feeForType(feeType)
        )
        CryptoCurrency.ETHER -> calculateEtherFee(fees).just()
    }

    fun getChangeAddress(
        cryptoCurrency: CryptoCurrency,
        account: JsonSerializableAccount
    ): Single<String> =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> (account as Account).getChangeAddress()
            CryptoCurrency.BCH -> (account as GenericMetadataAccount).getChangeAddress()
            CryptoCurrency.ETHER -> (account as EthereumAccount).address.just()
        }

    fun getReceiveAddress(
        cryptoCurrency: CryptoCurrency,
        account: JsonSerializableAccount
    ): Single<String> =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> (account as Account).getReceiveAddress()
            CryptoCurrency.BCH -> (account as GenericMetadataAccount).getReceiveAddress()
            CryptoCurrency.ETHER -> (account as EthereumAccount).address.just()
        }

    private fun calculateBtcFee(
        account: Account,
        amount: CryptoValue,
        feePerKb: BigInteger
    ): Single<CryptoValue> = calculateBtcOrBchAbsoluteFee(account.xpub, amount, feePerKb)
        .map { CryptoValue.bitcoinFromSatoshis(it) }

    private fun calculateBchFee(
        account: GenericMetadataAccount,
        amount: CryptoValue,
        feePerKb: BigInteger
    ): Single<CryptoValue> = calculateBtcOrBchAbsoluteFee(account.xpub, amount, feePerKb)
        .map { CryptoValue.bitcoinCashFromSatoshis(it) }

    private fun calculateBtcOrBchAbsoluteFee(
        xPub: String,
        amount: CryptoValue,
        feePerKb: BigInteger
    ): Single<BigInteger> = getUnspentOutputs(xPub, amount.currency)
        .map { getSuggestedAbsoluteFee(it, amount.amount, feePerKb) }

    private fun getSuggestedAbsoluteFee(
        coins: UnspentOutputs,
        amountToSend: BigInteger,
        feePerKb: BigInteger
    ): BigInteger = sendDataManager.getSpendableCoins(coins, amountToSend, feePerKb).absoluteFee

    private fun getMaxBitcoin(
        account: Account,
        fees: FeeOptions,
        feeType: FeeType
    ): Single<CryptoValue> = getMaxBchOrBtc(account.xpub, CryptoCurrency.BTC, fees, feeType)
        .map { CryptoValue.bitcoinFromSatoshis(it) }
        .doOnError { Timber.e(it) }
        .onErrorReturn { CryptoValue.ZeroBtc }

    private fun getMaxBitcoinCash(
        account: GenericMetadataAccount,
        fees: FeeOptions,
        feeType: FeeType
    ): Single<CryptoValue> = getMaxBchOrBtc(account.xpub, CryptoCurrency.BCH, fees, feeType)
        .map { CryptoValue.bitcoinCashFromSatoshis(it) }
        .doOnError { Timber.e(it) }
        .onErrorReturn { CryptoValue.ZeroBch }

    private fun getMaxBchOrBtc(
        xPub: String,
        cryptoCurrency: CryptoCurrency,
        fees: FeeOptions,
        feeType: FeeType
    ): Single<BigInteger> = getUnspentOutputs(xPub, cryptoCurrency)
        .map { sendDataManager.getMaximumAvailable(it, fees.toSatoshis(feeType)).left }

    private fun getMaxEther(fees: FeeOptions): Single<CryptoValue> =
        ethDataManager.fetchEthAddress()
            .map {
                val wei = calculateEtherFee(fees).amount
                return@map (it.getAddressResponse()!!.balance - wei).max(BigInteger.ZERO)
            }
            .map { CryptoValue.etherFromWei(it) }
            .doOnError { Timber.e(it) }
            .onErrorReturn { CryptoValue.ZeroEth }
            .singleOrError()

    private fun calculateEtherFee(fees: FeeOptions): CryptoValue {
        val wei = (fees.regularFee * fees.gasLimit).gweiToWei()
        return CryptoValue.etherFromWei(wei)
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
                gasPrice = fees.regularFee.gweiToWei(),
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
    ): Single<SpendableUnspentOutputs> = getUnspentOutputs(address, amount.currency)
        .subscribeOn(Schedulers.io())
        .map { sendDataManager.getSpendableCoins(it, amount.amount, feePerKb) }

    private fun getUnspentOutputs(
        address: String,
        currency: CryptoCurrency
    ): Single<UnspentOutputs> =
        when (currency) {
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
        payloadDataManager.getHDKeysForSigning(account, spendable).just()

    private fun Account.getChangeAddress(): Single<String> =
        payloadDataManager.getNextChangeAddress(this).singleOrError()

    private fun Account.getReceiveAddress(): Single<String> =
        payloadDataManager.getNextReceiveAddress(this).singleOrError()

    private fun GenericMetadataAccount.getChangeAddress(): Single<String> {
        val position = bchDataManager.getActiveAccounts()
            .indexOfFirst { it.xpub == this.xpub }
        return bchDataManager.getNextChangeAddress(position).singleOrError()
    }

    private fun GenericMetadataAccount.getReceiveAddress(): Single<String> {
        val position = bchDataManager.getActiveAccounts()
            .indexOfFirst { it.xpub == this.xpub }
        return bchDataManager.getNextReceiveAddress(position).singleOrError()
    }

    private fun FeeOptions.feeForType(feeType: FeeType): BigInteger = when (feeType) {
        FeeType.Regular -> regularFee
        FeeType.Priority -> priorityFee
    }.toBigInteger()

    private fun GenericMetadataAccount.getHdAccount(): Account =
        payloadDataManager.getAccountForXPub(this.xpub)

    private fun <T> T.just(): Single<T> = Single.just(this)
}

internal fun Long.gweiToWei(): BigInteger =
    Convert.toWei(this.toBigDecimal(), Convert.Unit.GWEI).toBigInteger()

internal fun FeeOptions.toSatoshis(feeType: FeeType): BigInteger = when (feeType) {
    FeeType.Regular -> regularFee * 1000
    FeeType.Priority -> priorityFee * 1000
}.toBigInteger()

sealed class FeeType {
    object Regular : FeeType()
    object Priority : FeeType()
}
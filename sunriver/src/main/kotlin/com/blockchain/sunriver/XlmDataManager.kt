package com.blockchain.sunriver

import com.blockchain.account.BalanceAndMin
import com.blockchain.account.DefaultAccountDataManager
import com.blockchain.balance.AsyncAccountBalanceReporter
import com.blockchain.balance.AsyncAddressBalanceReporter
import com.blockchain.sunriver.datamanager.XlmAccount
import com.blockchain.sunriver.datamanager.XlmMetaData
import com.blockchain.sunriver.datamanager.XlmMetaDataInitializer
import com.blockchain.sunriver.datamanager.default
import com.blockchain.sunriver.models.XlmTransaction
import com.blockchain.transactions.SendConfirmationDetails
import com.blockchain.transactions.SendDetails
import com.blockchain.transactions.SendFundsResult
import com.blockchain.transactions.TransactionSender
import com.blockchain.utils.toHex
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers

class XlmDataManager internal constructor(
    private val horizonProxy: HorizonProxy,
    metaDataInitializer: XlmMetaDataInitializer,
    private val xlmSecretAccess: XlmSecretAccess,
    private val memoMapper: MemoMapper
) : TransactionSender,
    DefaultAccountDataManager,
    AsyncAddressBalanceReporter,
    AsyncAccountBalanceReporter {

    override fun sendFunds(
        sendDetails: SendDetails
    ): Single<SendFundsResult> =
        Maybe.defer { xlmSecretAccess.getPrivate(HorizonKeyPair.Public(sendDetails.fromXlm.accountId)) }
            .map { private ->
                horizonProxy.sendTransaction(
                    private.toKeyPair(),
                    sendDetails.toAddress,
                    sendDetails.value,
                    memoMapper.mapMemo(sendDetails.memo)
                )
            }
            .map { it.mapToSendFundsResult(sendDetails) }
            .toSingle()

    override fun dryRunSendFunds(
        sendDetails: SendDetails
    ): Single<SendFundsResult> =
        Maybe.fromCallable {
            horizonProxy.dryRunTransaction(
                HorizonKeyPair.Public(sendDetails.fromXlm.accountId).toKeyPair(),
                sendDetails.toAddress,
                sendDetails.value,
                memoMapper.mapMemo(sendDetails.memo)
            ).mapToSendFundsResult(sendDetails)
        }.toSingle()
            .subscribeOn(Schedulers.io())

    private val wallet = Single.defer { metaDataInitializer.initWalletMaybePrompt.toSingle() }
    private val maybeWallet = Maybe.defer { metaDataInitializer.initWalletMaybe }

    override fun getBalance(address: String): Single<CryptoValue> =
        Single.fromCallable { horizonProxy.getBalance(address) }
            .subscribeOn(Schedulers.io())

    fun getBalance(accountReference: AccountReference.Xlm): Single<CryptoValue> =
        getBalance(accountReference.accountId)

    override fun balanceOf(accountReference: AccountReference): Maybe<CryptoValue> {
        if (accountReference.cryptoCurrency != CryptoCurrency.XLM) {
            return Maybe.empty()
        }
        return getBalance(accountReference as AccountReference.Xlm).toMaybe()
    }

    private fun getBalanceAndMin(accountReference: AccountReference.Xlm): Single<BalanceAndMin> =
        Single.fromCallable { horizonProxy.getBalanceAndMin(accountReference.accountId) }
            .subscribeOn(Schedulers.io())

    fun getBalance(): Single<CryptoValue> =
        Maybe.concat(
            maybeDefaultAccount().flatMap { getBalance(it).toMaybe() },
            Maybe.just(CryptoValue.ZeroXlm)
        ).firstOrError()

    fun fees() = CryptoValue.lumensFromStroop(100.toBigInteger()) // Tech debt AND-1663 Repeated Hardcoded fee

    /**
     * Balance - minimum - fees
     */
    override fun getMaxSpendableAfterFees(): Single<CryptoValue> =
        Maybe.concat(
            maybeDefaultAccount().flatMap {
                getBalanceAndMin(it).map { it.balance - it.minimumBalance - fees() }.toMaybe()
            },
            Maybe.just(CryptoValue.ZeroXlm)
        ).firstOrError()

    override fun getBalanceAndMin(): Single<BalanceAndMin> =
        Maybe.concat(
            maybeDefaultAccount().flatMap {
                getBalanceAndMin(it).toMaybe()
            },
            Maybe.just(BalanceAndMin(CryptoValue.ZeroXlm, CryptoValue.ZeroXlm))
        ).firstOrError()

    fun defaultAccount(): Single<AccountReference.Xlm> =
        defaultXlmAccount()
            .map(XlmAccount::toReference)

    override fun defaultAccountReference(): Single<AccountReference> = defaultAccount().map { it }

    fun maybeDefaultAccount(): Maybe<AccountReference.Xlm> =
        maybeDefaultXlmAccount()
            .map(XlmAccount::toReference)

    fun getTransactionList(accountReference: AccountReference.Xlm): Single<List<XlmTransaction>> =
        Single.fromCallable {
            horizonProxy.getTransactionList(accountReference.accountId).map(accountReference.accountId)
        }.subscribeOn(Schedulers.io())

    /**
     * See also [getOperationFee]
     */
    fun getTransactionFee(hash: String): Single<CryptoValue> =
        Single.fromCallable { horizonProxy.getTransaction(hash) }
            .map { CryptoValue.lumensFromStroop(it.feePaid.toBigInteger()) }
            .subscribeOn(Schedulers.io())

    /**
     * See also [getTransactionFee]
     */
    fun getOperationFee(transactionHash: String): Single<CryptoValue> =
        Single.fromCallable { horizonProxy.getTransaction(transactionHash) }
            .map { CryptoValue.lumensFromStroop((it.feePaid / it.operationCount).toBigInteger()) }
            .subscribeOn(Schedulers.io())

    fun getTransactionList(): Single<List<XlmTransaction>> =
        defaultAccount().flatMap { getTransactionList(it) }

    private fun defaultXlmAccount() =
        wallet.map(XlmMetaData::default)

    private fun maybeDefaultXlmAccount() =
        maybeWallet.map(XlmMetaData::default)
}

internal fun HorizonProxy.SendResult.mapToSendFundsResult(sendDetails: SendDetails): SendFundsResult =
    if (success) {
        SendFundsResult(
            sendDetails = sendDetails,
            errorCode = 0,
            confirmationDetails = SendConfirmationDetails(
                sendDetails = sendDetails,
                fees = CryptoValue.lumensFromStroop(transaction!!.fee.toBigInteger())
            ),
            hash = transaction.hash().toHex()
        )
    } else {
        SendFundsResult(
            sendDetails = sendDetails,
            errorCode = failureReason.errorCode,
            errorValue = failureValue,
            confirmationDetails = null,
            hash = null
        )
    }

private val SendDetails.fromXlm
    get() = from as? AccountReference.Xlm
        ?: throw XlmSendException("Source account reference is not an Xlm reference")

class XlmSendException(message: String) : RuntimeException(message)

private fun XlmAccount.toReference() =
    AccountReference.Xlm(label ?: "", publicKey)

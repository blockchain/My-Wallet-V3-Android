package com.blockchain.morph.ui.homebrew.exchange.confirmation

import com.blockchain.datamanagers.TransactionSendDataManager
import com.blockchain.datamanagers.fees.getFeeOptions
import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.service.TradeExecutionService
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.locked.ExchangeLockedModel
import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.formatWithUnit
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber

class ExchangeConfirmationPresenter(
    private val transactionSendDataManager: TransactionSendDataManager,
    private val tradeExecutionService: TradeExecutionService,
    private val feeDataManager: FeeDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val bchDataManager: BchDataManager,
    private val ethDataManager: EthDataManager,
    private val environmentConfig: EnvironmentConfig
) : BasePresenter<ExchangeConfirmationView>() {

    override fun onViewReady() {
        // Ensure user hasn't got a double encrypted wallet
        if (payloadDataManager.isDoubleEncrypted) {
            view.showSecondPasswordDialog()
        }

        compositeDisposable +=
            view.exchangeViewState
                .flatMapSingle { executeTrade(it.latestQuote!!, it.fromAccount, it.toAccount) }
                .subscribeBy(onError = Timber::e)
    }

    internal fun updateFee(
        amount: CryptoValue,
        sendingAccount: AccountReference
    ) {
        compositeDisposable +=
            feeDataManager.getFeeOptions(amount.currency)
                .flatMap {
                    transactionSendDataManager.getFeeForTransaction(
                        amount,
                        sendingAccount.getAccountFromAddressOrXPub(amount.currency),
                        it
                    )
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = { view.updateFee(it) },
                    onError = {
                        Timber.e(it)
                        view.showToast(
                            R.string.homebrew_confirmation_error_fetching_fee,
                            ToastCustom.TYPE_ERROR
                        )
                    }
                )
    }

    private fun executeTrade(
        quote: Quote,
        sendingAccount: AccountReference,
        receivingAccount: AccountReference
    ): Single<ExchangeLockedModel> {
        val sending = sendingAccount.getAccountFromAddressOrXPub(quote.from.cryptoValue.currency)
        val receiving = receivingAccount.getAccountFromAddressOrXPub(quote.to.cryptoValue.currency)

        return deriveAddressPair(quote, receiving, sending)
            .flatMap { (destination, refund) ->
                tradeExecutionService.executeTrade(quote, destination, refund)
                    .subscribeOn(Schedulers.io())
                    .flatMap { transaction ->
                        feeDataManager.getFeeOptions(transaction.deposit.currency)
                            .flatMap {
                                transactionSendDataManager.executeTransaction(
                                    transaction.deposit,
                                    transaction.depositAddress,
                                    sending,
                                    it
                                ).doOnError { Timber.e(it) }
                                    .subscribeOn(Schedulers.io())
                            }
                            .map {
                                ExchangeLockedModel(
                                    orderId = transaction.id,
                                    value = transaction.fiatValue.toStringWithSymbol(view.locale),
                                    fees = transaction.fee.formatWithUnit(),
                                    sending = transaction.deposit.formatWithUnit(),
                                    sendingCurrency = transaction.deposit.currency,
                                    receiving = transaction.withdrawal.formatWithUnit(),
                                    receivingCurrency = transaction.withdrawal.currency,
                                    accountName = receivingAccount.label
                                )
                            }
                    }
            }
            .observeOn(AndroidSchedulers.mainThread())
            .doOnSubscribe { view.showProgressDialog() }
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .doOnError { view.displayErrorDialog() }
            .doOnSuccess { view.continueToExchangeLocked(it) }
    }

    private fun deriveAddressPair(
        quote: Quote,
        receivingAccount: JsonSerializableAccount,
        sendingAccount: JsonSerializableAccount
    ): Single<Pair<String, String>> = Single.zip(
        transactionSendDataManager.getReceiveAddress(
            quote.to.cryptoValue.currency,
            receivingAccount
        ),
        transactionSendDataManager.getReceiveAddress(
            quote.from.cryptoValue.currency,
            sendingAccount
        ),
        BiFunction { destination: String, refund: String -> destination to refund }
    )

    internal fun onSecondPasswordValidated(validatedSecondPassword: String) {
        compositeDisposable +=
            decryptPayload(validatedSecondPassword)
                .andThen(decryptBch())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showProgressDialog() }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError { Timber.e(it) }
                .subscribe()
    }

    private fun decryptPayload(validatedSecondPassword: String): Completable =
        Completable.fromCallable {
            payloadDataManager.decryptHDWallet(
                environmentConfig.bitcoinNetworkParameters,
                validatedSecondPassword
            )
        }

    private fun decryptBch(): Completable = Completable.fromCallable {
        bchDataManager.decryptWatchOnlyWallet(payloadDataManager.mnemonic)
    }

    // TODO: Move this to an "all" data manager so that this class doesn't depend on all managers
    private fun AccountReference.getAccountFromAddressOrXPub(
        cryptoCurrency: CryptoCurrency
    ): JsonSerializableAccount {
        val xpubOrAddress = when (cryptoCurrency) {
            CryptoCurrency.BTC -> (this as AccountReference.BitcoinLike).xpub
            CryptoCurrency.ETHER -> (this as AccountReference.Ethereum).address
            CryptoCurrency.BCH -> (this as AccountReference.BitcoinLike).xpub
        }

        return when (cryptoCurrency) {
            CryptoCurrency.BTC -> payloadDataManager.getAccountForXPub(xpubOrAddress)
            CryptoCurrency.ETHER -> ethDataManager.getEthWallet()!!.account
            CryptoCurrency.BCH -> bchDataManager.getActiveAccounts()
                .asSequence()
                .filter { it.xpub == xpubOrAddress }
                .first()
        }
    }
}
package com.blockchain.morph.ui.homebrew.exchange.confirmation

import com.blockchain.datamanagers.TransactionSendDataManager
import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.service.TradeExecutionService
import com.blockchain.serialization.JsonSerializableAccount
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.api.data.FeeOptions
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber

class ExchangeConfirmationPresenter(
    private val transactionSendDataManager: TransactionSendDataManager,
    private val tradeExecutionService: TradeExecutionService,
    private val feeDataManager: FeeDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val bchDataManager: BchDataManager,
    private val environmentConfig: EnvironmentConfig
) : BasePresenter<ExchangeConfirmationView>() {

    override fun onViewReady() {
        // Ensure user hasn't got a double encrypted wallet
        if (payloadDataManager.isDoubleEncrypted) {
            view.showSecondPasswordDialog()
        }
    }

    internal fun updateFee(
        amount: CryptoValue,
        sendingAccount: JsonSerializableAccount,
        fees: FeeOptions
    ) {
        compositeDisposable +=
            transactionSendDataManager.getFeeForTransaction(amount, sendingAccount, fees)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSuccess { }
                .subscribeBy(
                    onSuccess = {
                        view.updateFee(it)
                    },
                    onError = {
                        // TODO: Missing data in the UI, how do we handle this?
                        Timber.e(it)
                    }
                )
    }

    internal fun executeTrade(
        quote: Quote,
        sendingAccount: JsonSerializableAccount,
        receivingAccount: JsonSerializableAccount
    ) {
        compositeDisposable +=
            getAddressPair(quote, receivingAccount, sendingAccount)
                .flatMap { (destination, refund) ->
                    tradeExecutionService.executeTrade(quote, destination, refund)
                        .subscribeOn(Schedulers.io())
                        .flatMap { transaction ->
                            transaction.deposit.currency.getFeeOptions()
                                .flatMap {
                                    transactionSendDataManager.executeTransaction(
                                        transaction.deposit,
                                        transaction.depositAddress,
                                        sendingAccount,
                                        it
                                    ).subscribeOn(Schedulers.io())
                                }
                                .doOnSuccess { view.continueToExchangeLocked(transaction.id) }
                        }
                }
                .doOnSubscribe { view.showProgressDialog() }
                .doOnEvent { _, _ -> view.dismissProgressDialog() }
                .doOnError { view.displayErrorDialog() }
                .subscribe()
    }

    private fun getAddressPair(
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

    private fun CryptoCurrency.getFeeOptions(): Single<FeeOptions> =
        when (this) {
            CryptoCurrency.BTC -> feeDataManager.btcFeeOptions
            CryptoCurrency.ETHER -> feeDataManager.ethFeeOptions
            CryptoCurrency.BCH -> feeDataManager.bchFeeOptions
        }.singleOrError()

    internal fun onSecondPasswordValidated(validatedSecondPassword: String) {
        compositeDisposable +=
            decryptPayload(validatedSecondPassword)
                .andThen(decryptBch())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnError { Timber.e(it) }
                .doOnSubscribe { view.showProgressDialog() }
                .doOnTerminate { view.dismissProgressDialog() }
                .subscribe()
    }

    private fun decryptPayload(validatedSecondPassword: String): Completable {
        return Completable.fromCallable {
            payloadDataManager.decryptHDWallet(
                environmentConfig.bitcoinNetworkParameters,
                validatedSecondPassword
            )
        }
    }

    private fun decryptBch(): Completable {
        return Completable.fromCallable {
            bchDataManager.decryptWatchOnlyWallet(payloadDataManager.mnemonic)
        }
    }
}
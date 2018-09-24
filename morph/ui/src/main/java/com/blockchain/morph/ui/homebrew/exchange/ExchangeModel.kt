package com.blockchain.morph.ui.homebrew.exchange

import android.arch.lifecycle.ViewModel
import com.blockchain.morph.exchange.mvi.ExchangeDialog
import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.ExchangeViewState
import com.blockchain.morph.exchange.mvi.Fix
import com.blockchain.morph.exchange.mvi.SetTradeLimits
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.exchange.service.QuoteServiceFactory
import com.blockchain.morph.exchange.service.TradeLimitService
import com.blockchain.morph.quote.ExchangeQuoteRequest
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ExchangeModel(
    quoteServiceFactory: QuoteServiceFactory,
    var configChangePersistence: ExchangeFragmentConfigurationChangePersistence,
    private var tradeLimitService: TradeLimitService
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val dialogDisposable = CompositeDisposable()

    val quoteService: QuoteService by lazy { quoteServiceFactory.createQuoteService() }

    private val exchangeViewModelsSubject = BehaviorSubject.create<ExchangeViewState>()

    val inputEventSink = PublishSubject.create<ExchangeIntent>()

    val exchangeViewStates: Observable<ExchangeViewState> = exchangeViewModelsSubject

    override fun onCleared() {
        super.onCleared()
        dialogDisposable.clear()
        compositeDisposable.clear()
        Timber.d("ExchangeViewModel cleared")
    }

    fun newDialog(exchangeDialog: ExchangeDialog) {
        dialogDisposable.clear()
        dialogDisposable += tradeLimitService.getTradesLimits()
            .subscribeBy {
                inputEventSink.onNext(SetTradeLimits(it.minOrder, it.maxOrder))
            }
        dialogDisposable += exchangeDialog.viewStates.distinctUntilChanged()
            .doOnError { Timber.e(it) }
            .subscribeBy {
                newViewModel(it)
            }
        dialogDisposable += exchangeViewStates
            .subscribeBy {
                configChangePersistence.fromReference = it.fromAccount
                configChangePersistence.toReference = it.toAccount
                quoteService.updateQuoteRequest(it.toExchangeQuoteRequest(it.fromFiat.currencyCode))
            }
    }

    private fun newViewModel(exchangeViewModel: ExchangeViewState) {
        exchangeViewModelsSubject.onNext(exchangeViewModel)
    }
}

private fun ExchangeViewState.toExchangeQuoteRequest(
    currency: String
): ExchangeQuoteRequest = when (fix) {
    Fix.COUNTER_FIAT ->
        ExchangeQuoteRequest.BuyingFiatLinked(
            offering = fromCrypto.currency,
            wanted = toCrypto.currency,
            wantedFiatValue = lastUserValue as FiatValue
        )
    Fix.BASE_FIAT ->
        ExchangeQuoteRequest.SellingFiatLinked(
            offering = fromCrypto.currency,
            wanted = toCrypto.currency,
            offeringFiatValue = lastUserValue as FiatValue
        )
    Fix.COUNTER_CRYPTO ->
        ExchangeQuoteRequest.Buying(
            offering = fromCrypto.currency,
            wanted = lastUserValue as CryptoValue,
            indicativeFiatSymbol = currency
        )
    Fix.BASE_CRYPTO ->
        ExchangeQuoteRequest.Selling(
            offering = lastUserValue as CryptoValue,
            wanted = toCrypto.currency,
            indicativeFiatSymbol = currency
        )
}

interface ExchangeViewModelProvider {
    val exchangeViewModel: ExchangeModel
}

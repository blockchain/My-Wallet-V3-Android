package com.blockchain.morph.ui.homebrew.exchange

import android.arch.lifecycle.ViewModel
import com.blockchain.morph.exchange.mvi.ExchangeDialog
import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.ExchangeViewModel
import com.blockchain.morph.exchange.mvi.Fix
import com.blockchain.morph.exchange.mvi.fixedField
import com.blockchain.morph.exchange.mvi.fixedMoneyValue
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.exchange.service.QuoteServiceFactory
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
    var configChangePersistence: ExchangeFragmentConfigurationChangePersistence
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val dialogDisposable = CompositeDisposable()

    val quoteService: QuoteService by lazy { quoteServiceFactory.createQuoteService() }

    private val exchangeViewModelsSubject = BehaviorSubject.create<ExchangeViewModel>()

    val inputEventSink = PublishSubject.create<ExchangeIntent>()

    val exchangeViewModels: Observable<ExchangeViewModel> = exchangeViewModelsSubject

    override fun onCleared() {
        super.onCleared()
        dialogDisposable.clear()
        compositeDisposable.clear()
        Timber.d("ExchangeViewModel cleared")
    }

    fun newDialog(exchangeDialog: ExchangeDialog) {
        dialogDisposable.clear()
        dialogDisposable += exchangeDialog.viewModel.distinctUntilChanged()
            .doOnError { Timber.e(it) }
            .subscribeBy {
                newViewModel(it)
            }
        dialogDisposable += exchangeViewModels
            .subscribeBy {
                configChangePersistence.fromReference = it.fromAccount
                configChangePersistence.toReference = it.toAccount
                quoteService.updateQuoteRequest(it.toExchangeQuoteRequest(it.from.fiatValue.currencyCode))
            }
    }

    private fun newViewModel(exchangeViewModel: ExchangeViewModel) {
        exchangeViewModelsSubject.onNext(exchangeViewModel)
    }
}

private fun ExchangeViewModel.toExchangeQuoteRequest(
    currency: String
): ExchangeQuoteRequest = when (fixedField) {
    Fix.COUNTER_FIAT ->
        ExchangeQuoteRequest.BuyingFiatLinked(
            offering = fromCryptoCurrency,
            wanted = toCryptoCurrency,
            wantedFiatValue = fixedMoneyValue as FiatValue
        )
    Fix.BASE_FIAT ->
        ExchangeQuoteRequest.SellingFiatLinked(
            offering = fromCryptoCurrency,
            wanted = toCryptoCurrency,
            offeringFiatValue = fixedMoneyValue as FiatValue
        )
    Fix.COUNTER_CRYPTO ->
        ExchangeQuoteRequest.Buying(
            offering = fromCryptoCurrency,
            wanted = fixedMoneyValue as CryptoValue,
            indicativeFiatSymbol = currency
        )
    Fix.BASE_CRYPTO ->
        ExchangeQuoteRequest.Selling(
            offering = fixedMoneyValue as CryptoValue,
            wanted = toCryptoCurrency,
            indicativeFiatSymbol = currency
        )
}

interface ExchangeViewModelProvider {
    val exchangeViewModel: ExchangeModel
}

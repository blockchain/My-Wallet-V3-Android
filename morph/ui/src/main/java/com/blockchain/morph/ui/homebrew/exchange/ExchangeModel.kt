package com.blockchain.morph.ui.homebrew.exchange

import android.arch.lifecycle.ViewModel
import com.blockchain.datamanagers.MaximumSpendableCalculator
import com.blockchain.morph.exchange.mvi.ExchangeDialog
import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.ExchangeViewState
import com.blockchain.morph.exchange.mvi.FiatExchangeRateIntent
import com.blockchain.morph.exchange.mvi.Fix
import com.blockchain.morph.exchange.mvi.SetTradeLimits
import com.blockchain.morph.exchange.mvi.SpendableValueIntent
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.exchange.service.QuoteServiceFactory
import com.blockchain.morph.exchange.service.TradeLimitService
import com.blockchain.morph.quote.ExchangeQuoteRequest
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber
import java.util.concurrent.atomic.AtomicReference

class ExchangeModel(
    quoteServiceFactory: QuoteServiceFactory,
    var configChangePersistence: ExchangeFragmentConfigurationChangePersistence,
    private var tradeLimitService: TradeLimitService,
    private var maximumSpendableCalculator: MaximumSpendableCalculator
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val dialogDisposable = CompositeDisposable()

    private val maxSpendableDisposable = CompositeDisposable()

    val quoteService: QuoteService by lazy { quoteServiceFactory.createQuoteService() }

    private val exchangeViewModelsSubject = BehaviorSubject.create<ExchangeViewState>()

    val inputEventSink = PublishSubject.create<ExchangeIntent>()

    val exchangeViewStates: Observable<ExchangeViewState> = exchangeViewModelsSubject

    private var accountThatHasCalculatedSpendable = AtomicReference<AccountReference?>()

    override fun onCleared() {
        super.onCleared()
        dialogDisposable.clear()
        compositeDisposable.clear()
        Timber.d("ExchangeViewModel cleared")
    }

    fun newDialog(
        fiatCurrency: String,
        exchangeDialog: ExchangeDialog
    ) {
        dialogDisposable.clear()
        dialogDisposable += quoteService.rates.subscribeBy {
            Timber.d("RawExchangeRate: $it")
            when (it) {
                is ExchangeRate.CryptoToFiat -> inputEventSink.onNext(FiatExchangeRateIntent(it))
            }
        }
        dialogDisposable += tradeLimitService.getTradesLimits(fiatCurrency)
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

                updateMaxSpendable(it.fromAccount)
            }
    }

    private fun updateMaxSpendable(account: AccountReference) {
        if (accountThatHasCalculatedSpendable.getAndSet(account) == account) return
        Timber.d("Updating max spendable for $account")
        maxSpendableDisposable.clear()
        maxSpendableDisposable += maximumSpendableCalculator.getMaximumSpendable(account)
            .subscribeBy {
                Timber.d("Max spendable is $it")
                inputEventSink.onNext(SpendableValueIntent(it))
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

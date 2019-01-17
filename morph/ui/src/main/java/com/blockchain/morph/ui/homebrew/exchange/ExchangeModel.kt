package com.blockchain.morph.ui.homebrew.exchange

import android.arch.lifecycle.ViewModel
import com.blockchain.accounts.AllAccountList
import com.blockchain.datamanagers.MaximumSpendableCalculator
import com.blockchain.morph.exchange.mvi.ExchangeDialog
import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.ExchangeViewState
import com.blockchain.morph.exchange.mvi.FiatExchangeRateIntent
import com.blockchain.morph.exchange.mvi.Fix
import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.mvi.SetTierLimit
import com.blockchain.morph.exchange.mvi.SetTradeLimits
import com.blockchain.morph.exchange.mvi.SetUserTier
import com.blockchain.morph.exchange.mvi.SpendableValueIntent
import com.blockchain.morph.exchange.mvi.allQuoteClearingConditions
import com.blockchain.morph.exchange.mvi.initial
import com.blockchain.morph.exchange.mvi.toIntent
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.exchange.service.QuoteServiceFactory
import com.blockchain.morph.exchange.service.TradeLimitService
import com.blockchain.morph.quote.ExchangeQuoteRequest
import com.blockchain.nabu.CurrentTier
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import com.blockchain.preferences.FiatCurrencyPreference
import timber.log.Timber
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

class ExchangeModel(
    quoteServiceFactory: QuoteServiceFactory,
    private val allAccountList: AllAccountList,
    private val tradeLimitService: TradeLimitService,
    private val currentTier: CurrentTier,
    private val maximumSpendableCalculator: MaximumSpendableCalculator,
    private val currencyPreference: FiatCurrencyPreference
) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val dialogDisposable = CompositeDisposable()

    private val maxSpendableDisposable = CompositeDisposable()

    val quoteService: QuoteService by lazy {
        quoteServiceFactory.createQuoteService()
            .also { initDialog(it) }
    }

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

    private fun initDialog(quoteService: QuoteService) {
        val fiatCurrency = currencyPreference.fiatCurrencyPreference
        newDialog(
            fiatCurrency,
            quoteService,
            ExchangeDialog(
                Observable.merge(
                    inputEventSink,
                    quoteService.quotes.map(Quote::toIntent)
                ),
                initial(
                    fiatCurrency,
                    allAccountList[CryptoCurrency.BTC].defaultAccountReference(),
                    allAccountList[CryptoCurrency.ETHER].defaultAccountReference()
                )
            )
        )
    }

    private fun newDialog(
        fiatCurrency: String,
        quoteService: QuoteService,
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
        dialogDisposable += Observable.interval(1, TimeUnit.MINUTES)
            .startWith(0L)
            .flatMapSingle {
                tradeLimitService.getTradesLimits(fiatCurrency)
            }
            .subscribeBy {
                inputEventSink.onNext(SetTierLimit(it.minAvailable()))
            }
        dialogDisposable += Observable.interval(1, TimeUnit.MINUTES)
            .startWith(0L)
            .flatMapSingle {
                currentTier.usersCurrentTier()
            }
            .subscribeBy {
                inputEventSink.onNext(SetUserTier(it))
            }
        dialogDisposable += exchangeDialog.viewStates.distinctUntilChanged()
            .doOnError { Timber.e(it) }
            .subscribeBy {
                newViewModel(it)
            }
        dialogDisposable += exchangeViewStates
            .subscribeBy {
                quoteService.updateQuoteRequest(it.toExchangeQuoteRequest(it.fromFiat.currencyCode))

                updateMaxSpendable(it.fromAccount)
            }
        dialogDisposable += exchangeViewStates.allQuoteClearingConditions()
            .subscribeBy {
                inputEventSink.onNext(it)
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

interface ExchangeLimitState {

    fun setOverTierLimit(overLimit: Boolean)
}

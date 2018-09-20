package com.blockchain.morph.ui.homebrew.exchange

import android.arch.lifecycle.ViewModel
import com.blockchain.morph.exchange.mvi.ExchangeDialog
import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.ExchangeViewModel
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.exchange.service.QuoteServiceFactory
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject
import timber.log.Timber

class ExchangeModel(quoteServiceFactory: QuoteServiceFactory) : ViewModel() {

    private val compositeDisposable = CompositeDisposable()

    private val dialogDisposable = CompositeDisposable()

    val quoteService: QuoteService by lazy { quoteServiceFactory.createQuoteService() }

    var configChangePersistence = ExchangeFragmentConfigurationChangePersistence()

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
                configChangePersistence.from = it.from.cryptoValue.currency
                configChangePersistence.to = it.to.cryptoValue.currency
            }
    }

    private fun newViewModel(exchangeViewModel: ExchangeViewModel) {
        exchangeViewModelsSubject.onNext(exchangeViewModel)
    }
}

interface ExchangeViewModelProvider {
    val exchangeViewModel: ExchangeModel
}

package com.blockchain.morph.ui.homebrew.exchange.history

import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeDataManager
import com.blockchain.morph.ui.homebrew.exchange.model.Trade
import info.blockchain.balance.formatWithUnit
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.utils.DateUtil
import timber.log.Timber

class TradeHistoryPresenter(
    private val dataManager: MorphTradeDataManager,
    private val dateUtil: DateUtil
) : BasePresenter<TradeHistoryView>() {

    override fun onViewReady() {
        compositeDisposable +=
            dataManager.getTrades()
                .subscribeOn(Schedulers.io())
                .flattenAsObservable { it }
                .map { it.map() }
                .toList()
                .toObservable()
                .map<ExchangeUiState> {
                    if (it.isNotEmpty()) {
                        ExchangeUiState.Data(it)
                    } else {
                        ExchangeUiState.Empty
                    }
                }
                .doOnError { Timber.e(it) }
                .onErrorReturn { ExchangeUiState.Error }
                .startWith(ExchangeUiState.Loading)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(onNext = { view.renderUi(it) })
    }

    private fun MorphTrade.map(): Trade = Trade(
        id = this.quote.orderId,
        state = this.status,
        currency = this.quote.pair.to.symbol,
        price = "Price not yet returned",
        pair = this.quote.pair.pairCode,
        quantity = this.quote.withdrawalAmount.formatWithUnit(view.locale),
        createdAt = dateUtil.formatted(this.timestamp),
        depositQuantity = this.quote.depositAmount.formatWithUnit(view.locale)
    )
}

sealed class ExchangeUiState {
    data class Data(val trades: List<Trade>) : ExchangeUiState()
    object Error : ExchangeUiState()
    object Loading : ExchangeUiState()
    object Empty : ExchangeUiState()
}
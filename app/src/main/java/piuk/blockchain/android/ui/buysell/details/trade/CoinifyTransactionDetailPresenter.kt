package piuk.blockchain.android.ui.buysell.details.trade

import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.coinify.CardDetails
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import javax.inject.Inject

class CoinifyTransactionDetailPresenter @Inject constructor(
    private val coinifyDataManager: CoinifyDataManager,
    private val exchangeService: ExchangeService
) : BasePresenter<CoinifyTransactionDetailView>() {

    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .addToCompositeDisposable(this)
            .applySchedulers()
            .singleOrError()
            .map { it.coinify!!.token }

    override fun onViewReady() = Unit

    internal fun onFinishCardPayment() {
        val orderDetails = view.orderDetails

        tokenSingle.flatMap { coinifyDataManager.getTradeStatus(it, orderDetails.tradeId) }
            .doOnSubscribe { view.showProgressDialog() }
            .doAfterTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onSuccess = {
                    val details = it.transferIn.details as CardDetails
                    view.launchCardPayment(
                        details.redirectUrl,
                        details.paymentId,
                        it.inCurrency,
                        it.inAmount
                    )
                },
                onError = { view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR) }
            )
    }

    internal fun cancelTrade(tradeId: Int) {
        tokenSingle
            .flatMap { coinifyDataManager.cancelTrade(it, tradeId) }
            .applySchedulers()
            .doOnSubscribe { view.showProgressDialog() }
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .doOnError { Timber.e(it) }
            .subscribeBy(
                onSuccess = {
                    view.showToast(
                        R.string.buy_sell_awaiting_funds_cancel_trade_success,
                        ToastCustom.TYPE_OK
                    )
                    view.finishPage()
                },
                onError = {
                    view.showToast(
                        R.string.buy_sell_awaiting_funds_cancel_trade_failed,
                        ToastCustom.TYPE_ERROR
                    )
                }
            )
    }
}
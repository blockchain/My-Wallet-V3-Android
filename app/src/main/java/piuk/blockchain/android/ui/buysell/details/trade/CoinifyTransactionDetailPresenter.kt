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
                onError = { view.showErrorToast(R.string.unexpected_error) }
            )
    }
}
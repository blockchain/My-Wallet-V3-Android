package piuk.blockchain.android.ui.buysell.details.awaitingtransfer

import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import javax.inject.Inject

class CoinifyAwaitingBankTransferPresenter @Inject constructor(
    private val exchangeService: ExchangeService,
    private val coinifyDataManager: CoinifyDataManager
) : BasePresenter<CoinifyAwaitingBankTransferView>() {

    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .addToCompositeDisposable(this)
            .applySchedulers()
            .singleOrError()
            .map { it.coinify!!.token }

    override fun onViewReady() = Unit

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
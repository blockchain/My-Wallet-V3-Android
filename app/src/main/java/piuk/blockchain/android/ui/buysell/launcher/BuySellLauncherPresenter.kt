package piuk.blockchain.android.ui.buysell.launcher

import io.reactivex.Maybe
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import javax.inject.Inject

class BuySellLauncherPresenter @Inject constructor(
    private val exchangeService: ExchangeService,
    private val coinifyDataManager: CoinifyDataManager
) : BasePresenter<BuySellLauncherView>() {

    override fun onViewReady() {
        exchangeService.getExchangeMetaData()
            .addToCompositeDisposable(this)
            .applySchedulers()
            .doOnSubscribe { view.displayProgressDialog() }
            .flatMapMaybe {
                if (it.hasCoinifyAccount()) {
                    coinifyDataManager.getKycReviews(it.coinify!!.token!!).toMaybe()
                        .doOnSuccess {
                            if (it.canContinueToOverview()) {
                                view.onStartCoinifyOverview()
                            } else {
                                view.onStartCoinifySignUp()
                            }
                        }
                } else {
                    view.onStartCoinifySignUp()
                    Maybe.empty()
                }
            }
            .doOnTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onComplete = { /* No-op */ },
                onError = {
                    Timber.e(it)
                    view.showErrorToast(R.string.buy_sell_launcher_error)
                    view.finishPage()
                }
            )
    }

    private fun ExchangeData.hasCoinifyAccount(): Boolean = coinify?.token != null

    private fun List<KycResponse>.canContinueToOverview(): Boolean = this.any {
        it.state == ReviewState.Reviewing || it.state == ReviewState.Completed
    }
}
package piuk.blockchain.android.ui.buysell.coinify.signup.identityinreview

import android.support.annotation.VisibleForTesting
import com.google.common.base.Optional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class CoinifyIdentityInReviewPresenter @Inject constructor(
    private val exchangeService: ExchangeService,
    private val coinifyDataManager: CoinifyDataManager
) : BasePresenter<CoinifyIdentityInReviewView>() {

    override fun onViewReady() {
        // Leave enough of a delay for KYC to move from Pending to Reviewing
        Observable.timer(10, TimeUnit.SECONDS, Schedulers.computation())
            .applySchedulers()
            .addToCompositeDisposable(this)
            .doOnSubscribe { view.onShowLoading() }
            .flatMap { getCoinifyMetaDataObservable() }
            .flatMapCompletable {
                if (it.isPresent) {
                    // User has coinify account - Continue sign-up or go to overview
                    continueTraderSignupOrGoToOverviewCompletable(it.get())
                } else {
                    // This will never happen but handle case anyway
                    view.onFinish()
                    Completable.complete()
                }
            }
            .doOnEvent { view.dismissLoading() }
            .subscribeBy(
                onError = {
                    Timber.e(it)
                    view.onFinish()
                }
            )
    }

    private fun continueTraderSignupOrGoToOverviewCompletable(coinifyData: CoinifyData) =
        coinifyDataManager.getTrader(coinifyData.token!!)
            .flatMap {
                // Trader exists - Check for any KYC reviews
                coinifyDataManager.getKycReviews(coinifyData.token!!)
            }.flatMapCompletable { kycList ->
                filterReviewStatus(kycList)
                Completable.complete()
            }
            .applySchedulers()

    @VisibleForTesting
    fun filterReviewStatus(kycList: List<KycResponse>) {
        when {
        // Unlikely to see this result - after supplying docs status will be pending
        // otherwise we will go straight to overview
            kycList.any { it.state == ReviewState.Completed } -> view.onShowCompleted()
        // Unlikely to see this result - after supplying docs status will be pending
        // otherwise we will go straight to overview
            kycList.any { it.state == ReviewState.Reviewing } -> view.onShowReviewing()
        // Please supply proof
        // Very likely that the back button was pressed
            kycList.any { it.state == ReviewState.Pending } -> view.onShowPending()
        // Unlikely to see this result
            kycList.any { it.state == ReviewState.DocumentsRequested } -> view.onShowDocumentsRequested()
        // Unlikely to see this result - User would be redirected to supply docs again before getting to this fragment
            kycList.any { it.state == ReviewState.Expired } -> view.onShowExpired()
        // We get stuck with the below cases
        // Can't create new account with same email
        // redirectUrl isn't valid - Same issue on web
            kycList.any { it.state == ReviewState.Failed } -> view.onShowFailed()
            kycList.any { it.state == ReviewState.Rejected } -> view.onShowRejected()
            else -> view.onFinish()
        }
    }

    /**
     * Fetches coinify data from metadata store i.e offline token and user/trader id.
     *
     * @return An [Observable] wrapping an [Optional] with coinify data
     */
    private fun getCoinifyMetaDataObservable() =
        exchangeService.getExchangeMetaData()
            .applySchedulers()
            .addToCompositeDisposable(this)
            .map { it.coinify?.run { Optional.of(this) } ?: Optional.absent() }
}
package piuk.blockchain.android.ui.buysell.coinify.signup

import com.google.common.base.Optional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.models.coinify.exceptions.CoinifyApiException
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import javax.inject.Inject

class CoinifySignUpPresenter @Inject constructor(
    private val exchangeService: ExchangeService,
    private val coinifyDataManager: CoinifyDataManager,
    private val stringUtils: StringUtils
) : BasePresenter<CoinifySignupView>() {

    override fun onViewReady() {
        getCoinifyTokenOptional()
            .applySchedulers()
            .flatMapCompletable { optionalCoinifyData ->
                if (optionalCoinifyData.isPresent) {
                    // User has coinify account - Continue signup or go to overview
                    startKycOrGoToOverviewCompletable(optionalCoinifyData.get())
                } else {
                    // No stored metadata for buy sell - Assume no Coinify account
                    view.onStartWelcome()
                    Completable.complete()
                }
            }
            .addToCompositeDisposable(this)
            .subscribeBy(onError = { handleCoinifyException(it) })
    }

    /**
     * Calculates the user/trader's signup status and redirects to proper fragment.
     *
     * Passed KYC state = Go to overview
     * DocumentsRequested, Pending = Go to redirect URL
     * Rejected, Failed, Expired = Allow user to reattempt sign up
     *
     * @return [Completable]
     */
    private fun startKycOrGoToOverviewCompletable(coinifyToken: String) =
        coinifyDataManager.getTrader(coinifyToken)
            .flatMap {
                // Trader exists - Check for any KYC reviews
                coinifyDataManager.getKycReviews(coinifyToken)
            }
            .flatMapCompletable { kycList ->
                if (kycList.isEmpty()) {
                    // Kyc review not started yet
                    startKycReviewProcess(coinifyToken)
                } else {
                    view.onStartOverview()
                    Completable.complete()
                }
            }

    internal fun continueVerifyIdentification() {
        getCoinifyTokenOptional()
            .flatMapSingle {
                if (!it.isPresent) throw IllegalStateException("No Coinify metadata found")
                val coinifyToken = it.get()

                return@flatMapSingle coinifyDataManager.getTrader(coinifyToken)
                    .flatMap { coinifyDataManager.getKycReviews(coinifyToken) }
                    .doOnSuccess { kycList ->
                        if (kycList.isEmpty()) {
                            throw IllegalStateException("KYC list should not be empty at this point")
                        } else {
                            val pendingState = kycList.lastOrNull {
                                it.state == ReviewState.DocumentsRequested || it.state == ReviewState.Pending
                            }

                            when {
                            // Continuing after Konfetti fragment
                                pendingState != null -> view.onStartVerifyIdentification(
                                    pendingState.redirectUrl,
                                    pendingState.externalId
                                )
                            // Returning after completed or failed KYC
                                else -> view.onStartOverview()
                            }
                        }
                    }
            }
            .addToCompositeDisposable(this)
            .subscribeBy(onError = { handleCoinifyException(it) })
    }

    private fun handleCoinifyException(it: Throwable) {
        Timber.e(it)
        if (it is CoinifyApiException) {
            view.showToast(it.getErrorDescription())
        } else {
            view.showToast(stringUtils.getString(R.string.buy_sell_confirmation_unexpected_error))
        }
        view.onFinish()
    }

    /**
     * Fetches coinify data from metadata store i.e offline token and user/trader id.
     *
     * @return An [Observable] wrapping an [Optional] with coinify data
     */
    private fun getCoinifyTokenOptional(): Observable<Optional<String>> =
        exchangeService.getExchangeMetaData()
            .applySchedulers()
            .addToCompositeDisposable(this)
            .map {
                it.coinify?.token?.run { Optional.of(this) } ?: Optional.absent()
            }

    private fun startKycReviewProcess(coinifyToken: String): Completable =
        coinifyDataManager.startKycReview(coinifyToken)
            .doOnSuccess { view.onStartVerifyIdentification(it.redirectUrl, it.externalId) }
            .ignoreElement()
            .applySchedulers()
}
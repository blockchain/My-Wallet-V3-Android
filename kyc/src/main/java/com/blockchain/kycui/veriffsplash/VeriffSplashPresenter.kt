package com.blockchain.kycui.veriffsplash

import com.blockchain.BaseKycPresenter
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.nabu.NabuToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.kyc.R
import timber.log.Timber

class VeriffSplashPresenter(
    nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager
) : BaseKycPresenter<VeriffSplashView>(nabuToken) {

    override fun onViewReady() {
        compositeDisposable +=
            view.uiState
                .flatMapSingle {
                    fetchOfflineToken
                        .flatMap { token ->
                            nabuDataManager.getVeriffToken(token)
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog(true) }
                        .doOnEvent { _, _ -> view.dismissProgressDialog() }
                        .doOnSuccess { applicant ->
                            view.continueToVeriff(applicant)
                        }
                        .doOnError { e ->
                            Timber.e(e)
                            view.showErrorToast(R.string.kyc_onfido_splash_verification_error)
                        }
                }
                .doOnError(Timber::e)
                .retry()
                .subscribe()
    }

    internal fun submitVerification() {
        compositeDisposable +=
            fetchOfflineToken
                .flatMap { token ->
                    nabuDataManager.getVeriffToken(token)
                        .map {
                            token to it
                        }
                        .subscribeOn(Schedulers.io())
                }
                .flatMapCompletable { (tokenResponse, applicant) ->
                    nabuDataManager.submitVeriffVerification(tokenResponse, applicant.applicantId)
                        .subscribeOn(Schedulers.io())
                }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showProgressDialog(false) }
                .doOnTerminate { view.dismissProgressDialog() }
                .doOnError(Timber::e)
                .subscribeBy(
                    onComplete = { view.continueToCompletion() },
                    onError = {
                        view.showErrorToast(R.string.kyc_onfido_splash_verification_error)
                    }
                )
    }

    internal fun onProgressCancelled() {
        // Clear outbound requests
        compositeDisposable.clear()
        // Resubscribe
        onViewReady()
    }
}
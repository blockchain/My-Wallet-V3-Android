package com.blockchain.kycui.onfidosplash

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.datamanagers.onfido.OnfidoDataManager
import com.blockchain.kycui.extensions.fetchNabuToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber

class OnfidoSplashPresenter(
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager,
    private val onfidoDataManager: OnfidoDataManager
) : BasePresenter<OnfidoSplashView>() {

    private val fetchOfflineToken by unsafeLazy { metadataManager.fetchNabuToken() }

    override fun onViewReady() {
        compositeDisposable +=
            view.uiState
                .flatMapSingle { _ ->
                    fetchOfflineToken
                        .flatMap { token ->
                            nabuDataManager.getOnfidoApiKey(token)
                                .subscribeOn(Schedulers.io())
                                .flatMap { apiKey ->
                                    nabuDataManager.getUser(token)
                                        .subscribeOn(Schedulers.io())
                                        .flatMap { user ->
                                            onfidoDataManager.createApplicant(
                                                user.firstName
                                                    ?: throw IllegalStateException("firstName is null"),
                                                user.lastName
                                                    ?: throw IllegalStateException("lastName is null"),
                                                apiKey
                                            ).subscribeOn(Schedulers.io())
                                        }
                                        .map { it to apiKey }
                                }
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog(true) }
                        .doOnEvent { _, _ -> view.dismissProgressDialog() }
                        .doOnSuccess { (applicant, apiKey) ->
                            view.continueToOnfido(apiKey, applicant.id)
                        }
                        .doOnError {
                            view.showErrorToast(R.string.kyc_onfido_splash_verification_error)
                        }
                }
                .doOnError(Timber::e)
                .retry()
                .subscribe()
    }

    internal fun submitVerification(applicantId: String) {
        compositeDisposable +=
            fetchOfflineToken
                .flatMapCompletable { tokenResponse ->
                    nabuDataManager.submitOnfidoVerification(tokenResponse, applicantId)
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
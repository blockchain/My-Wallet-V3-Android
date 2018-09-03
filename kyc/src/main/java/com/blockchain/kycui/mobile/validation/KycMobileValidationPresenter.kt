package com.blockchain.kycui.mobile.validation

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kycui.extensions.fetchNabuToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycMobileValidationPresenter(
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager,
    private val settingsDataManager: SettingsDataManager
) : BasePresenter<KycMobileValidationView>() {

    private val fetchOfflineToken by unsafeLazy { metadataManager.fetchNabuToken() }

    override fun onViewReady() {
        compositeDisposable +=
            view.uiStateObservable
                .flatMapCompletable { (verificationModel, _) ->
                    settingsDataManager.verifySms(verificationModel.verificationCode.code)
                        .flatMapCompletable { _ ->
                            nabuDataManager.requestJwt()
                                .subscribeOn(Schedulers.io())
                                .flatMap { jwt ->
                                    fetchOfflineToken.flatMap {
                                        nabuDataManager.updateUserWalletInfo(it, jwt)
                                            .subscribeOn(Schedulers.io())
                                    }
                                }
                                .ignoreElement()
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate { view.dismissProgressDialog() }
                        .doOnError {
                            Timber.e(it)
                            view.displayErrorDialog(R.string.kyc_phone_number_validation_error_incorrect)
                        }
                        .doOnComplete { view.continueSignUp() }
                }
                .retry()
                .doOnError(Timber::e)
                .subscribe()
    }

    internal fun onProgressCancelled() {
        // Clear outbound requests
        compositeDisposable.clear()
        // Resubscribe
        onViewReady()
    }
}

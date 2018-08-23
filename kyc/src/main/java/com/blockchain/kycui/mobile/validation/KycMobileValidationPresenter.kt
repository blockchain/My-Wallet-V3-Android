package com.blockchain.kycui.mobile.validation

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.NabuApiException
import com.blockchain.kyc.models.nabu.NabuErrorCodes
import com.blockchain.kycui.extensions.fetchNabuToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycMobileValidationPresenter(
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager
) : BasePresenter<KycMobileValidationView>() {

    private val fetchOfflineToken by unsafeLazy { metadataManager.fetchNabuToken() }

    override fun onViewReady() {
        compositeDisposable +=
            view.uiStateObservable
                .flatMapCompletable { (verificationModel, _) ->
                    fetchOfflineToken.flatMapCompletable { tokenResponse ->
                        nabuDataManager.verifyMobileNumber(
                            tokenResponse,
                            verificationModel.sanitizedPhoneNumber,
                            verificationModel.verificationCode.code
                        ).subscribeOn(Schedulers.io())
                    }.observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate { view.dismissProgressDialog() }
                        .doOnError {
                            if (it is NabuApiException &&
                                it.getErrorCode() == NabuErrorCodes.VerificationCodeIncorrect
                            ) {
                                view.displayErrorDialog(R.string.kyc_phone_number_validation_error_incorrect)
                            } else {
                                view.showErrorToast(R.string.kyc_phone_number_validation_error)
                            }
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

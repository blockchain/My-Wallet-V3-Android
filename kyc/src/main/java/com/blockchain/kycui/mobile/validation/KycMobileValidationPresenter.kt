package com.blockchain.kycui.mobile.validation

import com.blockchain.nabu.NabuUserSync
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import piuk.blockchain.androidcore.data.settings.PhoneNumberUpdater
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycMobileValidationPresenter(
    private val nabuUserSync: NabuUserSync,
    private val phoneNumberUpdater: PhoneNumberUpdater
) : BasePresenter<KycMobileValidationView>() {

    override fun onViewReady() {
        setupRxEvents()
    }

    private fun setupRxEvents() {
        compositeDisposable +=
            view.uiStateObservable
                .flatMapCompletable { (verificationModel, _) ->
                    phoneNumberUpdater.verifySms(verificationModel.verificationCode.code)
                        .flatMapCompletable {
                            nabuUserSync.syncUser()
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
        compositeDisposable +=
            view.resendObservable
                .flatMapCompletable { (phoneNumber, _) ->
                    phoneNumberUpdater.updateSms(phoneNumber)
                        .flatMapCompletable {
                            nabuUserSync.syncUser()
                        }
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate { view.dismissProgressDialog() }
                        .doOnError {
                            Timber.e(it)
                            view.displayErrorDialog(R.string.kyc_phone_number_error_resending)
                        }
                        .doOnComplete { view.theCodeWasResent() }
                }
                .retry()
                .doOnError(Timber::e)
                .subscribe()
    }

    internal fun onProgressCancelled() {
        // Clear outbound requests
        compositeDisposable.clear()
        // Resubscribe
        setupRxEvents()
    }
}

package com.blockchain.kycui.email.validation

import com.blockchain.nabu.NabuUserSync
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber
import java.util.concurrent.TimeUnit

class KycEmailValidationPresenter(
    private val nabuUserSync: NabuUserSync,
    private val emailUpdater: EmailSyncUpdater
) : BasePresenter<KycEmailValidationView>() {

    override fun onViewReady() {
        compositeDisposable += Observable.interval(1, TimeUnit.SECONDS)
            .flatMapSingle {
                emailUpdater.email()
            }
            .distinctUntilChanged()
            .flatMapSingle {
                synchronizeVerificationStatus()
                    .andThen(Single.just(it))
            }
            .observeOn(AndroidSchedulers.mainThread())
            .retry()
            .doOnError(Timber::e)
            .subscribe {
                view.setVerified(it.verified)
            }

        compositeDisposable +=
            view.uiStateObservable
                .flatMapCompletable { (email, _) ->
                    emailUpdater.updateEmailAndSync(email)
                        .ignoreElement()
                        .observeOn(AndroidSchedulers.mainThread())
                        .doOnSubscribe { view.showProgressDialog() }
                        .doOnTerminate { view.dismissProgressDialog() }
                        .doOnError {
                            Timber.e(it)
                            view.displayErrorDialog(R.string.kyc_email_error_saving_email)
                        }
                        .doOnComplete {
                            view.theEmailWasResent()
                        }
                }
                .retry()
                .doOnError(Timber::e)
                .subscribe()
    }

    private fun synchronizeVerificationStatus() =
        nabuUserSync.syncUser()

    internal fun onProgressCancelled() {
        // Clear outbound requests
        compositeDisposable.clear()
        // Resubscribe
        onViewReady()
    }
}

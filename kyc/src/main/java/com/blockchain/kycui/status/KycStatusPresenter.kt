package com.blockchain.kycui.status

import com.blockchain.BaseKycPresenter
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.nabu.NabuToken
import com.blockchain.notifications.NotificationTokenManager
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycStatusPresenter(
    nabuToken: NabuToken,
    private val nabuDataManager: NabuDataManager,
    private val notificationTokenManager: NotificationTokenManager
) : BaseKycPresenter<KycStatusView>(nabuToken) {

    override fun onViewReady() {
        compositeDisposable +=
            fetchOfflineToken
                .flatMap {
                    nabuDataManager.getUser(it)
                        .subscribeOn(Schedulers.io())
                }
                .map { it.kycState }
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showProgressDialog() }
                .doOnEvent { _, _ -> view.dismissProgressDialog() }
                .doOnError { Timber.e(it) }
                .subscribeBy(
                    onSuccess = { view.renderUi(it) },
                    onError = { view.finishPage() }
                )
    }

    internal fun onClickNotifyUser() {
        compositeDisposable +=
            notificationTokenManager.enableNotifications()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe { view.showProgressDialog() }
                .doOnEvent { view.dismissProgressDialog() }
                .subscribeBy(
                    onComplete = {
                        view.showNotificationsEnabledDialog()
                    },
                    onError = {
                        view.showToast(R.string.kyc_status_button_notifications_error)
                        Timber.e(it)
                    }
                )
    }

    internal fun onClickContinue() {
        view.startExchange()
    }

    internal fun onProgressCancelled() {
        // Cancel subscriptions
        compositeDisposable.clear()
        // Exit page as UI won't be rendered
        view.finishPage()
    }
}

package com.blockchain.kycui.status

import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kycui.extensions.fetchNabuToken
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import com.blockchain.notifications.NotificationTokenManager
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.kyc.R
import timber.log.Timber

class KycStatusPresenter(
    private val metadataManager: MetadataManager,
    private val nabuDataManager: NabuDataManager,
    private val notificationTokenManager: NotificationTokenManager
) : BasePresenter<KycStatusView>() {

    private val fetchOfflineToken by unsafeLazy { metadataManager.fetchNabuToken() }

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
                        view.showToast(R.string.kyc_status_button_notifications_enabled)
                    },
                    onError = { Timber.e(it) }
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

package piuk.blockchain.android.ui.login

import dagger.Lazy
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.launcher.LauncherActivity
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.logging.Logging
import piuk.blockchain.androidcoreui.utils.logging.PairingEvent
import piuk.blockchain.androidcoreui.utils.logging.PairingMethod
import javax.inject.Inject
import javax.net.ssl.SSLPeerUnverifiedException

class LoginPresenter @Inject constructor(
    private val appUtil: AppUtil,
    private val payloadDataManager: Lazy<PayloadDataManager>,
    private val prefsUtil: PrefsUtil
) : BasePresenter<LoginView>() {

    override fun onViewReady() {
        // No-op
    }

    internal fun pairWithQR(raw: String?) {
        appUtil.clearCredentials()

        if (raw == null) view.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)

        val dataManager = payloadDataManager.get()
        dataManager.handleQrCode(raw!!)
            .addToCompositeDisposable(this)
            .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
            .doOnComplete { appUtil.sharedKey = dataManager.wallet!!.sharedKey }
            .doAfterTerminate { view.dismissProgressDialog() }
            .subscribe({
                prefsUtil.setValue(PrefsUtil.KEY_GUID, dataManager.wallet!!.guid)
                prefsUtil.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true)
                prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, true)
                view.startPinEntryActivity()

                Logging.logCustom(
                    PairingEvent()
                        .putMethod(PairingMethod.QR_CODE)
                        .putSuccess(true)
                )
            }, { throwable ->
                Logging.logCustom(
                    PairingEvent()
                        .putMethod(PairingMethod.QR_CODE)
                        .putSuccess(false)
                )

                if (throwable is SSLPeerUnverifiedException) {
                    // BaseActivity handles message
                    appUtil.clearCredentials()
                } else {
                    view.showToast(R.string.pairing_failed, ToastCustom.TYPE_ERROR)
                    appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)
                }
            })
    }
}
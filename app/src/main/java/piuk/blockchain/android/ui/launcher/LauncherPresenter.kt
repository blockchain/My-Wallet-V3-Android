package piuk.blockchain.android.ui.launcher

import android.app.LauncherActivity
import android.content.Intent
import info.blockchain.wallet.api.data.Settings
import piuk.blockchain.android.R
import piuk.blockchain.android.data.notifications.FcmCallbackService.EXTRA_CONTACT_ACCEPTED
import piuk.blockchain.android.data.notifications.NotificationTokenManager
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import javax.inject.Inject

class LauncherPresenter @Inject constructor(
    private val appUtil: AppUtil,
    private val payloadDataManager: PayloadDataManager,
    private val prefsUtil: PrefsUtil,
    private val accessState: AccessState,
    private val settingsDataManager: SettingsDataManager,
    private val notificationTokenManager: NotificationTokenManager
) : BasePresenter<LauncherView>() {

    override fun onViewReady() {
        val intent = view.getPageIntent()
        val action = intent.action
        val scheme = intent.scheme
        val intentData = intent.dataString
        val extras = intent.extras
        val hasLoggedOut = prefsUtil.getValue(PrefsUtil.LOGGED_OUT, false)
        var isPinValidated = false

        // Store incoming bitcoin URI if needed
        if (action != null && Intent.ACTION_VIEW == action && scheme != null && scheme == "bitcoin") {
            prefsUtil.setValue(PrefsUtil.KEY_SCHEME_URL, intent.data.toString())
        }

        // Store incoming Contacts URI if needed
        if (action != null && Intent.ACTION_VIEW == action && intentData != null && intentData.contains(
                "blockchain"
            )
        ) {
            prefsUtil.setValue(PrefsUtil.KEY_METADATA_URI, intentData)
        }

        // Store if coming from specific Contacts notification
        if (intent.hasExtra(EXTRA_CONTACT_ACCEPTED)) {
            prefsUtil.setValue(PrefsUtil.KEY_CONTACTS_NOTIFICATION, true)
        }

        if (extras != null && extras.containsKey(INTENT_EXTRA_VERIFIED)) {
            isPinValidated = extras.getBoolean(INTENT_EXTRA_VERIFIED)
        }

        when {
        // No GUID? Treat as new installation
            prefsUtil.getValue(PrefsUtil.KEY_GUID, "").isEmpty() -> view.onNoGuid()
        // User has logged out recently. Show password reentry page
            hasLoggedOut -> view.onReEnterPassword()
        // No PIN ID? Treat as installed app without confirmed PIN
            prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "").isEmpty() -> view.onRequestPin()
        // Installed app, check sanity
            !appUtil.isSane -> view.onCorruptPayload()
        // Legacy app has not been prompted for upgrade
            isPinValidated && !payloadDataManager.wallet!!.isUpgraded -> promptUpgrade()
        // App has been PIN validated
            isPinValidated || accessState.isLoggedIn -> initSettings()
        // Something odd has happened, re-request PIN
            else -> view.onRequestPin()
        }
    }

    fun clearCredentialsAndRestart() =
        appUtil.clearCredentialsAndRestart(LauncherActivity::class.java)

    private fun promptUpgrade() {
        accessState.setIsLoggedIn(true)
        view.onRequestUpgrade()
    }

    /**
     * Init of the [SettingsDataManager] must complete here so that we can access the [Settings]
     * object from memory when the user is logged in.
     */
    private fun initSettings() {
        settingsDataManager.initSettings(
            payloadDataManager.wallet!!.guid,
            payloadDataManager.wallet!!.sharedKey
        )
            .doOnComplete { accessState.setIsLoggedIn(true) }
            .doOnNext { notificationTokenManager.registerAuthEvent() }
            .addToCompositeDisposable(this)
            .subscribe({ settings ->
                checkOnboardingStatus(settings)
                setCurrencyUnits(settings)
            }, { _ ->
                view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                view.onRequestPin()
            })
    }

    private fun checkOnboardingStatus(settings: Settings) {
        when {
            accessState.isNewlyCreated -> view.onStartOnboarding(false)
            !settings.isEmailVerified &&
                settings.email != null
                && !settings.email.isEmpty() -> checkIfOnboardingNeeded()
            else -> view.onStartMainActivity()
        }
    }

    private fun checkIfOnboardingNeeded() {
        var visits = prefsUtil.getValue(PrefsUtil.KEY_APP_VISITS, 0)
        // Nag user to verify email after second login
        when (visits) {
            1 -> view.onStartOnboarding(true)
            else -> view.onStartMainActivity()
        }

        visits++
        prefsUtil.setValue(PrefsUtil.KEY_APP_VISITS, visits)
    }

    private fun setCurrencyUnits(settings: Settings) {
        prefsUtil.setValue(PrefsUtil.KEY_SELECTED_FIAT, settings.currency)
    }

    companion object {
        const val INTENT_EXTRA_VERIFIED = "verified"
    }
}
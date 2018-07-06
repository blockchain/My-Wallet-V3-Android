package piuk.blockchain.android.ui.launcher

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.auth.LandingActivity
import piuk.blockchain.android.ui.auth.PasswordRequiredActivity
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.onboarding.OnboardingActivity
import piuk.blockchain.android.ui.onboarding.OnboardingActivity.EXTRAS_EMAIL_ONLY
import piuk.blockchain.android.ui.upgrade.UpgradeWalletActivity
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.extensions.toast
import javax.inject.Inject

class LauncherActivity : BaseMvpActivity<LauncherView, LauncherPresenter>(), LauncherView {

    @Suppress("MemberVisibilityCanBePrivate")
    @Inject
    lateinit var launcherPresenter: LauncherPresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)

        val handler = Handler()
        handler.postDelayed(DelayStartRunnable(this), 500)
    }

    override fun logScreenView() = Unit

    override fun createPresenter() = launcherPresenter

    override fun getView() = this

    override fun getPageIntent(): Intent = intent

    override fun onNoGuid() {
        startSingleActivity(LandingActivity::class.java, null)
    }

    override fun onRequestPin() {
        startSingleActivity(PinEntryActivity::class.java, null)
    }

    override fun onCorruptPayload() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(getString(R.string.not_sane_error))
            .setCancelable(false)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.clearCredentialsAndRestart()
            }
            .show()
    }

    override fun onRequestUpgrade() {
        startActivity(Intent(this, UpgradeWalletActivity::class.java))
        finish()
    }

    override fun onStartMainActivity() {
        startSingleActivity(MainActivity::class.java, null)
    }

    override fun onStartOnboarding(emailOnly: Boolean) {
        val bundle = Bundle().apply { putBoolean(EXTRAS_EMAIL_ONLY, emailOnly) }
        startSingleActivity(OnboardingActivity::class.java, bundle)
    }

    override fun onReEnterPassword() {
        startSingleActivity(PasswordRequiredActivity::class.java, null)
    }

    override fun showToast(message: Int, toastType: String) = toast(message, toastType)

    private fun startSingleActivity(clazz: Class<*>, extras: Bundle?) {
        val intent = Intent(this, clazz).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        extras?.let { intent.putExtras(extras) }
        startActivity(intent)
    }

    private class DelayStartRunnable internal constructor(
        private val activity: LauncherActivity
    ) : Runnable {

        override fun run() {
            if (activity.presenter != null && !activity.isFinishing) {
                activity.onViewReady()
            }
        }
    }
}

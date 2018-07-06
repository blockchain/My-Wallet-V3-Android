package piuk.blockchain.android.ui.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v7.app.AlertDialog
import android.view.MotionEvent
import kotlinx.android.synthetic.main.activity_landing.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.connectivity.ConnectivityStatus
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.createwallet.CreateWalletActivity
import piuk.blockchain.android.ui.login.LoginActivity
import piuk.blockchain.android.ui.recover.RecoverFundsActivity
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.OverlayDetection
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import javax.inject.Inject

@Suppress("MemberVisibilityCanBePrivate")
class LandingActivity : BaseMvpActivity<LandingView, LandingPresenter>(), LandingView {

    @Inject
    lateinit var landingPresenter: LandingPresenter
    @Inject
    lateinit var appUtil: AppUtil
    @Inject
    lateinit var overlayDetection: OverlayDetection

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_landing)

        create.setOnClickListener { startCreateActivity() }
        login.setOnClickListener { startLoginActivity() }
        recoverFunds.setOnClickListener { showFundRecoveryWarning() }

        if (!ConnectivityStatus.hasConnectivity(this)) {
            showConnectivityWarning()
        } else {
            presenter.initPreLoginPrompts(this)
        }

        onViewReady()
    }

    override fun logScreenView() = Unit

    override fun createPresenter() = landingPresenter

    override fun getView() = this

    override fun startLogoutTimer() {
        // No-op
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        // Test for screen overlays before user creates a new wallet or enters confidential information
        return overlayDetection.detectObscuredWindow(this, event) || super.dispatchTouchEvent(event)
    }

    override fun showDebugMenu() {
        buttonSettings.visible()
        buttonSettings.setOnClickListener {
            EnvironmentSwitcher(this, prefsUtil, appUtil).showDebugMenu()
        }
    }

    override fun showToast(message: String, toastType: String) = toast(message, toastType)

    private fun startCreateActivity() {
        val intent = Intent(this, CreateWalletActivity::class.java)
        startActivity(intent)
    }

    private fun startLoginActivity() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    private fun startRecoveryActivityFlow() {
        val intent = Intent(this, RecoverFundsActivity::class.java)
        startActivity(intent)
    }

    private fun showFundRecoveryWarning() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.recover_funds_warning_message)
            .setPositiveButton(R.string.dialog_continue) { _, _ -> startRecoveryActivityFlow() }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
            .show()
    }

    private fun showConnectivityWarning() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setMessage(getString(R.string.check_connectivity_exit))
            .setCancelable(false)
            .setNegativeButton(R.string.exit) { _, _ -> finishAffinity() }
            .setPositiveButton(R.string.retry) { _, _ ->
                val intent = Intent(this, LandingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            }
            .create()
            .show()
    }

    override fun showWarningPrompt(alertDialog: AlertDialog) {
        val handler = Handler()
        handler.postDelayed({
            if (!isFinishing) {
                alertDialog.show()
            }
        }, 100)
    }

    companion object {

        @JvmStatic
        fun start(context: Context) {
            val intent = Intent(context, LandingActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}

package piuk.blockchain.android.ui.login

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import kotlinx.android.synthetic.main.activity_login.button_manual_pair
import kotlinx.android.synthetic.main.activity_login.button_scan
import kotlinx.android.synthetic.main.activity_login.mainLayout
import kotlinx.android.synthetic.main.activity_login.pairingFirstStep
import kotlinx.android.synthetic.main.toolbar_general.toolbar_general
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.extensions.toast
import timber.log.Timber
import javax.inject.Inject

@Suppress("MemberVisibilityCanBePrivate")
class LoginActivity : BaseMvpActivity<LoginView, LoginPresenter>(), LoginView {

    @Inject lateinit var loginPresenter: LoginPresenter
    @Inject lateinit var appUtil: AppUtil
    @Inject lateinit var environmentConfig: EnvironmentConfig

    private var progressDialog: MaterialProgressDialog? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setupToolbar(toolbar_general, R.string.pair_your_wallet)

        pairingFirstStep.text =
                getString(R.string.pair_wallet_step_1, environmentConfig.explorerUrl + "wallet")

        button_manual_pair.setOnClickListener { onClickManualPair() }
        button_scan.setOnClickListener { requestCameraPermissionIfNeeded() }
    }

    override fun showToast(message: Int, toastType: String) = toast(message, toastType)

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && requestCode == PAIRING_QR) {
            if (data?.getStringExtra(CaptureActivity.SCAN_RESULT) != null) {
                presenter.pairWithQR(data.getStringExtra(CaptureActivity.SCAN_RESULT))
            }
        }
    }

    override fun showProgressDialog(message: Int) {
        dismissProgressDialog()
        progressDialog = MaterialProgressDialog(
                this
        ).apply {
            setCancelable(false)
            setMessage(getString(message))
            if (!isFinishing) show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply {
            dismiss()
            progressDialog = null
        }
    }

    override fun startPinEntryActivity() {
        val intent = Intent(this, PinEntryActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun onSupportNavigateUp() = consume { onBackPressed() }

    override fun createPresenter() = loginPresenter

    override fun getView() = this

    override fun startLogoutTimer() = Unit

    private fun requestCameraPermissionIfNeeded() {
        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
                .with(mainLayout, R.string.request_camera_permission)
                .withButton(android.R.string.ok) { requestCameraPermissionIfNeeded() }
                .build()

        val grantedPermissionListener = object : BasePermissionListener() {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                startScanActivity()
            }
        }

        val compositePermissionListener =
                CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(compositePermissionListener)
                .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
                .check()
    }

    private fun onClickManualPair() {
        startActivity(Intent(this, ManualPairingActivity::class.java))
    }

    private fun startScanActivity() {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(this, CaptureActivity::class.java)
            intent.putExtra("SCAN_FORMATS", "QR_CODE")
            startActivityForResult(intent, PAIRING_QR)
        } else {
            showToast(R.string.camera_unavailable, ToastCustom.TYPE_ERROR)
        }
    }

    companion object {
        const val PAIRING_QR = 2005
    }
}
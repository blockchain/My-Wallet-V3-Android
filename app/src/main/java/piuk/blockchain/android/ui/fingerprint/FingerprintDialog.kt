package piuk.blockchain.android.ui.fingerprint

import android.annotation.TargetApi
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatDialogFragment
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import kotlinx.android.synthetic.main.dialog_fingerprint.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.androidcoreui.ui.base.BaseDialogFragment
import javax.inject.Inject

private const val ERROR_TIMEOUT_MILLIS: Long = 1500
private const val SUCCESS_DELAY_MILLIS: Long = 600
private const val FATAL_ERROR_TIMEOUT_MILLIS: Long = 3500

class FingerprintDialog : BaseDialogFragment<FingerprintView, FingerprintPresenter>(),
    FingerprintView {

    @Inject
    lateinit var fingerprintPresenter: FingerprintPresenter
    private var authCallback: FingerprintAuthCallback? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (authCallback == null) {
            throw RuntimeException("Auth Callback is null, have you passed in into the dialog via setAuthCallback?")
        }
        setStyle(AppCompatDialogFragment.STYLE_NORMAL, android.R.style.Theme_Material_Light_Dialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog.apply {
            setTitle(getString(R.string.fingerprint_login_title))
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            setOnKeyListener(BackButtonListener(authCallback!!))
        }

        return inflater.inflate(R.layout.dialog_fingerprint, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_cancel.setOnClickListener { authCallback!!.onCanceled() }

        onViewReady()
    }

    fun setAuthCallback(authCallback: FingerprintAuthCallback) {
        this.authCallback = authCallback
    }

    override fun setCancelButtonText(@StringRes text: Int) {
        button_cancel.setText(text)
    }

    override fun setDescriptionText(@StringRes text: Int) {
        textview_description.setText(text)
    }

    override fun setStatusText(@StringRes text: Int) {
        textview_status.setText(text)
    }

    override fun setStatusText(text: String) {
        textview_status.text = text
    }

    override fun setStatusTextColor(@ColorRes color: Int) {
        textview_status.setTextColor(ContextCompat.getColor(context!!, color))
    }

    override fun setIcon(@DrawableRes drawable: Int) {
        icon_fingerprint.setImageResource(drawable)
    }

    override fun getBundle(): Bundle = arguments!!

    override fun onAuthenticated(data: String?) {
        textview_status.removeCallbacks(resetErrorTextRunnable)
        icon_fingerprint.postDelayed({ authCallback!!.onAuthenticated(data) }, SUCCESS_DELAY_MILLIS)
    }

    override fun onRecoverableError() {
        showErrorAnimation(ERROR_TIMEOUT_MILLIS)
    }

    override fun onFatalError() {
        showErrorAnimation(FATAL_ERROR_TIMEOUT_MILLIS)
        icon_fingerprint.postDelayed({ authCallback!!.onCanceled() }, FATAL_ERROR_TIMEOUT_MILLIS)
    }

    override fun onCanceled() {
        authCallback!!.onCanceled()
    }

    override fun createPresenter() = fingerprintPresenter

    override fun getMvpView() = this

    override fun onSaveInstanceState(outState: Bundle) {
        // This is to fix a long-standing bug in the Android framework
    }

    private val resetErrorTextRunnable = Runnable {
        context?.run {
            textview_status.setTextColor(ContextCompat.getColor(this, R.color.primary_gray_medium))
            textview_status.text = getString(R.string.fingerprint_hint)
            icon_fingerprint.setImageResource(R.drawable.ic_fingerprint_logo)
        }
    }

    private fun showErrorAnimation(timeout: Long) {
        val shake = AnimationUtils.loadAnimation(context, R.anim.fingerprint_failed_shake)
        icon_fingerprint.animation = shake
        icon_fingerprint.animate()
        textview_status.removeCallbacks(resetErrorTextRunnable)
        textview_status.postDelayed(resetErrorTextRunnable, timeout)
    }

    private class BackButtonListener constructor(
        private val fingerprintAuthCallback: FingerprintAuthCallback
    ) : DialogInterface.OnKeyListener {

        override fun onKey(dialog: DialogInterface, keyCode: Int, event: KeyEvent): Boolean {
            return if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (event.action != KeyEvent.ACTION_DOWN) {
                    true
                } else {
                    fingerprintAuthCallback.onCanceled()
                    true
                }
            } else {
                false
            }
        }
    }

    interface FingerprintAuthCallback {

        fun onAuthenticated(data: String?)

        fun onCanceled()
    }

    companion object {

        const val TAG = "FingerprintDialog"
        const val KEY_BUNDLE_PIN_CODE = "pin_code"
        const val KEY_BUNDLE_STAGE = "stage"

        fun newInstance(pin: String, stage: FingerprintStage): FingerprintDialog {
            val fragment = FingerprintDialog()
            fragment.arguments = Bundle().apply {
                putString(KEY_BUNDLE_PIN_CODE, pin)
                putString(KEY_BUNDLE_STAGE, stage.name)
            }
            return fragment
        }
    }
}

/**
 * Indicate which stage of the auth process the user is currently at
 */
enum class FingerprintStage {
    REGISTER_FINGERPRINT,
    AUTHENTICATE
}

package piuk.blockchain.android.ui.start

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.view.MenuItem
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.AppCompatEditText
import com.blockchain.koin.scopedInject
import androidx.appcompat.widget.Toolbar
import info.blockchain.wallet.api.data.Settings
import kotlinx.android.synthetic.main.activity_manual_pairing.*
import org.json.JSONObject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.base.MvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils

class ManualPairingActivity : MvpActivity<ManualPairingView, ManualPairingPresenter>(),
    ManualPairingView {

    override val view: ManualPairingView = this
    override val presenter: ManualPairingPresenter by scopedInject()

    private val guid: String="d7be7cce-b2fe-46b7-afc1-8cf56dde81f4"
//        get() = wallet_id.text.toString()
    private val password: String="ItsRainingDogs"
//        get() = wallet_pass.text.toString()

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manual_pairing)

        val toolbar = findViewById<Toolbar>(R.id.toolbar_general)
        setupToolbar(toolbar, R.string.manual_pairing)

        command_next.setOnClickListener { presenter.onContinueClicked(guid, password) }

        wallet_pass.setOnEditorActionListener { _, i, _ ->
            if (i == EditorInfo.IME_ACTION_GO) {
                presenter.onContinueClicked(guid, password)
            }
            true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun showToast(@StringRes messageId: Int, @ToastCustom.ToastType toastType: String) {
        ToastCustom.makeText(this, getString(messageId), ToastCustom.LENGTH_SHORT, toastType)
    }

    override fun goToPinPage() {
        startActivity(Intent(this, PinEntryActivity::class.java))
    }

    override fun updateWaitingForAuthDialog(secondsRemaining: Int) {
        updateProgressDialog(getString(R.string.check_email_to_auth_login) + " " + secondsRemaining)
    }

    override fun showTwoFactorCodeNeededDialog(
        responseObject: JSONObject,
        sessionId: String,
        authType: Int,
        guid: String,
        password: String
    ) {
        ViewUtils.hideKeyboard(this)

        val editText = AppCompatEditText(this)
        editText.setHint(R.string.two_factor_dialog_hint)

        val message = when (authType) {
            Settings.AUTH_TYPE_GOOGLE_AUTHENTICATOR -> {
                editText.inputType = InputType.TYPE_NUMBER_VARIATION_NORMAL
                editText.keyListener = DigitsKeyListener.getInstance("1234567890")
                R.string.two_factor_dialog_message_authenticator
            }
            Settings.AUTH_TYPE_SMS -> {
                editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                R.string.two_factor_dialog_message_sms
            }
            else -> throw IllegalArgumentException("Auth Type $authType should not be passed to this function")
        }

        showAlert(AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.two_factor_dialog_title)
            .setMessage(message)
            .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
            .setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.submitTwoFactorCode(
                    responseObject,
                    sessionId,
                    guid,
                    password,
                    editText.text.toString()
                )
            }
            .setNegativeButton(android.R.string.cancel, null)
            .create()
        )
    }

    override fun resetPasswordField() {
        if (!isFinishing)
            wallet_pass.setText("")
    }

    public override fun onDestroy() {
        currentFocus?.let {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        dismissProgressDialog()
        presenter.cancelAuthTimer()
        super.onDestroy()
    }
}

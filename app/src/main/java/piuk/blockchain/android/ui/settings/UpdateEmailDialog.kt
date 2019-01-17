package piuk.blockchain.android.ui.settings

import android.content.Context
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import info.blockchain.wallet.util.FormatsUtil
import piuk.blockchain.android.R
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils

internal fun showUpdateEmailDialog(activity: Context, settingsPresenter: SettingsPresenter) {
    val originalEmail = settingsPresenter.email

    val editText = AppCompatEditText(activity)
        .apply {
            inputType = InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setText(originalEmail)
            setSelection(text.length)
        }

    val alertDialog = AlertDialog.Builder(activity, R.style.AlertDialogStyle)
        .setTitle(R.string.email)
        .setMessage(R.string.verify_email2)
        .setView(ViewUtils.getAlertDialogPaddedView(activity, editText))
        .setCancelable(false)
        .setPositiveButton(R.string.update) { _, _ ->
            val newEmail = editText.text.toString()
            if (!FormatsUtil.isValidEmailAddress(newEmail)) {
                ToastCustom.makeText(
                    activity,
                    activity.getString(R.string.invalid_email),
                    ToastCustom.LENGTH_SHORT,
                    ToastCustom.TYPE_ERROR
                )
            } else {
                settingsPresenter.updateEmail(newEmail)
            }
        }
        .also {
            if (!settingsPresenter.isEmailVerified) {
                it.setNeutralButton(R.string.resend) { _, _ ->
                    // Resend verification code
                    settingsPresenter.updateEmail(originalEmail)
                }
            }
        }
        .setNegativeButton(android.R.string.cancel, null)
        .create()

    alertDialog.show()

    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false

    editText.addTextChangedListener(object : TextWatcher {

        override fun afterTextChanged(s: Editable) {
            val email = s.toString()
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                email != originalEmail && FormatsUtil.isValidEmailAddress(email)
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
    })
}

package piuk.blockchain.android.data.datamanagers

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatDialogFragment
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.auth.LandingActivity
import piuk.blockchain.android.ui.auth.PinEntryActivity
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.home.SecurityPromptDialog
import piuk.blockchain.android.ui.settings.SettingsActivity
import piuk.blockchain.android.ui.settings.SettingsFragment
import piuk.blockchain.android.ui.settings.SettingsFragment.EXTRA_SHOW_ADD_EMAIL_DIALOG
import piuk.blockchain.android.util.RootUtil
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.injection.PresenterScope
import piuk.blockchain.androidcore.utils.PrefsUtil
import javax.inject.Inject

@PresenterScope
class PromptManager @Inject constructor(
    private val prefsUtil: PrefsUtil,
    private val payloadDataManager: PayloadDataManager,
    private val transactionListDataManager: TransactionListDataManager
) {

    fun getPreLoginPrompts(context: Context): Observable<List<AlertDialog>> {
        val list = mutableListOf<AlertDialog>()

        if (isRooted()) list.add(getRootWarningDialog(context))

        return Observable.fromArray(list)
    }

    fun getCustomPrompts(
        context: Context,
        settings: Settings
    ): Observable<List<AppCompatDialogFragment>> {
        val list = mutableListOf<AppCompatDialogFragment>()

        if (isBackedUpReminderAllowed()) list.add(getBackupCustomDialog(context))
        if (is2FAReminderAllowed(settings)) list.add(get2FACustomDialog(context))
        if (isVerifyEmailReminderAllowed(settings)) {
            storeTimeOfLastSecurityPrompt()
            list.add(getVerifyEmailCustomDialog(context))
        }

        return Observable.fromArray(list)
    }

    private fun isFirstRun(): Boolean {
        return prefsUtil.getValue(PrefsUtil.KEY_APP_VISITS, 0) == 0
    }

    private fun getAppVisitCount(): Int {
        return prefsUtil.getValue(PrefsUtil.KEY_APP_VISITS, 0)
    }

    private fun getGuid(): String {
        return prefsUtil.getValue(PrefsUtil.KEY_GUID, "")
    }

    private fun getIfNeverPrompt2Fa(): Boolean {
        return prefsUtil.getValue(PrefsUtil.KEY_SECURITY_TWO_FA_NEVER, false)
    }

    private fun getTimeOfLastSecurityPrompt(): Long {
        return prefsUtil.getValue(PrefsUtil.KEY_SECURITY_TIME_ELAPSED, 0L)
    }

    private fun storeTimeOfLastSecurityPrompt() {
        prefsUtil.setValue(PrefsUtil.KEY_SECURITY_TIME_ELAPSED, System.currentTimeMillis())
    }

    private fun neverPrompt2Fa() {
        prefsUtil.setValue(PrefsUtil.KEY_SECURITY_TWO_FA_NEVER, true)
    }

    private fun getIfNeverPromptBackup(): Boolean {
        return prefsUtil.getValue(PrefsUtil.KEY_SECURITY_BACKUP_NEVER, false)
    }

    private fun setBackupCompleted() {
        prefsUtil.setValue(PrefsUtil.KEY_SECURITY_BACKUP_NEVER, true)
    }

    private fun hasTransactions(): Boolean {
        return !transactionListDataManager.getTransactionList().isEmpty()
    }

    private fun isRooted(): Boolean {
        return RootUtil().isDeviceRooted && !prefsUtil.getValue("disable_root_warning", false)
    }

    private fun isVerifyEmailReminderAllowed(settings: Settings): Boolean {
        val isCorrectTime = getTimeOfLastSecurityPrompt() == 0L ||
            System.currentTimeMillis() - getTimeOfLastSecurityPrompt() >= ONE_MONTH
        return !isFirstRun() && !settings.isEmailVerified && !settings.email.isEmpty() && isCorrectTime
    }

    private fun is2FAReminderAllowed(settings: Settings): Boolean {
        // On third visit onwards, prompt 2FA
        val isEnoughVisits = (!getIfNeverPrompt2Fa() && getAppVisitCount() >= 3)
        val isNeeded = !settings.isSmsVerified && settings.authType == Settings.AUTH_TYPE_OFF
        val isCorrectTime = getTimeOfLastSecurityPrompt() == 0L ||
            System.currentTimeMillis() - getTimeOfLastSecurityPrompt() >= ONE_MONTH

        if (isEnoughVisits && isNeeded && isCorrectTime) {
            storeTimeOfLastSecurityPrompt()
        }

        return !isFirstRun() && isEnoughVisits && isNeeded && isCorrectTime
    }

    private fun isBackedUpReminderAllowed(): Boolean {
        val isAllowed = !isFirstRun() &&
            !getIfNeverPromptBackup() &&
            !payloadDataManager.isBackedUp && hasTransactions()

        val isCorrectTime = getTimeOfLastSecurityPrompt() == 0L ||
            System.currentTimeMillis() - getTimeOfLastSecurityPrompt() >= ONE_MONTH

        if (isAllowed && isCorrectTime) {
            storeTimeOfLastSecurityPrompt()
            return true
        }

        return false
    }

    private fun isLastBackupReminder(): Boolean {
        return System.currentTimeMillis() - getTimeOfLastSecurityPrompt() >= ONE_MONTH
    }

    // ********************************************************************************************//
    // *                              Default Prompts                                             *//
    // ********************************************************************************************//

    private fun getRootWarningDialog(context: Context): AlertDialog {
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setMessage(R.string.device_rooted)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_continue, null)
            .create()
    }

    private fun getConnectivityDialog(context: Context): AlertDialog {
        return AlertDialog.Builder(context, R.style.AlertDialogStyle)
            .setMessage(R.string.check_connectivity_exit)
            .setCancelable(false)
            .setPositiveButton(R.string.dialog_continue) { _, _ ->
                if (getGuid().isEmpty()) {
                    LandingActivity.start(context)
                } else {
                    PinEntryActivity.start(context)
                }
            }.create()
    }

    // ********************************************************************************************//
    // *                         Custom Security Prompts                                          *//
    // ********************************************************************************************//

    private fun getVerifyEmailCustomDialog(context: Context): SecurityPromptDialog {
        val securityPromptDialog = SecurityPromptDialog.newInstance(
            R.string.security_centre_add_email_title,
            context.getString(R.string.security_centre_add_email_message),
            R.drawable.vector_email,
            R.string.security_centre_add_email_positive_button,
            true,
            false
        )
        securityPromptDialog.setPositiveButtonListener {
            securityPromptDialog.dismiss()
            val bundle = Bundle()
            bundle.putBoolean(EXTRA_SHOW_ADD_EMAIL_DIALOG, true)
            SettingsActivity.start(context, bundle)
        }

        securityPromptDialog.setNegativeButtonListener { securityPromptDialog.dismiss() }

        return securityPromptDialog
    }

    private fun get2FACustomDialog(context: Context): SecurityPromptDialog {
        val securityPromptDialog = SecurityPromptDialog.newInstance(
            R.string.two_fa,
            context.getString(R.string.security_centre_two_fa_message),
            R.drawable.vector_mobile,
            R.string.enable,
            true,
            true
        )
        securityPromptDialog.setPositiveButtonListener {
            securityPromptDialog.dismiss()
            if (securityPromptDialog.isChecked) {
                neverPrompt2Fa()
            }
            val bundle = Bundle()
            bundle.putBoolean(SettingsFragment.EXTRA_SHOW_TWO_FA_DIALOG, true)
            SettingsActivity.start(context, bundle)
        }

        securityPromptDialog.setNegativeButtonListener {
            securityPromptDialog.dismiss()
            if (securityPromptDialog.isChecked) {
                neverPrompt2Fa()
            }
        }

        return securityPromptDialog
    }

    private fun getBackupCustomDialog(context: Context): SecurityPromptDialog {
        val securityPromptDialog = SecurityPromptDialog.newInstance(
            R.string.security_centre_backup_title,
            context.getString(R.string.security_centre_backup_message),
            R.drawable.vector_lock,
            R.string.security_centre_backup_positive_button,
            true,
            isLastBackupReminder()
        )
        securityPromptDialog.setPositiveButtonListener {
            securityPromptDialog.dismiss()
            if (securityPromptDialog.isChecked) {
                setBackupCompleted()
            }
            BackupWalletActivity.start(context, null)
        }

        securityPromptDialog.setNegativeButtonListener {
            securityPromptDialog.dismiss()
            if (securityPromptDialog.isChecked) {
                setBackupCompleted()
            }
        }

        return securityPromptDialog
    }

    companion object {

        private const val ONE_MONTH = 28 * 24 * 60 * 60 * 1000L
    }
}
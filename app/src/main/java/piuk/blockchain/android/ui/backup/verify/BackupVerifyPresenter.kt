package piuk.blockchain.android.ui.backup.verify

import android.support.annotation.VisibleForTesting
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.backup.BackupWalletActivity
import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment.Companion.ARGUMENT_SECOND_PASSWORD
import piuk.blockchain.android.util.BackupWalletUtil
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import javax.inject.Inject

class BackupVerifyPresenter @Inject constructor(
    private val payloadDataManager: PayloadDataManager,
    private val prefsUtil: PrefsUtil,
    private val backupWalletUtil: BackupWalletUtil
) : BasePresenter<BackupVerifyView>() {

    private val sequence by unsafeLazy { getBackupConfirmSequence() }

    override fun onViewReady() {
        view.showWordHints(listOf(sequence[0].first, sequence[1].first, sequence[2].first))
    }

    internal fun onVerifyClicked(firstWord: String, secondWord: String, thirdWord: String) {
        if (firstWord.trim { it <= ' ' }.equals(sequence[0].second, ignoreCase = true) &&
            secondWord.trim { it <= ' ' }.equals(sequence[1].second, ignoreCase = true) &&
            thirdWord.trim { it <= ' ' }.equals(sequence[2].second, ignoreCase = true)
        ) {

            updateBackupStatus()
        } else {
            view.showToast(R.string.backup_word_mismatch, ToastCustom.TYPE_ERROR)
        }
    }

    @VisibleForTesting
    internal fun updateBackupStatus() {
        payloadDataManager.wallet!!.hdWallets[0].isMnemonicVerified = true

        payloadDataManager.syncPayloadWithServer()
            .doOnSubscribe { view.showProgressDialog() }
            .doAfterTerminate { view.hideProgressDialog() }
            .addToCompositeDisposable(this)
            .subscribe(
                {
                    prefsUtil.setValue(
                        BackupWalletActivity.BACKUP_DATE_KEY,
                        (System.currentTimeMillis() / 1000).toInt()
                    )
                    view.showToast(R.string.backup_confirmed, ToastCustom.TYPE_OK)
                    view.showCompletedFragment()
                },
                { throwable ->
                    Timber.e(throwable)
                    view.showToast(R.string.api_fail, ToastCustom.TYPE_ERROR)
                    view.showStartingFragment()
                }
            )
    }

    private fun getBackupConfirmSequence(): List<Pair<Int, String>> {
        val bundle = view.getPageBundle()
        val secondPassword = bundle?.getString(ARGUMENT_SECOND_PASSWORD)
        return backupWalletUtil.getConfirmSequence(secondPassword)
    }
}

package piuk.blockchain.android.ui.backup.wordlist

import piuk.blockchain.android.ui.backup.wordlist.BackupWalletWordListFragment.Companion.ARGUMENT_SECOND_PASSWORD
import piuk.blockchain.android.util.BackupWalletUtil
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import javax.inject.Inject

class BackupWalletWordListPresenter @Inject constructor(
    private val backupWalletUtil: BackupWalletUtil
) : BasePresenter<BackupWalletWordListView>() {

    internal var secondPassword: String? = null
    private var mnemonic: List<String>? = null

    override fun onViewReady() {
        val bundle = view.getPageBundle()
        secondPassword = bundle?.getString(ARGUMENT_SECOND_PASSWORD)

        mnemonic = backupWalletUtil.getMnemonic(secondPassword)
        if (mnemonic == null) {
            view.finish()
        }
    }

    internal fun getWordForIndex(index: Int) = mnemonic!![index]

    internal fun getMnemonicSize() = mnemonic?.size ?: -1
}
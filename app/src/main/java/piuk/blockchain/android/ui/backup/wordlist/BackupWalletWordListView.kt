package piuk.blockchain.android.ui.backup.wordlist

import android.os.Bundle
import piuk.blockchain.androidcoreui.ui.base.View

interface BackupWalletWordListView : View {

    fun getPageBundle(): Bundle?

    fun finish()
}
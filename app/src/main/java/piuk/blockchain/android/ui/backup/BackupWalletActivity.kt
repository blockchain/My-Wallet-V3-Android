package piuk.blockchain.android.ui.backup

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import info.blockchain.wallet.payload.PayloadManager
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import org.koin.android.ext.android.inject
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedFragment
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity

class BackupWalletActivity : BaseAuthActivity() {

    private val payloadManger: PayloadManager by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_backup_wallet)

        setupToolbar(toolbar_general, R.string.backup_wallet)

        if (isBackedUp()) {
            startFragment(
                BackupWalletCompletedFragment.newInstance(false),
                BackupWalletCompletedFragment.TAG
            )
        } else {
            startFragment(BackupWalletStartingFragment(), BackupWalletStartingFragment.TAG)
        }
    }

    private fun startFragment(fragment: Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.content_frame, fragment)
            .addToBackStack(tag)
            .commit()
    }

    override fun onBackPressed() {
        if (fragmentManager.backStackEntryCount <= 1) {
            finish()
        } else {
            fragmentManager.popBackStack()
        }
    }

    override fun enforceFlagSecure() = true

    override fun onSupportNavigateUp() =
        consume { onBackPressed() }

    private fun isBackedUp() =
        payloadManger.payload?.hdWallets?.get(0)?.isMnemonicVerified ?: false

    companion object {

        const val BACKUP_DATE_KEY = "BACKUP_DATE_KEY"

        fun start(context: Context, extras: Bundle?) {
            val starter = Intent(context, BackupWalletActivity::class.java)
            if (extras != null) starter.putExtras(extras)
            context.startActivity(starter)
        }
    }
}
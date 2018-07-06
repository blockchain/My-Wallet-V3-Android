package piuk.blockchain.android.ui.backup.completed

import android.app.FragmentManager
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_backup_complete.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment
import piuk.blockchain.android.ui.backup.transfer.ConfirmFundsTransferDialogFragment
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class BackupWalletCompletedFragment :
    BaseFragment<BackupWalletCompletedView, BackupWalletCompletedPresenter>(),
    BackupWalletCompletedView {

    @Inject
    lateinit var backupWalletCompletedPresenter: BackupWalletCompletedPresenter

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_backup_complete)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_backup_again.setOnClickListener { onBackupAgainRequested() }

        if (arguments?.getBoolean(KEY_CHECK_TRANSFER) == true) {
            presenter.checkTransferableFunds()
        }

        onViewReady()
    }

    override fun showLastBackupDate(lastBackup: Long) {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        val date = dateFormat.format(Date(lastBackup * 1000))
        val message = String.format(resources.getString(R.string.backup_last), date)
        subheading_date.text = message
    }

    override fun hideLastBackupDate() {
        subheading_date.gone()
    }

    override fun showTransferFundsPrompt() {
        val alertDialog = AlertDialog.Builder(context!!, R.style.AlertDialogStyle)
            .setTitle(R.string.transfer_funds)
            .setMessage(getString(R.string.transfer_recommend))
            .setPositiveButton(R.string.transfer) { _, _ -> showTransferFundsConfirmationDialog() }
            .setNegativeButton(R.string.not_now, null)
            .create()

        alertDialog.show()

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).apply {
            setTextColor(ContextCompat.getColor(context, R.color.primary_gray_dark))
        }
    }

    override fun createPresenter() = backupWalletCompletedPresenter

    override fun getMvpView() = this

    private fun onBackupAgainRequested() {
        activity?.run {
            supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            supportFragmentManager.beginTransaction()
                .replace(R.id.content_frame, BackupWalletStartingFragment())
                .addToBackStack(BackupWalletStartingFragment.TAG)
                .commit()
        }
    }

    private fun showTransferFundsConfirmationDialog() {
        activity?.let {
            val fragment = ConfirmFundsTransferDialogFragment.newInstance()
            fragment.show(it.supportFragmentManager, ConfirmFundsTransferDialogFragment.TAG)
        }
    }

    companion object {

        const val TAG = "BackupWalletCompletedFragment"
        private val KEY_CHECK_TRANSFER = "check_transfer"

        fun newInstance(checkTransfer: Boolean): BackupWalletCompletedFragment {
            val fragment = BackupWalletCompletedFragment()
            fragment.arguments = Bundle().apply { putBoolean(KEY_CHECK_TRANSFER, checkTransfer) }
            return fragment
        }
    }
}

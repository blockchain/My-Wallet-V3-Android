package piuk.blockchain.android.ui.backup.verify

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import kotlinx.android.synthetic.main.fragment_backup_wallet_verify.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.backup.completed.BackupWalletCompletedFragment
import piuk.blockchain.android.ui.backup.start.BackupWalletStartingFragment
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.toast
import javax.inject.Inject

class BackupWalletVerifyFragment : BaseFragment<BackupVerifyView, BackupVerifyPresenter>(),
        BackupVerifyView {

    @Inject lateinit var backupVerifyPresenter: BackupVerifyPresenter

    private var progressDialog: MaterialProgressDialog? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = container!!.inflate(R.layout.fragment_backup_wallet_verify)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onViewReady()

        button_verify.setOnClickListener {
            presenter.onVerifyClicked(
                    edittext_first_word.text.toString(),
                    edittext_second_word.text.toString(),
                    edittext_third_word.text.toString()
            )
        }
    }

    override fun showWordHints(hints: List<Int>) {
        val mnemonicRequestHint = resources.getStringArray(R.array.mnemonic_word_requests)
        edittext_first_word.hint = mnemonicRequestHint[hints[0]]
        edittext_second_word.hint = mnemonicRequestHint[hints[1]]
        edittext_third_word.hint = mnemonicRequestHint[hints[2]]
    }

    override fun getPageBundle(): Bundle? = arguments

    override fun createPresenter() = backupVerifyPresenter

    override fun getMvpView() = this

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(activity).apply {
            setMessage("${getString(R.string.please_wait)}…")
            setCancelable(false)
            show()
        }
    }

    override fun hideProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog?.dismiss()
        }
    }

    override fun showCompletedFragment() {
        popAllAndStartFragment(
                BackupWalletCompletedFragment.newInstance(true),
                BackupWalletCompletedFragment.TAG
        )
    }

    override fun showStartingFragment() {
        popAllAndStartFragment(BackupWalletStartingFragment(), BackupWalletStartingFragment.TAG)
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val view = activity.currentFocus
        if (view != null) {
            val imm = activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun popAllAndStartFragment(fragment: Fragment, tag: String) {
        fragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        fragmentManager.beginTransaction()
                .replace(R.id.content_frame, fragment)
                .addToBackStack(tag)
                .commit()
    }

}
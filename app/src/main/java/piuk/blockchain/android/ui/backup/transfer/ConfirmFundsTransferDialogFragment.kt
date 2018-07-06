package piuk.blockchain.android.ui.backup.transfer

import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.app.DialogFragment
import android.support.v4.content.LocalBroadcastManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import kotlinx.android.synthetic.main.dialog_transfer_funds.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.SecondPasswordHandler
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.androidcoreui.ui.base.BaseDialogFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.helperfunctions.onItemSelectedListener
import java.util.Locale
import javax.inject.Inject

class ConfirmFundsTransferDialogFragment :
    BaseDialogFragment<ConfirmFundsTransferView, ConfirmFundsTransferPresenter>(),
    ConfirmFundsTransferView {

    @Inject
    lateinit var confirmFundsTransferPresenter: ConfirmFundsTransferPresenter
    override val locale: Locale = Locale.getDefault()

    private var progressDialog: MaterialProgressDialog? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        dialog.apply { setCancelable(true) }

        // You'd think we could use container?.inflate(...) here, but container is null at this point
        return inflater.inflate(R.layout.dialog_transfer_funds, container, false).apply {
            isFocusableInTouchMode = true
            requestFocus()
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val window = dialog.window
        window?.let {
            val params = window.attributes
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            params.height = WindowManager.LayoutParams.MATCH_PARENT
            window.attributes = params
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toolbar.setNavigationOnClickListener { dismiss() }
        toolbar.title = getString(R.string.transfer_confirm)

        val receiveToAdapter = AddressAdapter(
            activity,
            R.layout.spinner_item,
            presenter.getReceiveToList(),
            true
        ).apply { setDropDownViewResource(R.layout.spinner_dropdown) }
        spinner_destination.adapter = receiveToAdapter
        spinner_destination.onItemSelectedListener =
            onItemSelectedListener {
                spinner_destination.setSelection(spinner_destination.selectedItemPosition)
                presenter.accountSelected(spinner_destination.selectedItemPosition)
            }

        spinner_destination.viewTreeObserver.addOnGlobalLayoutListener(
            object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    spinner_destination.viewTreeObserver.removeOnGlobalLayoutListener(this)
                    spinner_destination.dropDownWidth = spinner_destination.width
                }
            })

        button_transfer_all.setOnClickListener {
            SecondPasswordHandler(activity).validate(object : SecondPasswordHandler.ResultListener {
                override fun onNoSecondPassword() {
                    presenter.sendPayment(null)
                }

                override fun onSecondPasswordValidated(validateSecondPassword: String) {
                    presenter.sendPayment(validateSecondPassword)
                }
            })
        }

        spinner_destination.setSelection(presenter.getDefaultAccount())

        onViewReady()
    }

    override fun showProgressDialog() {
        hideProgressDialog()
        if (activity != null && !activity!!.isFinishing) {
            progressDialog = MaterialProgressDialog(
                context
            ).apply {
                setMessage(getString(R.string.please_wait))
                setCancelable(false)
                show()
            }
        }
    }

    override fun hideProgressDialog() {
        if (progressDialog != null && progressDialog!!.isShowing) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    override fun onUiUpdated() {
        loading_layout.gone()
    }

    override fun updateFromLabel(label: String) {
        label_from.text = label
    }

    override fun updateTransferAmountBtc(amount: String) {
        label_transfer_amount_btc.text = amount
    }

    override fun updateTransferAmountFiat(amount: String) {
        label_transfer_amount_fiat.text = amount
    }

    override fun updateFeeAmountBtc(amount: String) {
        label_fee_amount_btc.text = amount
    }

    override fun updateFeeAmountFiat(amount: String) {
        label_fee_amount_fiat.text = amount
    }

    override fun setPaymentButtonEnabled(enabled: Boolean) {
        button_transfer_all.isEnabled = enabled
    }

    override fun getIfArchiveChecked() = checkbox_archive.isChecked

    override fun dismissDialog() {
        activity?.run {
            val intent = Intent(BalanceFragment.ACTION_INTENT)
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
        }
        dismiss()
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        toast(message, toastType)
    }

    override fun createPresenter() = confirmFundsTransferPresenter

    override fun getMvpView() = this

    companion object {

        const val TAG = "ConfirmFundsTransferDialogFragment"

        @JvmStatic
        fun newInstance(): ConfirmFundsTransferDialogFragment {
            return ConfirmFundsTransferDialogFragment().apply {
                setStyle(DialogFragment.STYLE_NO_FRAME, R.style.FullscreenDialog)
            }
        }
    }
}

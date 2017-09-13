package piuk.blockchain.android.ui.contacts.payments

import android.content.Context
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_contact_payment_request_notes.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.contacts.models.PaymentRequestType
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.base.BaseAuthActivity
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.customviews.MaterialProgressDialog
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.util.ViewUtils
import piuk.blockchain.android.util.extensions.*
import javax.inject.Inject

class ContactConfirmRequestFragment : BaseFragment<ContactConfirmRequestView, ContactConfirmRequestPresenter>(),
        ContactConfirmRequestView {

    override val fragmentBundle: Bundle?
        get() = arguments
    override val note: String
        get() = edittext_description.getTextString()

    @Inject lateinit var paymentRequestPresenter: ContactConfirmRequestPresenter
    private var progressDialog: MaterialProgressDialog? = null
    private var listener: FragmentInteractionListener? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_contact_payment_request_notes)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        button_send.setOnClickListener {
            presenter.sendRequest()
            ViewUtils.hideKeyboard(activity)
        }

//        setupToolbar()
        onViewReady()
    }

    override fun contactLoaded(name: String) {
        textview_to_name.text = name
        edittext_description.hint = getString(R.string.contacts_confirm_shared_with, name)
        loading_layout.gone()
        main_layout.visible()
    }

    override fun updateAccountName(name: String) {
        textview_from_address.text = name
    }

    override fun updateTotalBtc(total: String) {
        textview_total_btc.text = total
    }

    override fun updateTotalFiat(total: String) {
        textview_total_fiat.text = total
    }

    override fun updatePaymentType(paymentRequestType: PaymentRequestType) {
        when (paymentRequestType) {
            PaymentRequestType.SEND -> button_send.setText(R.string.contacts_confirm_start_transaction)
            PaymentRequestType.REQUEST -> button_send.setText(R.string.contacts_confirm_request_payment)
            else -> throw IllegalArgumentException("This payment type is not supported by this Fragment")
        }
    }

    override fun finishPage() {
        listener?.onPageFinished()
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(context)
                .apply {
                    setCancelable(false)
                    setMessage(R.string.please_wait)
                    show()
                }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply {
            dismiss()
            progressDialog = null
        }
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        toast(message, toastType)
    }

    override fun onRequestSuccessful(
            paymentRequestType: PaymentRequestType,
            contactName: String,
            btcAmount: String
    ) {
        listener?.onRequestSuccessful(paymentRequestType, contactName, btcAmount)
    }

    override fun createPresenter() = paymentRequestPresenter

    override fun getMvpView() = this

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is FragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement FragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun setupToolbar() {
        // TODO: For now this isn't reset when leaving this page as onResume isn't triggered ¯\_(ツ)_/¯
        if ((activity as AppCompatActivity).supportActionBar != null) {
            (activity as BaseAuthActivity).setupToolbar(
                    (activity as MainActivity).supportActionBar, R.string.contacts_confirm_title)
        } else {
            finishPage()
        }
    }

    interface FragmentInteractionListener {

        fun onPageFinished()

        fun onRequestSuccessful(
                paymentRequestType: PaymentRequestType,
                contactName: String,
                btcAmount: String
        )

    }

    companion object {

        const val ARGUMENT_CONFIRMATION_DETAILS = "ARGUMENT_CONFIRMATION_DETAILS"
        const val ARGUMENT_CONTACT_ID = "ARGUMENT_CONTACT_ID"
        const val ARGUMENT_SATOSHIS = "ARGUMENT_SATOSHIS"
        const val ARGUMENT_REQUEST_TYPE = "ARGUMENT_REQUEST_TYPE"
        const val ARGUMENT_ACCOUNT_POSITION = "ARGUMENT_ACCOUNT_POSITION"

        @JvmStatic
        fun newInstance(
                confirmationDetails: PaymentConfirmationDetails,
                requestType: PaymentRequestType,
                contactId: String,
                satoshis: Long,
                accountPosition: Int
        ): ContactConfirmRequestFragment {

            val args = Bundle().apply {
                putParcelable(ARGUMENT_CONFIRMATION_DETAILS, confirmationDetails)
                putSerializable(ARGUMENT_REQUEST_TYPE, requestType)
                putString(ARGUMENT_CONTACT_ID, contactId)
                putLong(ARGUMENT_SATOSHIS, satoshis)
                putInt(ARGUMENT_ACCOUNT_POSITION, accountPosition)
            }
            return ContactConfirmRequestFragment().apply { arguments = args }
        }
    }

}

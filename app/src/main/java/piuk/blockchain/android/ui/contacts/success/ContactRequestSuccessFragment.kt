package piuk.blockchain.android.ui.contacts.success

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_contact_request_success.*
import piuk.blockchain.android.R
import piuk.blockchain.androidcore.data.contacts.models.PaymentRequestType
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ContactRequestSuccessFragment : Fragment() {

    private var listener: ContactsRequestSuccessListener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_contact_request_success)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            val bundle = arguments!!
            val requestType = bundle.getSerializable(ARGUMENT_REQUEST_TYPE)
            val contactName = bundle.getString(ARGUMENT_CONTACT_NAME)
            val btcAmount = bundle.getString(ARGUMENT_BTC_AMOUNT)
            when (requestType) {
                PaymentRequestType.REQUEST -> updateUiForRequest(contactName, btcAmount)
                PaymentRequestType.SEND -> updateUiForSend(contactName)
                PaymentRequestType.CONTACT ->
                    throw IllegalArgumentException("This case is not handled by this fragment")
            }
        }

        button_done.setOnClickListener { listener?.onRequestSuccessDismissed() }
    }

    private fun updateUiForSend(contactName: String) {
        textview_title.setText(R.string.contacts_request_success_tx_started_title)
        textview_description.text =
            getString(R.string.contacts_request_success_tx_started_description, contactName)
    }

    private fun updateUiForRequest(contactName: String, btcAmount: String) {
        textview_title.setText(R.string.contacts_request_success_sent_title)
        textview_description.text =
            getString(R.string.contacts_request_success_sent_description, btcAmount, contactName)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is ContactsRequestSuccessListener) {
            listener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement ContactsRequestSuccessListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    interface ContactsRequestSuccessListener {

        fun onRequestSuccessDismissed()
    }

    companion object {

        const val ARGUMENT_REQUEST_TYPE = "ARGUMENT_REQUEST_TYPE"
        const val ARGUMENT_CONTACT_NAME = "ARGUMENT_CONTACT_NAME"
        const val ARGUMENT_BTC_AMOUNT = "ARGUMENT_BTC_AMOUNT"

        @JvmStatic
        fun newInstance(
            requestType: PaymentRequestType,
            contactName: String,
            btcAmount: String
        ): ContactRequestSuccessFragment {
            val args = Bundle().apply {
                putSerializable(ARGUMENT_REQUEST_TYPE, requestType)
                putString(ARGUMENT_CONTACT_NAME, contactName)
                putString(ARGUMENT_BTC_AMOUNT, btcAmount)
            }
            return ContactRequestSuccessFragment().apply { arguments = args }
        }
    }
}
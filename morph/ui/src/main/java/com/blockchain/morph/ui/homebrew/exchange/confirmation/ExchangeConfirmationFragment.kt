package com.blockchain.morph.ui.homebrew.exchange.confirmation

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.blockchain.balance.colorRes
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.ExchangeLockedActivity
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewHostActivityListener
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.ui.extensions.throttledClicks
import info.blockchain.balance.CryptoCurrency
import kotlinx.android.parcel.Parcelize
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class ExchangeConfirmationFragment :
    BaseMvpFragment<ExchangeConfirmationView, ExchangeConfirmationPresenter>(),
    ExchangeConfirmationView {

    private val presenter: ExchangeConfirmationPresenter by inject()
    private val activityListener: HomebrewHostActivityListener by ParentActivityDelegate(this)

    private lateinit var sendButton: Button
    private lateinit var fromButton: Button
    private lateinit var toButton: Button
    private lateinit var valueTextView: TextView
    private lateinit var feesTextView: TextView
    private lateinit var receiveTextView: TextView
    private lateinit var sendToTextView: TextView

    private val exchangeConfirmationModel by unsafeLazy {
        arguments!!.getParcelable<ExchangeConfirmationModel>(ARGUMENT_CONFIRMATION_MODEL)
    }

    override val clickEvents by unsafeLazy { sendButton.throttledClicks() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.activity_homebrew_trade_confirmation)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        sendButton = view.findViewById(R.id.button_send_now)
        fromButton = view.findViewById(R.id.select_from_account_button)
        toButton = view.findViewById(R.id.select_to_account_button)
        valueTextView = view.findViewById(R.id.value_textView)
        feesTextView = view.findViewById(R.id.fees_textView)
        receiveTextView = view.findViewById(R.id.receive_textView)
        sendToTextView = view.findViewById(R.id.send_to_textView)

        activityListener.setToolbarTitle(R.string.confirm_exchange)

        renderUi()

        onViewReady()
    }

    private fun renderUi() {
        with(exchangeConfirmationModel) {
            toButton.setBackgroundResource(receivingCurrency.colorRes())
            toButton.text = receiving
            fromButton.setBackgroundResource(sendingCurrency.colorRes())
            fromButton.text = sending
            valueTextView.text = value
            feesTextView.text = fees
            receiveTextView.text = receiving
            sendToTextView.text = accountItem.toString()
        }
    }

    override fun continueToExchangeLocked() {
        val intent = Intent(requireContext(), ExchangeLockedActivity::class.java)
        startActivity(intent)
        requireActivity().finish()
    }

    override fun createPresenter(): ExchangeConfirmationPresenter = presenter

    override fun getMvpView(): ExchangeConfirmationView = this

    companion object {
        private const val ARGUMENT_CONFIRMATION_MODEL = "ARGUMENT_CONFIRMATION_MODEL"

        fun bundleArgs(exchangeConfirmationModel: ExchangeConfirmationModel): Bundle =
            Bundle().apply {
                putParcelable(ARGUMENT_CONFIRMATION_MODEL, exchangeConfirmationModel)
            }
    }
}

@Parcelize
data class ExchangeConfirmationModel(
    val value: String,
    val fees: String,
    val sending: String,
    val sendingCurrency: CryptoCurrency,
    val receiving: String,
    val receivingCurrency: CryptoCurrency,
    val accountItem: JsonSerializableAccount
) : Parcelable

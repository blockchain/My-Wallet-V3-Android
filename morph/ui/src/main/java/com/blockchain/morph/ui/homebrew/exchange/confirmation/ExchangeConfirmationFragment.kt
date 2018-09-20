package com.blockchain.morph.ui.homebrew.exchange.confirmation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.blockchain.balance.colorRes
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.ExchangeModel
import com.blockchain.morph.ui.homebrew.exchange.ExchangeViewModelProvider
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewHostActivityListener
import com.blockchain.ui.extensions.throttledClicks
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.formatWithUnit
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import timber.log.Timber
import java.util.Locale

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

    override val clickEvents by unsafeLazy { sendButton.throttledClicks() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.activity_homebrew_trade_confirmation)

    private lateinit var exchangeModel: ExchangeModel

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        val provider = (context as? ExchangeViewModelProvider)
            ?: throw Exception("Host activity must support ExchangeViewModelProvider")
        exchangeModel = provider.exchangeViewModel
        Timber.d("The view model is $exchangeModel")
    }

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

        onViewReady()
    }

    private var compositeDisposable = CompositeDisposable()

    override fun onResume() {
        super.onResume()
        compositeDisposable += exchangeModel
            .exchangeViewModels
            .observeOn(AndroidSchedulers.mainThread())
            .map {
                ExchangeConfirmationViewModel(
                    sending = it.from.cryptoValue,
                    receiving = it.to.cryptoValue,
                    value = it.to.fiatValue
                )
            }
            .subscribeBy {
                renderUi(it)
            }
    }

    private fun renderUi(viewModel: ExchangeConfirmationViewModel) {
        val locale = Locale.getDefault()
        with(viewModel) {
            fromButton.setBackgroundResource(sending.currency.colorRes())
            fromButton.text = sending.formatWithUnit(locale)
            toButton.setBackgroundResource(receiving.currency.colorRes())
            val receivingCryptoValue = receiving.formatWithUnit(locale)
            toButton.text = receivingCryptoValue
            receiveTextView.text = receivingCryptoValue
            valueTextView.text = value.toStringWithSymbol(locale)
            feesTextView.text = "TODO"
            sendToTextView.text = "TODO"
        }
    }

    override fun continueToExchangeLocked() {
        // TODO:
//        ExchangeLockedActivity.start(
//            requireContext(),
//            ExchangeLockedModel(
//                "ORDER ID",
//                exchangeConfirmationModel.value,
//                exchangeConfirmationModel.fees,
//                exchangeConfirmationModel.sending,
//                exchangeConfirmationModel.sendingCurrency,
//                exchangeConfirmationModel.receiving,
//                exchangeConfirmationModel.receivingCurrency,
//                exchangeConfirmationModel.accountItem
//            )
//        )
//        requireActivity().finish()
    }

    override fun createPresenter(): ExchangeConfirmationPresenter = presenter

    override fun getMvpView(): ExchangeConfirmationView = this
}

class ExchangeConfirmationViewModel(
    val value: FiatValue,
    val sending: CryptoValue,
    val receiving: CryptoValue
)

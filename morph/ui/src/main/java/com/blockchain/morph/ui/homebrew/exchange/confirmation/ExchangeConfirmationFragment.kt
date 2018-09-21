package com.blockchain.morph.ui.homebrew.exchange.confirmation

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import com.blockchain.balance.colorRes
import com.blockchain.koin.injectActivity
import com.blockchain.morph.exchange.mvi.ExchangeViewModel
import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.ExchangeModel
import com.blockchain.morph.ui.homebrew.exchange.ExchangeViewModelProvider
import com.blockchain.morph.ui.homebrew.exchange.host.HomebrewHostActivityListener
import com.blockchain.morph.ui.homebrew.exchange.locked.ExchangeLockedActivity
import com.blockchain.morph.ui.homebrew.exchange.locked.ExchangeLockedModel
import com.blockchain.ui.extensions.throttledClicks
import com.blockchain.ui.password.SecondPasswordHandler
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.formatWithUnit
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.ParentActivityDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.toast
import timber.log.Timber
import java.util.Locale

class ExchangeConfirmationFragment :
    BaseMvpFragment<ExchangeConfirmationView, ExchangeConfirmationPresenter>(),
    ExchangeConfirmationView {

    private val presenter: ExchangeConfirmationPresenter by inject()
    private val secondPasswordHandler: SecondPasswordHandler by injectActivity()
    private val activityListener: HomebrewHostActivityListener by ParentActivityDelegate(this)

    private lateinit var sendButton: Button
    private lateinit var fromButton: Button
    private lateinit var toButton: Button
    private lateinit var valueTextView: TextView
    private lateinit var feesTextView: TextView
    private lateinit var receiveTextView: TextView
    private lateinit var sendToTextView: TextView

    private var progressDialog: MaterialProgressDialog? = null

    override val clickEvents: Observable<ExchangeViewModel> by unsafeLazy {
        sendButton.throttledClicks()
            .flatMap { exchangeModel.exchangeViewModels }
    }

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
            .doOnNext { it.from }
            .filter { it.latestQuote?.rawQuote != null }
            .map {
                ExchangeConfirmationViewModel(
                    fromAccount = it.fromAccount,
                    toAccount = it.toAccount,
                    sending = it.from.cryptoValue,
                    receiving = it.to.cryptoValue,
                    value = it.to.fiatValue,
                    quote = it.latestQuote!!
                )
            }
            .doOnNext { presenter.updateFee(it.sending, it.fromAccount) }
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
            sendToTextView.text = viewModel.toAccount.label
        }
    }

    override fun continueToExchangeLocked(lockedModel: ExchangeLockedModel) {
        ExchangeLockedActivity.start(requireContext(), lockedModel)
        requireActivity().finish()
    }

    override fun updateFee(cryptoValue: CryptoValue) {
        feesTextView.text = cryptoValue.formatWithUnit()
    }

    override fun showSecondPasswordDialog() {
        secondPasswordHandler.validate(object : SecondPasswordHandler.ResultListener {
            override fun onNoSecondPassword() = Unit

            override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                presenter.onSecondPasswordValidated(validatedSecondPassword)
            }
        })
    }

    override fun showProgressDialog() {
        progressDialog = MaterialProgressDialog(activity).apply {
            setMessage(R.string.please_wait)
            setCancelable(false)
            show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply { dismiss() }
        progressDialog = null
    }

    override fun displayErrorDialog() {
        AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
            .setTitle(R.string.execution_error_title)
            .setMessage(R.string.execution_error_message)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun showToast(message: Int, type: String) {
        toast(message, type)
    }

    override fun createPresenter(): ExchangeConfirmationPresenter = presenter

    override fun getMvpView(): ExchangeConfirmationView = this
}

class ExchangeConfirmationViewModel(
    val fromAccount: AccountReference,
    val toAccount: AccountReference,
    val value: FiatValue,
    val sending: CryptoValue,
    val receiving: CryptoValue,
    val quote: Quote
)

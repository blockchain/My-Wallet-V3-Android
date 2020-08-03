package piuk.blockchain.android.ui.send

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Spinner
import androidx.annotation.CallSuper
import androidx.annotation.ColorRes
import androidx.annotation.Nullable
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import com.blockchain.koin.scopedInject
import com.blockchain.koin.scopedInjectActivity
import com.blockchain.notifications.analytics.SendAnalytics
import com.blockchain.serialization.JsonSerializableAccount
import com.blockchain.swap.nabu.extensions.fromIso8601ToUtc
import com.blockchain.transactions.Memo
import com.blockchain.ui.chooser.AccountChooserActivity
import com.blockchain.ui.chooser.AccountMode
import com.blockchain.ui.password.SecondPasswordHandler
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxbinding2.widget.textChanges
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoCurrency.Companion.MULTI_WALLET
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.*
import kotlinx.android.synthetic.main.fragment_send.*
import kotlinx.android.synthetic.main.include_amount_row.*
import kotlinx.android.synthetic.main.include_amount_row.view.*
import kotlinx.android.synthetic.main.include_from_row.view.*
import kotlinx.android.synthetic.main.include_to_row_editable.*
import kotlinx.android.synthetic.main.include_to_row_editable.view.*
import kotlinx.android.synthetic.main.view_expanding_currency_header.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.data.api.bitpay.models.events.BitPayEvent
import piuk.blockchain.android.data.connectivity.ConnectivityStatus
import piuk.blockchain.android.data.currency.CurrencyState
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.confirm.ConfirmPaymentDialog
import piuk.blockchain.android.ui.customviews.ErrorBottomDialog
import piuk.blockchain.android.ui.customviews.callbacks.OnTouchOutsideViewListener
import piuk.blockchain.android.ui.home.HomeScreenMvpFragment
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.AppRate
import piuk.blockchain.android.util.AppUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.errorIcon
import piuk.blockchain.androidcore.data.events.ActionEvent
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.ToolBarActivity
import piuk.blockchain.androidcoreui.ui.customviews.NumericKeyboardCallback
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.CameraPermissionListener
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import piuk.blockchain.androidcoreui.utils.helperfunctions.AfterTextChangedWatcher
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit

class SendFragment : HomeScreenMvpFragment<SendView, SendPresenter<SendView>>(),
    SendView,
    NumericKeyboardCallback {

    override val presenter: SendPresenter<SendView> by scopedInject()
    override val view: SendView = this

    private val appUtil: AppUtil by inject()
    private val secondPasswordHandler: SecondPasswordHandler by scopedInjectActivity()

    private val currencyState: CurrencyState by inject()
    private val stringUtils: StringUtils by inject()
    private val rxBus: RxBus by inject()

    private var confirmPaymentDialog: ConfirmPaymentDialog? = null
    private var transactionSuccessDialog: AlertDialog? = null
    private var bitpayInvoiceExpiredDialog: AlertDialog? = null
    private val timerDisposable = CompositeDisposable()
    private var handlingActivityResult = false
    private var pitEnabled = false
    private var pitAddressState: PitAddressFieldState = PitAddressFieldState.CLEARED

    private var isBitpayPayPro = false
    private var bitPayAddressScanned = false
    private var pitAddressAvailable = false
    private var validAddressFilled = false

    private val compositeDisposable = CompositeDisposable()

    private val dialogHandler = Handler()
    private val dialogRunnable = Runnable {
        transactionSuccessDialog?.apply {
            if (isShowing && !activity.isFinishing) {
                dismiss()
            }
        }
        bitpayInvoiceExpiredDialog?.apply {
            if (isShowing && !activity.isFinishing) {
                dismiss()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = container?.inflate(R.layout.fragment_send)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity.apply {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            with(activity as MainActivity) {
                setOnTouchOutsideViewListener(currency_header,
                    object : OnTouchOutsideViewListener {
                        override fun onTouchOutside(view: View, event: MotionEvent) {
                            currency_header.close()
                        }
                    })
            }
        }

        setCustomKeypad()
        setupCurrencyHeader()

        setupSendingView()
        setupTransferReceiveView()
        setupCryptoTextField()
        setupFiatTextField()
        setupFeesView()
        setUpMemoEdittext()
        setUpSpinner()

        buttonContinue.setOnClickListener {
            if (ConnectivityStatus.hasConnectivity(activity)) {
                presenter.onContinueClicked()
            } else {
                showSnackbar(R.string.check_connectivity_exit, Snackbar.LENGTH_LONG)
            }
            analytics.logEvent(SendAnalytics.SendFormClicked(currencyState.cryptoCurrency))
        }

        max.setOnClickListener {
            analytics.logEvent(SendAnalytics.SendSpendableBalanceClicked(currencyState.cryptoCurrency))
            presenter.onSpendMaxClicked()
        }

        info_link.setOnClickListener {
            fragmentManager?.let {
                MinBalanceExplanationDialog()
                    .show(it, "Dialog")
            }
        }

        amountContainer.currencyFiat.text = currencyState.fiatUnit
    }

    @CallSuper
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        handleIncomingArguments()
        onViewReady()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    private val event by unsafeLazy {
        rxBus.register(ActionEvent::class.java)
    }

    override fun onResume() {
        super.onResume()

        setSelectedCurrency(currencyState.cryptoCurrency)

        if (!handlingActivityResult)
            presenter.onResume()

        handlingActivityResult = false

        setupToolbar()
        closeKeypad()

        compositeDisposable += event.subscribe {
            presenter.onBroadcastReceived()
        }
    }

    override fun onPause() {
        super.onPause()

        rxBus.unregister(ActionEvent::class.java, event)
        compositeDisposable.clear()

        currency_header?.close()
    }

    override fun hideCurrencyHeader() {
        textview_selected_currency?.apply {
            isClickable = false
        }
    }

    override fun updateRequiredLabelVisibility(isVisible: Boolean) {
        if (isVisible) {
            required_label?.visible()
        } else {
            required_label?.gone()
        }
    }

    private fun setCustomKeypad() {
        keyboard.setCallback(this)
        keyboard.setDecimalSeparator(presenter.getDefaultDecimalSeparator())

        // Enable custom keypad and disables default keyboard from popping up
        keyboard.enableOnView(amountCrypto)
        keyboard.enableOnView(amountFiat)

        amountCrypto.setText("")
        amountCrypto.requestFocus()

        toContainer.toAddressEditTextView.setOnFocusChangeListener { _, focused ->
            if (focused) closeKeypad()
        }
    }

    private fun closeKeypad() {
        keyboard.setNumpadVisibility(View.GONE)
    }

    private val memoEditText: EditText?
        get() = memo_text_edit

    private fun setUpSpinner() {
        memo_type_spinner.apply {
            setupOptions(0)
            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(parent: AdapterView<*>, spinner: View, pos: Int, id: Long) {
                    val newMemoType = if (pos == 0) SendPresenter.MEMO_TEXT_TYPE else SendPresenter.MEMO_ID_TYPE
                    presenter.onMemoTypeChanged(newMemoType)
                    if (pos == 0)
                        memoEditText?.inputType = InputType.TYPE_CLASS_TEXT
                    else
                        memoEditText?.inputType = InputType.TYPE_CLASS_NUMBER
                    post {
                        if (memoEditText?.hasFocus() == true) {
                            memoEditText?.requestFocus()
                            memoEditText?.setText("")
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                }
            }
            setSelection(0)
        }
    }

    private fun Spinner.setupOptions(selectedIndex: Int) {
        ArrayAdapter.createFromResource(
            requireContext(),
            arrayToDisplay(selectedIndex),
            piuk.blockchain.androidcoreui.R.layout.dialog_edit_memo_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            this.adapter = adapter
        }
    }

    private fun arrayToDisplay(selectedIndex: Int): Int {
        val manualArraySize =
            resources.getStringArray(R.array.xlm_memo_types_manual) ?: return 0
        return if (selectedIndex < manualArraySize.size) {
            R.array.xlm_memo_types_manual
        } else {
            R.array.xlm_memo_types_all
        }
    }

    private fun isKeyboardVisible(): Boolean = keyboard.isVisible

    override fun onKeypadClose() {
        // Show bottom nav if applicable
        navigator().showNavigation()

        // Resize activity to default
        scrollView.apply {
            setPadding(0, 0, 0, 0)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onKeypadOpen() {
        currency_header?.close()
        navigator().hideNavigation()
    }

    override fun onKeypadOpenCompleted() {
        // Resize activity around view
        val translationY = keyboard.height
        scrollView.apply {
            setPadding(0, 0, 0, translationY)
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun setupCurrencyHeader() {
        ViewUtils.hideKeyboard(activity)
        closeKeypad()
        currency_header.setSelectionListener { currency ->
            currencyState.cryptoCurrency = currency
            // This will restart a new instance of this activity for the selected currency.
            navigator().gotoSendFor(currency)
        }
    }

    private fun setupToolbar() {
        val supportActionBar = (activity as AppCompatActivity).supportActionBar
        if (supportActionBar != null) {

            (activity as ToolBarActivity).setupToolbar(
                supportActionBar, R.string.send
            )
        } else {
            finishPage()
        }
    }

    override fun finishPage() {
        navigator().gotoDashboard()
    }

    private fun startScanActivity() {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(activity, CaptureActivity::class.java)
            startActivityForResult(intent, SCAN_PRIVX)
        } else {
            showSnackbar(R.string.camera_unavailable, Snackbar.LENGTH_LONG)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handlingActivityResult = true
        if (resultCode != AppCompatActivity.RESULT_OK) return
        resetPitAddressState()
        when (requestCode) {
            SCAN_PRIVX -> presenter.handlePrivxScan(data?.getStringExtra(CaptureActivity.SCAN_RESULT))
            REQUEST_CODE_BTC_SENDING -> presenter.selectSendingAccount(unpackAccountResult(data))
            REQUEST_CODE_BTC_RECEIVING -> presenter.selectReceivingAccount(unpackAccountResult(data))
            REQUEST_CODE_BCH_SENDING -> presenter.selectSendingAccount(unpackAccountResult(data))
            REQUEST_CODE_BCH_RECEIVING -> presenter.selectReceivingAccount(unpackAccountResult(data))
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun resetPitAddressState() {
        if (pitAddressState == PitAddressFieldState.CLEARED)
            return
        pitAddressState = PitAddressFieldState.CLEARED
        pitAddress.setImageResource(R.drawable.ic_the_exchange_colour)
        presenter.onPitAddressCleared()
        toContainer.toAddressEditTextView.isEnabled = true
    }

    private fun unpackAccountResult(intent: Intent?): JsonSerializableAccount? =
        AccountChooserActivity.unpackAccountResult(intent)

    @SuppressLint("CheckResult")
    private fun setupTransferReceiveView() {
        // Avoid OntouchListener - causes paste issues on some Samsung devices
        toContainer.toAddressEditTextView.setOnClickListener {
            if (currencyState.cryptoCurrency != CryptoCurrency.XLM) {
                toContainer.toAddressEditTextView.setText("")
                presenter.clearReceivingObject()
            }
        }
        // LongClick listener required to clear receive address in memory when user long clicks to paste
        toContainer.toAddressEditTextView.setOnLongClickListener { v ->
            if (currencyState.cryptoCurrency != CryptoCurrency.XLM) {
                toContainer.toAddressEditTextView.setText("")
                presenter.clearReceivingObject()
                v.performClick()
            }
            false
        }

        // TextChanged listener required to invalidate receive address in memory when user
        // chooses to edit address populated via QR
        toContainer.toAddressEditTextView.textChanges()
            .doOnNext {
                if (requireActivity().currentFocus === toContainer.toAddressEditTextView) {
                    presenter.clearReceivingObject()
                }
            }
            .map { it.toString() }
            .subscribe(
                { presenter.onAddressTextChange(it) },
                { Timber.e(it) })

        toContainer.toArrow.setOnClickListener { startToAccountChooser() }
    }

    private fun disableCryptoTextChangeListener() {
        amountContainer.amountCrypto.removeTextChangedListener(cryptoTextWatcher)
    }

    @SuppressLint("NewApi")
    private fun enableCryptoTextChangeListener() {
        amountContainer.amountCrypto.addTextChangedListener(cryptoTextWatcher)
        try {
            // This method is hidden but accessible on <API21, but here we catch exceptions just in case
            amountContainer.amountCrypto.showSoftInputOnFocus = false
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun updateCryptoAmount(cryptoValue: CryptoValue, silent: Boolean) {
        if (silent) disableCryptoTextChangeListener()
        amountContainer.amountCrypto.setText(cryptoValue.toStringWithoutSymbol())
        if (silent) enableCryptoTextChangeListener()
    }

    private fun disableFiatTextChangeListener() {
        amountContainer.amountFiat.removeTextChangedListener(fiatTextWatcher)
    }

    @SuppressLint("NewApi")
    private fun enableFiatTextChangeListener() {
        amountContainer.amountFiat.addTextChangedListener(fiatTextWatcher)
        try {
            // This method is hidden but accessible on <API21, but here we catch exceptions just in case
            amountContainer.amountFiat.showSoftInputOnFocus = false
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun updateFiatAmount(fiatValue: FiatValue, silent: Boolean) {
        if (silent) disableFiatTextChangeListener()
        amountContainer.amountFiat.setText(fiatValue.toStringWithoutSymbol())
        if (silent) enableFiatTextChangeListener()
    }

    // BTC Field
    @SuppressLint("NewApi")
    private fun setupCryptoTextField() {
        amountContainer.amountCrypto.hint = "0" + presenter.getDefaultDecimalSeparator() + "00"
        amountContainer.amountCrypto.setSelectAllOnFocus(true)
        enableCryptoTextChangeListener()
    }

    // Fiat Field
    @SuppressLint("NewApi")
    private fun setupFiatTextField() {
        amountContainer.amountFiat.hint = "0" + presenter.getDefaultDecimalSeparator() + "00"
        amountContainer.amountFiat.setSelectAllOnFocus(true)
        enableFiatTextChangeListener()
    }

    private val cryptoTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(editable: Editable) {
            presenter.updateFiatTextField(editable, amountContainer.amountCrypto)
            updateTotals()
        }
    }

    private val fiatTextWatcher = object : AfterTextChangedWatcher() {
        override fun afterTextChanged(editable: Editable) {
            presenter.updateCryptoTextField(editable, amountContainer.amountFiat)
            updateTotals()
        }
    }

    private fun handleIncomingArguments() {
        if (!handlePredefinedInput(arguments.inputDeeplinked)) {
            presenter.onCurrencySelected(currencyState.cryptoCurrency)
        }
    }

    private fun handlePredefinedInput(isDeeplinked: Boolean): Boolean {
        if (arguments != null) {
            val input = arguments.input
            if (input != null) {
                handlingActivityResult = true
                presenter.handlePredefinedInput(input, currencyState.cryptoCurrency, isDeeplinked)
                return true
            }
        }
        return false
    }

    private fun setupSendingView() {
        fromContainer.fromAddressTextView.setOnClickListener { startFromAccountChooser() }
        fromContainer.fromArrowImage.setOnClickListener { startFromAccountChooser() }
    }

    override fun updateSendingAddress(label: String) {
        fromContainer.fromAddressTextView.text = label
    }

    private val pitIconCanBeDisplayed: Boolean
        get() = pitEnabled && !validAddressFilled && !bitPayAddressScanned

    override fun updateReceivingHintAndAccountDropDowns(
        currency: CryptoCurrency,
        listSize: Int,
        pitAddressAvailable: Boolean,
        onPitClicked: () -> Unit
    ) {
        if (listSize == 1) {
            hideReceivingDropdown()
            hideSendingFieldDropdown()
        } else {
            showSendingFieldDropdown()
            showReceivingDropdown()
        }

        if (pitAddressAvailable && pitIconCanBeDisplayed) {
            showPitAddressIconWithClickListener(onPitClicked)
        } else {
            hidePitAddressIcon()
        }

        this.pitAddressAvailable = pitAddressAvailable

        val hint: Int = if (listSize > 1) {
            when (currencyState.cryptoCurrency) {
                CryptoCurrency.BTC -> R.string.to_field_helper
                CryptoCurrency.ETHER -> R.string.eth_to_field_helper
                CryptoCurrency.BCH -> R.string.bch_to_field_helper
                CryptoCurrency.XLM -> R.string.xlm_to_field_helper
                CryptoCurrency.PAX -> R.string.pax_to_field_helper_1
                CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
                CryptoCurrency.ALGO -> TODO("STUB: ALGO NOT IMPLEMENTED")
                CryptoCurrency.USDT -> R.string.usdt_to_field_helper_1
            }
        } else {
            when (currencyState.cryptoCurrency) {
                CryptoCurrency.BTC -> R.string.to_field_helper_no_dropdown
                CryptoCurrency.ETHER -> R.string.eth_to_field_helper_no_dropdown
                CryptoCurrency.BCH -> R.string.bch_to_field_helper_no_dropdown
                CryptoCurrency.XLM -> R.string.xlm_to_field_helper_no_dropdown
                CryptoCurrency.PAX -> R.string.pax_to_field_helper_no_dropdown_1
                CryptoCurrency.STX -> TODO("STUB: STX NOT IMPLEMENTED")
                CryptoCurrency.ALGO -> TODO("STUB: ALGO NOT IMPLEMENTED")
                CryptoCurrency.USDT -> R.string.usdt_to_field_helper_no_dropdown_1
            }
        }
        toContainer.toAddressEditTextView.setHint(hint)
    }

    private fun showPitAddressIconWithClickListener(onPitClicked: () -> Unit) {
        toContainer.pitAddress.apply {
            visible()
            setOnClickListener {
                onPitClicked()
                analytics.logEvent(SendAnalytics.PitButtonClicked(currencyState.cryptoCurrency))
            }
        }
    }

    private fun startFromAccountChooser() {
        val currency = currencyState.cryptoCurrency

        if (currency.hasFeature(MULTI_WALLET)) {
            AccountChooserActivity.startForResult(
                this,
                AccountMode.CryptoAccountMode(cryptoCurrency = currency, isSend = true),
                if (currency == CryptoCurrency.BTC) {
                    REQUEST_CODE_BTC_SENDING
                } else {
                    REQUEST_CODE_BCH_SENDING
                },
                getString(R.string.from)
            )
        }
    }

    private fun startToAccountChooser() {
        val currency = currencyState.cryptoCurrency

        if (currency.hasFeature(MULTI_WALLET)) {
            AccountChooserActivity.startForResult(
                this,
                AccountMode.CryptoAccountMode(cryptoCurrency = currency, isSend = false),
                if (currency == CryptoCurrency.BTC) {
                    REQUEST_CODE_BTC_RECEIVING
                } else {
                    REQUEST_CODE_BCH_RECEIVING
                },
                getString(R.string.to)
            )
        }
    }

    fun onChangeFeeClicked() {
        confirmPaymentDialog?.dismiss()
    }

    fun onContinueClicked() {
        if (ConnectivityStatus.hasConnectivity(activity)) {
            presenter.onContinueClicked()
        } else {
            showSnackbar(R.string.check_connectivity_exit, Snackbar.LENGTH_LONG)
        }
    }

    fun onSendClicked() {
        presenter.submitPayment()
    }

    override fun getReceivingAddress() = toContainer.toAddressEditTextView.getTextString()

    override fun onBackPressed(): Boolean =
        when {
            isKeyboardVisible() -> {
                closeKeypad()
                true
            }
            currency_header.isOpen() -> {
                currency_header.close()
                true
            }
            else -> false
        }

    override fun setSelectedCurrency(cryptoCurrency: CryptoCurrency) {
        currency_header.setCurrentlySelectedCurrency(cryptoCurrency)
        amountContainer.currencyCrypto.text = cryptoCurrency.displayTicker
    }

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        toast(getString(message), toastType)
    }

    override fun showSnackbar(message: Int, duration: Int) {
        showSnackbar(getString(message), null, duration)
    }

    override fun showSnackbar(message: String, @Nullable extraInfo: String?, duration: Int) {
        activity.run {
            val snackbar = Snackbar.make(coordinator_layout, message, duration)
                .setActionTextColor(ContextCompat.getColor(this, R.color.primary_blue_accent))

            if (extraInfo != null) {
                snackbar.setAction(R.string.more) {
                    showSnackbar(
                        extraInfo,
                        null,
                        Snackbar.LENGTH_INDEFINITE
                    )
                }
            } else {
                if (duration == Snackbar.LENGTH_INDEFINITE) {
                    snackbar.setAction(R.string.ok_cap, null)
                }
            }

            snackbar.show()
        }
    }

    override fun showEthContractSnackbar() {
        activity.run {
            Snackbar.make(coordinator_layout,
                R.string.eth_support_contract_not_allowed,
                Snackbar.LENGTH_INDEFINITE
            ).setActionTextColor(ContextCompat.getColor(this, R.color.primary_blue_accent))
                .setAction(R.string.learn_more) {
                    showSnackbar(
                        R.string.eth_support_only_eth,
                        Snackbar.LENGTH_INDEFINITE
                    )
                }
                .show()
        }
    }

    private fun showSendingFieldDropdown() {
        fromContainer.fromArrowImage.visible()
        fromContainer.fromArrowImage.isClickable = true
        fromContainer.fromAddressTextView.isClickable = true
    }

    private fun hideSendingFieldDropdown() {
        fromContainer.fromArrowImage.gone()
        fromContainer.fromArrowImage.isClickable = false
        fromContainer.fromAddressTextView.isClickable = false
    }

    private fun showReceivingDropdown() {
        toContainer.toArrow.visible()
        toContainer.toArrow.isClickable = true
        toContainer.toAddressEditTextView.isClickable = true
    }

    private fun hideReceivingDropdown() {
        toContainer.toArrow.gone()
        toContainer.toArrow.isClickable = false
        toContainer.toAddressEditTextView.isClickable = false
    }

    private fun hidePitAddressIcon() {
        toContainer.pitAddress.gone()
    }

    override fun updateReceivingAddress(address: String) {
        toContainer.toAddressEditTextView.setText(address)
    }

    private fun setupFeesView() {
        val adapter = FeePriorityAdapter(activity, presenter.getFeeOptionsForDropDown())

        spinnerPriority.adapter = adapter

        spinnerPriority.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (position) {
                    0, 1 -> {
                        if (currencyState.cryptoCurrency != CryptoCurrency.XLM) {
                            buttonContinue.isEnabled = true
                        }
                        textviewFeeAbsolute.visible()
                        textInputLayout.gone()
                        updateTotals()
                    }
                    2 -> if (presenter.shouldShowAdvancedFeeWarning()) {
                        alertCustomSpend()
                    } else {
                        displayCustomFeeField()
                    }
                }

                val options = presenter.getFeeOptionsForDropDown()[position]
                textviewFeeType.text = options.title
                textviewFeeTime.text = if (position != 2) options.description else null
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // No-op
            }
        }

        textviewFeeAbsolute.setOnClickListener { spinnerPriority.performClick() }
        textviewFeeType.setText(R.string.fee_options_regular)
        textviewFeeTime.setText(R.string.fee_options_regular_time)
    }

    private fun setUpMemoEdittext() {
        memo_text_edit.addTextChangedListener(object : AfterTextChangedWatcher() {
            override fun afterTextChanged(p0: Editable?) {
                presenter.onMemoChange(p0?.toString() ?: return)
            }
        })
    }

    override fun enableFeeDropdown() {
        spinnerPriority.isEnabled = true
        textviewFeeAbsolute.isEnabled = true
    }

    override fun disableFeeDropdown() {
        spinnerPriority.isEnabled = false
        textviewFeeAbsolute.isEnabled = false
    }

    override fun setSendButtonEnabled(enabled: Boolean) {
        buttonContinue.isEnabled = enabled
    }

    internal fun updateTotals() {
        presenter.onCryptoTextChange(amountContainer.amountCrypto.text.toString())
    }

    @FeeType.FeePriorityDef
    override fun getFeePriority(): Int {
        return when (spinnerPriority.selectedItemPosition) {
            1 -> FeeType.FEE_OPTION_PRIORITY
            2 -> FeeType.FEE_OPTION_CUSTOM
            else -> FeeType.FEE_OPTION_REGULAR
        }
    }

    override fun getCustomFeeValue(): Long {
        val amount = edittextCustomFee.text.toString()
        return amount.toLongOrNull() ?: 0
    }

    override fun showMaxAvailable() {
        max.visible()
        progressBarMaxAvailable.invisible()
        max.goneIf(isBitpayPayPro)
    }

    override fun hideMaxAvailable() {
        max.invisible()
        progressBarMaxAvailable.visible()
    }

    private var lastMemo: Memo = Memo.None
        set(value) {
            field = value
            memo.text = value.toText(resources)
        }

    override fun showMemo() {
        memo_container.visible()
    }

    override fun hideMemo() {
        memo_container.gone()
    }

    override fun displayMemo(usersMemo: Memo) {
        lastMemo = usersMemo
        if (memo_text_edit.text.toString() != usersMemo.value) {
            memo_text_edit.setText(usersMemo.value)
        }
        usersMemo.type?.let {
            val position = if (it == "text") 0 else 1
            memo_type_spinner.setSelection(position)
        }
    }

    override fun enableMemo(enabled: Boolean) {
        memo_text_edit.isEnabled = enabled
    }

    override fun updateWarning(message: String) {
        arbitraryWarning?.apply {
            visible()
            text = message
        }
        analytics.logEvent(SendAnalytics.SendFormErrorAppears(currencyState.cryptoCurrency))
    }

    override fun clearWarning() {
        arbitraryWarning?.apply {
            gone()
            text = ""
        }
    }

    override fun showInfoLink() {
        info_link.visible()
    }

    override fun hideInfoLink() {
        info_link.gone()
    }

    @SuppressLint("SetTextI18n")
    override fun updateFeeAmount(feeCrypto: CryptoValue, feeFiat: FiatValue) {
        textviewFeeAbsolute.text = "${feeCrypto.toStringWithSymbol()} (${feeFiat.toStringWithSymbol()})"
    }

    override fun clearFeeAmount() {
        textviewFeeAbsolute.text = ""
    }

    override fun clearAmount() {
        amountCrypto.text = null
        amountFiat.text = null
    }

    override fun updateMaxAvailable(maxAmount: String) {
        max.text = maxAmount
    }

    override fun updateMaxAvailable(maxAmount: CryptoValue, min: CryptoValue) {
        if (maxAmount <= min) {
            updateMaxAvailable(getString(R.string.insufficient_funds))
            updateMaxAvailableColor(R.color.product_red_medium)
        } else {
            updateMaxAvailable("${getString(R.string.max_available)} ${maxAmount.toStringWithSymbol()}")
            updateMaxAvailableColor(R.color.primary_blue_accent)
        }
        showMaxAvailable()
    }

    override fun updateMaxAvailableColor(@ColorRes color: Int) {
        max.setTextColor(ContextCompat.getColor(context!!, color))
    }

    override fun setCryptoMaxLength(length: Int) {
        val filterArray = arrayOfNulls<InputFilter>(1)
        filterArray[0] = InputFilter.LengthFilter(length)
        amountContainer.amountCrypto.filters = filterArray
    }

    override fun showFeePriority() {
        textviewFeeType.visible()
        textviewFeeTime.visible()
        spinnerPriority.visible()
    }

    override fun hideFeePriority() {
        textviewFeeType.gone()
        textviewFeeTime.gone()
        spinnerPriority.invisible()
    }

    override fun showBIP38PassphrasePrompt(scanData: String) {
        val password = AppCompatEditText(activity)
        password.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        password.setHint(R.string.password)

        activity.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(ViewUtils.getAlertDialogPaddedView(context, password))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    presenter.spendFromWatchOnlyBIP38(
                        password.text.toString(),
                        scanData
                    )
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        }
    }

    override fun showWatchOnlyWarning(address: String) {
        activity.run {
            val dialogView = layoutInflater.inflate(R.layout.alert_watch_only_spend, null)
            val alertDialog = AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setView(dialogView.rootView)
                .setCancelable(false)
                .create()

            dialogView.confirm_cancel.setOnClickListener {
                toContainer.toAddressEditTextView.setText("")
                presenter.setWarnWatchOnlySpend(!dialogView.confirm_dont_ask_again.isChecked)
                alertDialog.dismiss()
            }

            dialogView.confirm_continue.setOnClickListener {
                presenter.setWarnWatchOnlySpend(!dialogView.confirm_dont_ask_again.isChecked)
                alertDialog.dismiss()
            }

            alertDialog.show()
        }
    }

    override fun getClipboardContents(): String? {
        val clipMan = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipMan.primaryClip
        return if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).coerceToText(activity).toString()
        } else null
    }

    private fun playAudio() {
        activity.run {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                val mp: MediaPlayer = MediaPlayer.create(applicationContext, R.raw.beep)
                mp.setOnCompletionListener {
                    it.reset()
                    it.release()
                }
                mp.start()
            }
        }
    }

    override fun showSpendFromWatchOnlyWarning(address: String) {
        showAlert(
            AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
                .setTitle(R.string.privx_required)
                .setMessage(
                    getString(
                        R.string.watch_only_spend_instructions,
                        address
                    )
                )
                .setCancelable(false)
                .setPositiveButton(R.string.dialog_continue) { _, _ -> requestScanPermissions() }
                .setNegativeButton(android.R.string.cancel, null)
                .create()
        )
    }

    private fun requestScanPermissions() {
        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
            .with(coordinator_layout, R.string.request_camera_permission)
            .withButton(android.R.string.ok) { requestScanPermissions() }
            .build()

        val grantedPermissionListener = CameraPermissionListener(analytics, {
            startScanActivity()
        })

        val compositePermissionListener =
            CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)

        Dexter.withActivity(requireActivity())
            .withPermission(Manifest.permission.CAMERA)
            .withListener(compositePermissionListener)
            .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
            .check()
    }

    override fun showSecondPasswordDialog() {
        secondPasswordHandler.validate(object : SecondPasswordHandler.ResultListener {
            override fun onNoSecondPassword() {
                presenter.onNoSecondPassword()
            }

            override fun onSecondPasswordValidated(validatedSecondPassword: String) {
                presenter.onSecondPasswordValidated(validatedSecondPassword)
            }
        })
    }

    override fun showPaymentDetails(confirmationDetails: SendConfirmationDetails) {
        showPaymentDetails(
            confirmationDetails = confirmationDetails.toPaymentConfirmationDetails(),
            note = confirmationDetails.sendDetails.memo.valueTextOrNull(),
            noteDescription = confirmationDetails.sendDetails.memo.describeType(resources),
            allowFeeChange = false
        )
    }

    override fun showPaymentDetails(
        confirmationDetails: PaymentConfirmationDetails,
        note: String?,
        noteDescription: String?,
        allowFeeChange: Boolean
    ) {
        fragmentManager?.let { fragmentManager ->
            confirmPaymentDialog =
                ConfirmPaymentDialog.newInstance(confirmationDetails, note, noteDescription, allowFeeChange)
                    .also {
                        it.show(fragmentManager, ConfirmPaymentDialog::class.java.simpleName)
                    }
        }
    }

    override fun showLargeTransactionWarning() {
        coordinator_layout.postDelayed({
            activity.run {
                AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setCancelable(false)
                    .setTitle(R.string.warning)
                    .setMessage(R.string.large_tx_warning)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }, 500L)
    }

    override fun dismissConfirmationDialog() {
        confirmPaymentDialog?.dismiss()
    }

    internal fun alertCustomSpend() {
        activity.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.transaction_fee)
                .setMessage(R.string.fee_options_advanced_warning)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    presenter.disableAdvancedFeeWarning()
                    displayCustomFeeField()
                }
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                    spinnerPriority.setSelection(0)
                }
                .show()
        }
    }

    override fun setFeePrioritySelection(index: Int) {
        spinnerPriority.setSelection(index)
    }

    fun setExpiryColorFiveMinute() {
        bitpayTimeRemaining.setTextColor(getResolvedColor(R.color.secondary_yellow_medium))
    }

    fun setExpiryColorOneMinute() {
        bitpayTimeRemaining.setTextColor(getResolvedColor(R.color.secondary_red_light))
    }

    override fun showBitPayTimerAndMerchantInfo(expiry: String, merchantName: String) {
        bitpayMerchantText.visible()
        bitpayTimeRemaining.visible()
        bitpayDivider.visible()
        bitpayMerchantText.text = merchantName
        isBitpayPayPro = true
        val expiryDateGmt = expiry.fromIso8601ToUtc()
        if (expiryDateGmt != null) {
            startBitPayTimer(expiryDateGmt)
        }
    }

    override fun resetBitpayState() {
        bitpayMerchantText.gone()
        bitpayTimeRemaining.gone()
        bitpayDivider.gone()
        timerDisposable.clear()
    }

    private fun startCountdown(endTime: Long) {
        val timeRemainingText = stringUtils.getString(R.string.bitpay_remaining_time)
        var remaining = (endTime - System.currentTimeMillis()) / 1000
        var spaceChar = " "
        if (remaining <= 2) {
            // Finish page with error
            showbitpayInvoiceExpiredDialog()
        } else {
            timerDisposable.clear()
            timerDisposable += Observable.interval(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .doOnEach { remaining-- }
                .map { remaining }
                .doOnNext {
                    if (spaceChar != "" && it < 10 * 60) {
                        spaceChar = ""
                    }
                    val readableTime = String.format(
                        "%2d:%02d",
                        TimeUnit.SECONDS.toMinutes(it),
                        TimeUnit.SECONDS.toSeconds(it) -
                                TimeUnit.MINUTES.toSeconds(TimeUnit.SECONDS.toMinutes(it))
                    )
                    val timerText = timeRemainingText + spaceChar + readableTime
                    bitpayTimeRemaining.text = timerText
                    bitpayTimeRemaining.setTextColor(getResolvedColor(R.color.primary_grey_light))
                }
                .doOnNext { if (it < 5 * 60) setExpiryColorFiveMinute() }
                .doOnNext { if (it < 60) setExpiryColorOneMinute() }
                .takeUntil { it <= 2 }
                .doOnComplete { showbitpayInvoiceExpiredDialog() }
                .subscribe()
        }
    }

    override fun onStop() {
        super.onStop()
        timerDisposable.clear()
    }

    private fun startBitPayTimer(expiryDateGmt: Date) {
        val calendar = Calendar.getInstance()
        val timeZone = calendar.timeZone
        val offset = timeZone.getOffset(expiryDateGmt.time)

        startCountdown(expiryDateGmt.time + offset)
    }

    private fun showbitpayInvoiceExpiredDialog() {

        val appRate = AppRate(activity)
            .setMinTransactionsUntilPrompt(3)
            .incrementTransactionCount()
        analytics.logEvent(BitPayEvent.ExpiredEvent)
        activity.run {
            val dialogBuilder = AlertDialog.Builder(this)
            val dialogView = View.inflate(activity, R.layout.modal_transaction_failed, null)
            bitpayInvoiceExpiredDialog = dialogBuilder.setView(dialogView)
                .setMessage(R.string.bitpay_invoice_expired_message)
                .setTitle(R.string.bitpay_invoice_expired)
                .setPositiveButton(getString(R.string.btn_ok), null)
                .create()

            bitpayInvoiceExpiredDialog?.apply {
                setOnDismissListener {
                    confirmPaymentDialog?.dismiss()
                    finishPage()
                }
                if (!isFinishing) show()
            }
        }

        dialogHandler.postDelayed(dialogRunnable, (10 * 1000).toLong())
    }

    @SuppressLint("CheckResult")
    internal fun displayCustomFeeField() {
        textviewFeeAbsolute.gone()
        textviewFeeTime.invisible()
        textInputLayout.visible()
        buttonContinue.isEnabled = false
        textInputLayout.editText?.hint = getString(R.string.fee_options_sat_byte_hint)

        edittextCustomFee.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus || !edittextCustomFee.text.toString().isEmpty()) {
                textInputLayout.editText?.hint = getString(
                    R.string.fee_options_sat_byte_inline_hint,
                    presenter.getBitcoinFeeOptions()?.regularFee.toString(),
                    presenter.getBitcoinFeeOptions()?.priorityFee.toString()
                )
            } else if (edittextCustomFee.text.toString().isEmpty()) {
                textInputLayout.editText?.hint = getString(R.string.fee_options_sat_byte_hint)
            } else {
                textInputLayout.editText?.hint = getString(R.string.fee_options_sat_byte_hint)
            }
        }

        edittextCustomFee.textChanges()
            .skip(1)
            .map { it.toString() }
            .doOnNext { buttonContinue.isEnabled = it.isNotEmpty() && it != "0" }
            .filter { !it.isEmpty() }
            .map { it.toLong() }
            .onErrorReturnItem(0L)

            .doOnNext { value ->
                val feeOptions = presenter.getBitcoinFeeOptions()
                if (feeOptions != null && value < feeOptions.limits!!.min) {
                    textInputLayout.error = getString(R.string.fee_options_fee_too_low)
                } else if (feeOptions != null && value > feeOptions.limits!!.max) {
                    textInputLayout.error = getString(R.string.fee_options_fee_too_high)
                } else {
                    textInputLayout.error = null
                }
            }
            .debounce(300, TimeUnit.MILLISECONDS)
            .subscribeOn(AndroidSchedulers.mainThread())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                { updateTotals() },
                { Timber.e(it) })
    }

    override fun showTransactionSuccess(cryptoCurrency: CryptoCurrency) {
        playAudio()

        val appRate = AppRate(activity)
            .setMinTransactionsUntilPrompt(3)
            .incrementTransactionCount()

        activity.run {
            val dialogBuilder = AlertDialog.Builder(this)
            val dialogView = View.inflate(activity, R.layout.modal_transaction_success, null)
            transactionSuccessDialog = dialogBuilder.setView(dialogView)
                .setTitle(R.string.transaction_submitted)
                .setPositiveButton(getString(R.string.done), null)
                .create()

            transactionSuccessDialog?.apply {
                // If should show app rate, success dialog shows first and launches
                // rate dialog on dismiss. Dismissing rate dialog then closes the page. This will
                // happen if the user chooses to rate the app - they'll return to the main page.
                // Won't show if contact transaction, as other dialog takes preference
                if (appRate.shouldShowDialog()) {
                    val ratingDialog = appRate.rateDialog
                    ratingDialog.setOnDismissListener { finishPage() }
                    setOnDismissListener { ratingDialog.show() }
                } else {
                    setOnDismissListener { finishPage() }
                }

                if (cryptoCurrency == CryptoCurrency.ETHER) {
                    setMessage(getString(R.string.eth_transaction_complete))
                }

                if (!isFinishing) show()
            }
        }

        dialogHandler.postDelayed(dialogRunnable, (10 * 1000).toLong())
    }

    override fun showInsufficientGasDlg(cryptoCurrency: CryptoCurrency) {

        fragmentManager?.let {
            ErrorBottomDialog.newInstance(
                ErrorBottomDialog.Content(
                    title = getString(R.string.pax_need_more_eth_error_title),
                    description = getString(R.string.erc20_need_more_eth_error_body_1, cryptoCurrency.displayTicker),
                    icon = CryptoCurrency.ETHER.errorIcon(),
                    dismissText = R.string.btn_ok
                )
            ).show(it, "BottomDialog")
        }
    }

    override fun enableInput() {
        toAddressEditTextView.isEnabled = true
        toArrow.isEnabled = true
        amountCrypto.isEnabled = true
        amountFiat.isEnabled = true
        max.isEnabled = true
        max.visible()
        edittextCustomFee.isEnabled = true
    }

    override fun disableInput() {
        toAddressEditTextView.isEnabled = false
        toArrow.isEnabled = false
        amountCrypto.isEnabled = false
        amountFiat.isEnabled = false
        max.isEnabled = false
        edittextCustomFee.isEnabled = false
    }

    companion object {
        const val SCAN_PRIVX = 2011
        private const val ARGUMENT_SEND_INPUT = "send_input"
        private const val ARGUMENT_SEND_INPUT_DEEPLINKED = "send_input_deeplinked"

        private const val REQUEST_CODE_BTC_RECEIVING = 911
        private const val REQUEST_CODE_BTC_SENDING = 912
        private const val REQUEST_CODE_BCH_RECEIVING = 913
        private const val REQUEST_CODE_BCH_SENDING = 914
        private const val REQUEST_CODE_MEMO = 915

        fun newInstance(scanUri: String?, inputDeeplinked: Boolean = false): SendFragment {
            val fragment = SendFragment()
            fragment.arguments = Bundle().apply {
                input = scanUri
                this.inputDeeplinked = inputDeeplinked
            }
            return fragment
        }

        private var Bundle?.input: String?
            get() = this?.getString(ARGUMENT_SEND_INPUT)
            set(v) = this?.putString(ARGUMENT_SEND_INPUT, v) ?: throw NullPointerException()

        private var Bundle?.inputDeeplinked: Boolean
            get() = this?.getBoolean(ARGUMENT_SEND_INPUT_DEEPLINKED) ?: false
            set(v) = this?.putBoolean(ARGUMENT_SEND_INPUT_DEEPLINKED, v) ?: throw NullPointerException()
    }

    override fun isPitEnabled(enabled: Boolean) {
        pitEnabled = enabled
        if (!pitEnabled) {
            pitAddress.gone()
        }
    }

    override fun onBitPayAddressScanned() {
        bitPayAddressScanned = true
        hidePitAddressIcon()
    }

    override fun lastEnteredCryptoAmount(): String {
        return amountCrypto.text?.takeIf { it.isBlank().not() }?.toString() ?: "0"
    }

    override fun show2FANotAvailableError() {
        toast(R.string.the_exchange_two_fa_unavailable, ToastCustom.TYPE_ERROR)
    }

    override fun fillOrClearAddress() {
        if (pitAddressState == PitAddressFieldState.CLEARED) {
            pitAddressState = PitAddressFieldState.FILLED
            pitAddress.setImageResource(R.drawable.vector_dismiss_pit_address)
            presenter.onPitAddressSelected()
            toContainer.toAddressEditTextView.isEnabled = false
        } else {
            pitAddressState = PitAddressFieldState.CLEARED
            pitAddress.setImageResource(R.drawable.ic_the_exchange_colour)
            presenter.onPitAddressCleared()
            toContainer.toAddressEditTextView.isEnabled = true
        }
    }

    override fun hidePitIconForValidAddress() {
        validAddressFilled = true
        hidePitAddressIcon()
    }

    override fun showPitIconIfAvailable() {
        if (pitAddressAvailable && pitEnabled)
            toContainer.pitAddress.visible()
        validAddressFilled = false
    }
}

private fun Memo?.toText(resources: Resources) =
    toTextOrNull(resources) ?: resources.getString(R.string.sunriver_set_memo)

private fun Memo?.toTextOrNull(resources: Resources) =
    if (this == null || isEmpty()) null
    else describeType(resources) + ": " + value

private fun Memo?.valueTextOrNull() =
    if (this == null || isEmpty()) null
    else value

private fun Memo?.describeType(resources: Resources) =
    when {
        this == null || isEmpty() -> null
        else -> when (type) {
            "text" -> resources.getString(R.string.xlm_memo_text)
            "id" -> resources.getString(R.string.xlm_memo_id)
            "hash" -> resources.getString(R.string.xlm_memo_hash)
            "return" -> resources.getString(R.string.xlm_memo_return)
            else -> null
        }
    }

enum class PitAddressFieldState {
    FILLED, CLEARED
}

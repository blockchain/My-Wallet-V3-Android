package piuk.blockchain.android.ui.send

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.support.annotation.ColorRes
import android.support.annotation.Nullable
import android.support.annotation.StringRes
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.AppCompatEditText
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.LinearLayout
import com.jakewharton.rxbinding2.widget.RxTextView
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.confirm_cancel
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.confirm_continue
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.confirm_dont_ask_again
import kotlinx.android.synthetic.main.fragment_send.amountContainer
import kotlinx.android.synthetic.main.fragment_send.arbitraryWarning
import kotlinx.android.synthetic.main.fragment_send.buttonContinue
import kotlinx.android.synthetic.main.fragment_send.coordinator_layout
import kotlinx.android.synthetic.main.fragment_send.currency_header
import kotlinx.android.synthetic.main.fragment_send.edittextCustomFee
import kotlinx.android.synthetic.main.fragment_send.fromContainer
import kotlinx.android.synthetic.main.fragment_send.keyboard
import kotlinx.android.synthetic.main.fragment_send.max
import kotlinx.android.synthetic.main.fragment_send.progressBarMaxAvailable
import kotlinx.android.synthetic.main.fragment_send.scrollView
import kotlinx.android.synthetic.main.fragment_send.spinnerPriority
import kotlinx.android.synthetic.main.fragment_send.textInputLayout
import kotlinx.android.synthetic.main.fragment_send.textviewFeeAbsolute
import kotlinx.android.synthetic.main.fragment_send.textviewFeeTime
import kotlinx.android.synthetic.main.fragment_send.textviewFeeType
import kotlinx.android.synthetic.main.fragment_send.toContainer
import kotlinx.android.synthetic.main.include_amount_row.amountCrypto
import kotlinx.android.synthetic.main.include_amount_row.amountFiat
import kotlinx.android.synthetic.main.include_amount_row.view.amountCrypto
import kotlinx.android.synthetic.main.include_amount_row.view.amountFiat
import kotlinx.android.synthetic.main.include_amount_row.view.currencyCrypto
import kotlinx.android.synthetic.main.include_amount_row.view.currencyFiat
import kotlinx.android.synthetic.main.include_from_row.view.fromAddressTextView
import kotlinx.android.synthetic.main.include_from_row.view.fromArrowImage
import kotlinx.android.synthetic.main.include_to_row_editable.toAddressEditTextView
import kotlinx.android.synthetic.main.include_to_row_editable.toArrow
import kotlinx.android.synthetic.main.include_to_row_editable.view.toAddressEditTextView
import kotlinx.android.synthetic.main.include_to_row_editable.view.toArrow
import kotlinx.android.synthetic.main.view_expanding_currency_header.textview_selected_currency
import piuk.blockchain.android.R
import piuk.blockchain.android.data.connectivity.ConnectivityStatus
import piuk.blockchain.android.data.logging.EventService
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.account.SecondPasswordHandler
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.chooser.AccountChooserActivity
import piuk.blockchain.android.ui.chooser.AccountMode
import piuk.blockchain.android.ui.confirm.ConfirmPaymentDialog
import piuk.blockchain.android.ui.customviews.callbacks.OnTouchOutsideViewListener
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.util.AppRate
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.NumericKeyboardCallback
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Suppress("MemberVisibilityCanBePrivate")
class SendFragment : BaseFragment<SendView, SendPresenter>(), SendView,
    NumericKeyboardCallback {

    override val locale: Locale = Locale.getDefault()

    @Inject lateinit var sendPresenter: SendPresenter
    @Inject lateinit var appUtil: AppUtil

    private var backPressed: Long = 0
    private var progressDialog: MaterialProgressDialog? = null
    private var confirmPaymentDialog: ConfirmPaymentDialog? = null
    private var transactionSuccessDialog: AlertDialog? = null
    private var listener: OnSendFragmentInteractionListener? = null
    private var handlingActivityResult = false

    private val dialogHandler = Handler()
    private val dialogRunnable = Runnable {
        transactionSuccessDialog?.apply {
            if (isShowing && activity != null && !activity!!.isFinishing) {
                dismiss()
            }
        }
    }

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BalanceFragment.ACTION_INTENT) {
                presenter.onBroadcastReceived()
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

        activity?.apply {
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            (activity as MainActivity).setOnTouchOutsideViewListener(currency_header,
                    object : OnTouchOutsideViewListener {
                        override fun onTouchOutside(view: View, event: MotionEvent) {
                            currency_header.close()
                        }
                    })
        }

        setCustomKeypad()

        setupCurrencyHeader()
        handleIncomingArguments()
        setupSendingView()
        setupReceivingView()
        setupBtcTextField()
        setupFiatTextField()
        setupFeesView()

        buttonContinue.setOnClickListener {
            if (ConnectivityStatus.hasConnectivity(activity)) {
                presenter.onContinueClicked()
            } else {
                showSnackbar(R.string.check_connectivity_exit, Snackbar.LENGTH_LONG)
            }
        }
        max.setOnClickListener { presenter.onSpendMaxClicked() }

        onViewReady()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()

        if (!handlingActivityResult) presenter.onResume()

        handlingActivityResult = false

        setupToolbar()
        closeKeypad()

        val filter = IntentFilter(BalanceFragment.ACTION_INTENT)
        LocalBroadcastManager.getInstance(activity!!).registerReceiver(receiver, filter)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(activity!!).unregisterReceiver(receiver)
        currency_header?.close()
    }

    override fun createPresenter() = sendPresenter

    override fun getMvpView() = this

    override fun hideCurrencyHeader() {
        textview_selected_currency?.apply {
            isClickable = false
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

    private fun isKeyboardVisible(): Boolean = keyboard.isVisible

    override fun onKeypadClose() {
        // Show bottom nav if applicable
        if (activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView.restoreBottomNavigation()
            (activity as MainActivity).bottomNavigationView.isBehaviorTranslationEnabled = true
        }

        // Resize activity to default
        scrollView.apply {
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onKeypadOpen() {
        currency_header?.close()
        // Hide bottom nav if applicable
        if (activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView.hideBottomNavigation()
            (activity as MainActivity).bottomNavigationView.isBehaviorTranslationEnabled = false
        }
    }

    override fun onKeypadOpenCompleted() {
        // Resize activity around view
        val translationY = keyboard.height
        scrollView.apply {
            setPadding(0, 0, 0, translationY)
            layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT
            )
        }
    }

    private fun setupCurrencyHeader() {
        ViewUtils.hideKeyboard(activity)
        closeKeypad()
        currency_header.setSelectionListener { currency ->
            when (currency) {
                CryptoCurrencies.BTC -> presenter?.onBitcoinChosen()
                CryptoCurrencies.ETHER -> presenter?.onEtherChosen()
                CryptoCurrencies.BCH -> presenter?.onBitcoinCashChosen()
            }
        }
    }

    private fun setupToolbar() {
        if ((activity as AppCompatActivity).supportActionBar != null) {
            (activity as BaseAuthActivity).setupToolbar(
                    (activity as MainActivity).supportActionBar, R.string.send_bitcoin
            )
        } else {
            finishPage()
        }
    }

    override fun finishPage() {
        listener?.onSendFragmentClose()
    }

    private fun startScanActivity(code: Int) {
        if (!appUtil.isCameraOpen) {
            val intent = Intent(activity, CaptureActivity::class.java)
            startActivityForResult(intent, code)
        } else {
            showSnackbar(R.string.camera_unavailable, Snackbar.LENGTH_LONG)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handlingActivityResult = true

        if (resultCode != Activity.RESULT_OK) return

        when (requestCode) {
            MainActivity.SCAN_URI -> presenter.handleURIScan(
                    data?.getStringExtra(CaptureActivity.SCAN_RESULT),
                    EventService.EVENT_TX_INPUT_FROM_QR
            )
            SCAN_PRIVX -> presenter.handlePrivxScan(data?.getStringExtra(CaptureActivity.SCAN_RESULT))
            REQUEST_CODE_BTC_SENDING -> presenter.selectSendingAccountBtc(data)
            REQUEST_CODE_BTC_RECEIVING -> presenter.selectReceivingAccountBtc(data)

            REQUEST_CODE_BCH_SENDING -> presenter.selectSendingAccountBch(data)
            REQUEST_CODE_BCH_RECEIVING -> presenter.selectReceivingAccountBch(data)
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun setupReceivingView() {
        //Avoid OntouchListener - causes paste issues on some Samsung devices
        toContainer.toAddressEditTextView.setOnClickListener {
            toContainer.toAddressEditTextView.setText("")
            presenter.clearReceivingObject()
        }
        //LongClick listener required to clear receive address in memory when user long clicks to paste
        toContainer.toAddressEditTextView.setOnLongClickListener { v ->
            toContainer.toAddressEditTextView.setText("")
            presenter.clearReceivingObject()
            v.performClick()
            false
        }

        //TextChanged listener required to invalidate receive address in memory when user
        //chooses to edit address populated via QR
        RxTextView.textChanges(toContainer.toAddressEditTextView)
                .doOnNext {
                    if (activity!!.currentFocus === toContainer.toAddressEditTextView) {
                        presenter.clearReceivingObject()
                    }
                }
                .emptySubscribe()

        toContainer.toArrow.setOnClickListener {
            val currency = CurrencyState.getInstance().cryptoCurrency
            AccountChooserActivity.startForResult(
                    this,
                    if (CurrencyState.getInstance().cryptoCurrency == CryptoCurrencies.BTC) {
                        AccountMode.Bitcoin
                    } else {
                        AccountMode.BitcoinCash
                    },
                    if (currency == CryptoCurrencies.BTC) {
                        REQUEST_CODE_BTC_RECEIVING
                    } else {
                        REQUEST_CODE_BCH_RECEIVING
                    },
                    getString(R.string.to)
            )
        }
    }

    override fun updateCryptoCurrency(currency: String) {
        amountContainer.currencyCrypto.text = currency
    }

    override fun updateFiatCurrency(currency: String) {
        amountContainer.currencyFiat.text = currency
    }

    override fun disableCryptoTextChangeListener() {
        amountContainer.amountCrypto.removeTextChangedListener(cryptoTextWatcher)
    }

    @SuppressLint("NewApi")
    override fun enableCryptoTextChangeListener() {
        amountContainer.amountCrypto.addTextChangedListener(cryptoTextWatcher)
        try {
            // This method is hidden but accessible on <API21, but here we catch exceptions just in case
            amountContainer.amountCrypto.showSoftInputOnFocus = false
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun updateCryptoAmount(amountString: String?) {
        amountContainer.amountCrypto.setText(amountString)
    }

    override fun disableFiatTextChangeListener() {
        amountContainer.amountFiat.removeTextChangedListener(fiatTextWatcher)
    }

    @SuppressLint("NewApi")
    override fun enableFiatTextChangeListener() {
        amountContainer.amountFiat.addTextChangedListener(fiatTextWatcher)
        try {
            // This method is hidden but accessible on <API21, but here we catch exceptions just in case
            amountContainer.amountFiat.showSoftInputOnFocus = false
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun updateFiatAmount(amountString: String?) {
        amountContainer.amountFiat.setText(amountString)
    }

    // BTC Field
    @SuppressLint("NewApi")
    private fun setupBtcTextField() {
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

    private val cryptoTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // No-op
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // No-op
        }

        override fun afterTextChanged(editable: Editable) {
            presenter.updateFiatTextField(editable, amountContainer.amountCrypto)
            updateTotals()
        }
    }

    private val fiatTextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // No-op
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // No-op
        }

        override fun afterTextChanged(editable: Editable) {
            presenter.updateCryptoTextField(editable, amountContainer.amountFiat)
            updateTotals()
        }
    }

    private fun handleIncomingArguments() {
        if (arguments != null) {

            presenter.selectDefaultOrFirstFundedSendingAccount()

            val scanData = arguments!!.getString(ARGUMENT_SCAN_DATA)
            val metricInputFlag = arguments!!.getString(ARGUMENT_SCAN_DATA_ADDRESS_INPUT_ROUTE)

            if (scanData != null) {
                handlingActivityResult = true
                presenter.handleURIScan(scanData, metricInputFlag)
            }
        }
    }

    private fun setupSendingView() {
        fromContainer.fromAddressTextView.setOnClickListener { startFromFragment() }
        fromContainer.fromArrowImage.setOnClickListener { startFromFragment() }
    }

    override fun updateSendingAddress(label: String) {
        fromContainer.fromAddressTextView.text = label
    }

    override fun updateReceivingHint(hint: Int) {
        toContainer.toAddressEditTextView.setHint(hint)
    }

    private fun startFromFragment() {
        val currency = CurrencyState.getInstance().cryptoCurrency
        AccountChooserActivity.startForResult(
                this,
                if (CurrencyState.getInstance().cryptoCurrency == CryptoCurrencies.BTC) {
                    AccountMode.Bitcoin
                } else {
                    AccountMode.BitcoinCashSend
                },
                if (currency == CryptoCurrencies.BTC) {
                    REQUEST_CODE_BTC_SENDING
                } else {
                    REQUEST_CODE_BCH_SENDING
                },
                getString(R.string.from)
        )
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

    fun onBackPressed() {
        if (isKeyboardVisible()) {
            closeKeypad()
        } else {
            handleBackPressed()
        }
    }

    override fun setSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        currency_header.setCurrentlySelectedCurrency(cryptoCurrency)
    }

    private fun handleBackPressed() {
        when {
            isKeyboardVisible() -> closeKeypad()
            currency_header.isOpen() -> currency_header.close()
            else -> {
                if (backPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
                    AccessState.getInstance().logout(context)
                    return
                } else {
                    toast(R.string.exit_confirm)
                }

                backPressed = System.currentTimeMillis()
            }
        }
    }

    fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        toast(getString(message), toastType)
    }

    override fun showSnackbar(message: Int, duration: Int) {
        showSnackbar(getString(message), null, duration)
    }

    override fun showSnackbar(message: String, @Nullable extraInfo: String?, duration: Int) {
        activity?.run {
            val snackbar = Snackbar.make(findViewById(R.id.coordinator_layout), message, duration)
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
        activity?.run {
            Snackbar.make(
                    findViewById(R.id.coordinator_layout),
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

    override fun showSendingFieldDropdown() {
        fromContainer.fromArrowImage.visible()
        fromContainer.fromAddressTextView.isClickable = true
    }

    override fun hideSendingFieldDropdown() {
        fromContainer.fromArrowImage.gone()
        fromContainer.fromAddressTextView.isClickable = false
    }

    override fun showReceivingDropdown() {
        toContainer.toArrow.visible()
        toContainer.toAddressEditTextView.isClickable = true
    }

    override fun hideReceivingDropdown() {
        toContainer.toArrow.gone()
        toContainer.toAddressEditTextView.isClickable = false
    }

    override fun updateReceivingAddress(address: String) {
        toContainer.toAddressEditTextView.setText(address)
    }

    private fun setupFeesView() {
        val adapter = FeePriorityAdapter(activity!!, presenter.getFeeOptionsForDropDown())

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
                        buttonContinue.isEnabled = true
                        textviewFeeAbsolute.visibility = View.VISIBLE
                        textInputLayout.visibility = View.GONE
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
        val position = spinnerPriority.selectedItemPosition
        return when (position) {
            1 -> FeeType.FEE_OPTION_PRIORITY
            2 -> FeeType.FEE_OPTION_CUSTOM
            else -> FeeType.FEE_OPTION_REGULAR
        }
    }

    override fun getCustomFeeValue(): Long {
        val amount = edittextCustomFee.text.toString()
        return if (!amount.isEmpty()) amount.toLong() else 0
    }

    override fun showMaxAvailable() {
        max.visible()
        progressBarMaxAvailable.invisible()
    }

    override fun hideMaxAvailable() {
        max.invisible()
        progressBarMaxAvailable.visible()
    }

    override fun updateWarning(message: String) {
        arbitraryWarning?.apply {
            visible()
            text = message
        }
    }

    override fun clearWarning() {
        arbitraryWarning?.apply {
            gone()
            text = ""
        }
    }

    override fun updateFeeAmount(fee: String) {
        textviewFeeAbsolute.text = fee
    }

    override fun updateMaxAvailable(maxAmount: String) {
        max.text = maxAmount
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

        activity?.run {
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
        activity?.run {
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
        val clipMan = activity!!.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = clipMan.primaryClip
        return if (clip != null && clip.itemCount > 0) {
            clip.getItemAt(0).coerceToText(activity).toString()
        } else null
    }

    private fun playAudio() {
        activity?.run {
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

    override fun showProgressDialog(title: Int) {
        progressDialog =
                MaterialProgressDialog(activity)
        progressDialog?.apply {
            setCancelable(false)
            setMessage(R.string.please_wait)
            show()
        }
    }

    override fun dismissProgressDialog() {
        if (progressDialog != null) {
            progressDialog?.apply { dismiss() }
            progressDialog = null
        }
    }

    override fun showSpendFromWatchOnlyWarning(address: String) {
        activity?.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.privx_required)
                    .setMessage(
                            String.format(
                                    getString(R.string.watch_only_spend_instructionss),
                                    address
                            )
                    )
                    .setCancelable(false)
                    .setPositiveButton(R.string.dialog_continue) { _, _ -> requestScanPermissions() }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
        }
    }

    private fun requestScanPermissions() {
        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
                .with(coordinator_layout, R.string.request_camera_permission)
                .withButton(android.R.string.ok) { requestScanPermissions() }
                .build()

        val grantedPermissionListener = object : BasePermissionListener() {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                startScanActivity(SCAN_PRIVX)
            }
        }

        val compositePermissionListener =
                CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)

        Dexter.withActivity(requireActivity())
                .withPermission(Manifest.permission.CAMERA)
                .withListener(compositePermissionListener)
                .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
                .check()
    }

    override fun showSecondPasswordDialog() {
        SecondPasswordHandler(context).validate(object : SecondPasswordHandler.ResultListener {
            override fun onNoSecondPassword() {
                presenter.onNoSecondPassword()
            }

            override fun onSecondPasswordValidated(validateSecondPassword: String) {
                presenter.onSecondPasswordValidated(validateSecondPassword)
            }
        })
    }

    override fun showPaymentDetails(
            confirmationDetails: PaymentConfirmationDetails,
            note: String?,
            allowFeeChange: Boolean
    ) {
        confirmPaymentDialog =
                ConfirmPaymentDialog.newInstance(confirmationDetails, note, allowFeeChange)
        confirmPaymentDialog?.show(fragmentManager, ConfirmPaymentDialog::class.java.simpleName)
    }

    override fun showLargeTransactionWarning() {
        coordinator_layout.postDelayed({
            activity?.run {
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
        activity?.run {
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

    internal fun displayCustomFeeField() {
        textviewFeeAbsolute.visibility = View.GONE
        textviewFeeTime.visibility = View.INVISIBLE
        textInputLayout.visibility = View.VISIBLE
        buttonContinue.isEnabled = false
        textInputLayout.hint = getString(R.string.fee_options_sat_byte_hint)

        edittextCustomFee.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus || !edittextCustomFee.text.toString().isEmpty()) {
                textInputLayout.hint = getString(
                        R.string.fee_options_sat_byte_inline_hint,
                        presenter.getBitcoinFeeOptions()?.regularFee.toString(),
                        presenter.getBitcoinFeeOptions()?.priorityFee.toString()
                )
            } else if (edittextCustomFee.text.toString().isEmpty()) {
                textInputLayout.hint = getString(R.string.fee_options_sat_byte_hint)
            } else {
                textInputLayout.hint = getString(R.string.fee_options_sat_byte_hint)
            }
        }

        RxTextView.textChanges(edittextCustomFee)
                .skip(1)
                .map { it.toString() }
                .doOnNext { buttonContinue.isEnabled = !it.isEmpty() && it != "0" }
                .filter { !it.isEmpty() }
                .map { it.toLong() }
                .onErrorReturnItem(0L)
                .doOnNext { value ->
                    if (presenter.getBitcoinFeeOptions() != null && value < presenter.getBitcoinFeeOptions()!!.limits.min) {
                        textInputLayout.error = getString(R.string.fee_options_fee_too_low)
                    } else if (presenter.getBitcoinFeeOptions() != null && value > presenter.getBitcoinFeeOptions()!!.limits.max) {
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

    override fun showTransactionSuccess(
            hash: String,
            transactionValue: Long,
            cryptoCurrency: CryptoCurrencies
    ) {

        playAudio()

        val appRate = AppRate(activity)
                .setMinTransactionsUntilPrompt(3)
                .incrementTransactionCount()

        activity?.run {
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

                if (cryptoCurrency == CryptoCurrencies.ETHER) {
                    setMessage(getString(R.string.eth_transaction_complete))
                }

                if (!isFinishing) show()
            }
        }

        dialogHandler.postDelayed(dialogRunnable, (10 * 1000).toLong())
    }

    interface OnSendFragmentInteractionListener {
        fun onSendFragmentClose()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnSendFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnSendFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun enableInput() {
        toAddressEditTextView.isEnabled = true
        toArrow.isEnabled = true
        amountCrypto.isEnabled = true
        amountFiat.isEnabled = true
        max.isEnabled = true
    }

    override fun disableInput() {
        toAddressEditTextView.isEnabled = false
        toArrow.isEnabled = false
        amountCrypto.isEnabled = false
        amountFiat.isEnabled = false
        max.isEnabled = false
    }

    companion object {

        const val SCAN_PRIVX = 2011
        const val ARGUMENT_SCAN_DATA = "scan_data"
        const val ARGUMENT_SELECTED_ACCOUNT_POSITION = "selected_account_position"
        const val ARGUMENT_SCAN_DATA_ADDRESS_INPUT_ROUTE = "address_input_route"

        private const val COOL_DOWN_MILLIS = 2 * 1000
        private const val ARGUMENT_CONTACT_ID = "contact_id"
        private const val ARGUMENT_CONTACT_MDID = "contact_mdid"
        private const val ARGUMENT_FCTX_ID = "fctx_id"
        private const val REQUEST_CODE_BTC_RECEIVING = 911
        private const val REQUEST_CODE_BTC_SENDING = 912
        private const val REQUEST_CODE_BCH_RECEIVING = 913
        private const val REQUEST_CODE_BCH_SENDING = 914

        @JvmStatic
        fun newInstance(
                scanData: String?,
                scanRoute: String?,
                selectedAccountPosition: Int
        ): SendFragment {
            val fragment = SendFragment()
            val args = Bundle()
            args.putString(ARGUMENT_SCAN_DATA, scanData)
            args.putString(ARGUMENT_SCAN_DATA_ADDRESS_INPUT_ROUTE, scanRoute)
            args.putInt(ARGUMENT_SELECTED_ACCOUNT_POSITION, selectedAccountPosition)
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun newInstance(
                uri: String,
                contactId: String,
                contactMdid: String,
                fctxId: String
        ): SendFragment {
            val fragment = SendFragment()
            val args = Bundle()
            args.putString(ARGUMENT_SCAN_DATA, uri)
            args.putString(ARGUMENT_CONTACT_ID, contactId)
            args.putString(ARGUMENT_CONTACT_MDID, contactMdid)
            args.putString(ARGUMENT_FCTX_ID, fctxId)
            fragment.arguments = args
            return fragment
        }
    }
}
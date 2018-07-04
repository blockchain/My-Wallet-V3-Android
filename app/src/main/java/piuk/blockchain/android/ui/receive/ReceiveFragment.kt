package piuk.blockchain.android.ui.receive

import android.Manifest
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import com.fasterxml.jackson.databind.ObjectMapper
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.confirm_cancel
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.confirm_continue
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.confirm_dont_ask_again
import kotlinx.android.synthetic.main.fragment_receive.amount_container
import kotlinx.android.synthetic.main.fragment_receive.button_request
import kotlinx.android.synthetic.main.fragment_receive.coordinator_layout_receive
import kotlinx.android.synthetic.main.fragment_receive.currency_header
import kotlinx.android.synthetic.main.fragment_receive.custom_keyboard
import kotlinx.android.synthetic.main.fragment_receive.divider1
import kotlinx.android.synthetic.main.fragment_receive.divider3
import kotlinx.android.synthetic.main.fragment_receive.divider4
import kotlinx.android.synthetic.main.fragment_receive.divider_to
import kotlinx.android.synthetic.main.fragment_receive.from_container
import kotlinx.android.synthetic.main.fragment_receive.image_qr
import kotlinx.android.synthetic.main.fragment_receive.progressbar
import kotlinx.android.synthetic.main.fragment_receive.scrollview
import kotlinx.android.synthetic.main.fragment_receive.textview_receiving_address
import kotlinx.android.synthetic.main.fragment_receive.textview_whats_this
import kotlinx.android.synthetic.main.fragment_receive.to_container
import kotlinx.android.synthetic.main.include_amount_row.amountCrypto
import kotlinx.android.synthetic.main.include_amount_row.amountFiat
import kotlinx.android.synthetic.main.include_amount_row.currencyCrypto
import kotlinx.android.synthetic.main.include_amount_row.currencyFiat
import kotlinx.android.synthetic.main.include_amount_row.view.amountCrypto
import kotlinx.android.synthetic.main.include_amount_row.view.amountFiat
import kotlinx.android.synthetic.main.include_from_row.fromAddressTextView
import kotlinx.android.synthetic.main.include_from_row.fromArrowImage
import kotlinx.android.synthetic.main.include_from_row.view.fromAddressTextView
import kotlinx.android.synthetic.main.include_from_row.view.fromArrowImage
import kotlinx.android.synthetic.main.include_to_row.constraint_layout_to_row
import kotlinx.android.synthetic.main.include_to_row.toAddressTextView
import kotlinx.android.synthetic.main.include_to_row.toArrowImage
import kotlinx.android.synthetic.main.view_expanding_currency_header.textview_selected_currency
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.chooser.AccountChooserActivity
import piuk.blockchain.android.ui.chooser.AccountChooserActivity.Companion.EXTRA_SELECTED_ITEM
import piuk.blockchain.android.ui.chooser.AccountChooserActivity.Companion.EXTRA_SELECTED_OBJECT_TYPE
import piuk.blockchain.android.ui.chooser.AccountMode
import piuk.blockchain.android.ui.contacts.IntroducingContactsPromptDialog
import piuk.blockchain.android.ui.customviews.callbacks.OnTouchOutsideViewListener
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.util.EditTextFormatUtil
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.contacts.models.PaymentRequestType
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.extensions.emptySubscribe
import piuk.blockchain.androidcore.utils.extensions.toKotlinObject
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import piuk.blockchain.androidcoreui.ui.base.BaseFragment
import piuk.blockchain.androidcoreui.ui.customviews.NumericKeyboardCallback
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.AppUtil
import piuk.blockchain.androidcoreui.utils.extensions.disableSoftKeyboard
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import timber.log.Timber
import java.io.IOException
import java.text.DecimalFormatSymbols
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Suppress("MemberVisibilityCanPrivate")
class ReceiveFragment : BaseFragment<ReceiveView, ReceivePresenter>(), ReceiveView,
    NumericKeyboardCallback {

    override val isContactsEnabled: Boolean = BuildConfig.CONTACTS_ENABLED
    override val locale: Locale = Locale.getDefault()

    @Suppress("MemberVisibilityCanBePrivate")
    @Inject lateinit var receivePresenter: ReceivePresenter
    @Inject lateinit var appUtil: AppUtil
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var listener: OnReceiveFragmentInteractionListener? = null

    private var textChangeAllowed = true
    private var backPressed: Long = 0
    private var textChangeSubject = PublishSubject.create<String>()
    private var selectedAccountPosition = -1
    private var handlingActivityResult = false

    private val intentFilter = IntentFilter(BalanceFragment.ACTION_INTENT)
    private val defaultDecimalSeparator =
            DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    private val receiveIntentHelper by unsafeLazy {
        ReceiveIntentHelper(context!!, appUtil)
    }
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BalanceFragment.ACTION_INTENT) {
                presenter?.apply {
                    // Update UI with new Address + QR
                    onResume(selectedAccountPosition)
                }
            }
        }
    }

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        arguments?.run {
            selectedAccountPosition = getInt(ARG_SELECTED_ACCOUNT_POSITION)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_receive)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        activity?.apply {
            (activity as MainActivity).setOnTouchOutsideViewListener(
                    currency_header,
                    object : OnTouchOutsideViewListener {
                        override fun onTouchOutside(view: View, event: MotionEvent) {
                            currency_header.close()
                        }
                    }
            )
        }

        onViewReady()
        setupLayout()
        setCustomKeypad()

        scrollview?.post { scrollview.scrollTo(0, 0) }

        currency_header.setSelectionListener { currency ->
            when (currency) {
                CryptoCurrencies.BTC -> presenter?.onSelectDefault(selectedAccountPosition)
                CryptoCurrencies.ETHER -> presenter?.onEthSelected()
                CryptoCurrencies.BCH -> presenter?.onSelectBchDefault()
            }
        }
    }

    private fun setupToolbar() {
        if ((activity as AppCompatActivity).supportActionBar != null) {
            (activity as BaseAuthActivity).setupToolbar(
                    (activity as MainActivity).supportActionBar, R.string.receive_bitcoin
            )
        } else {
            finishPage()
        }
    }

    override fun disableCurrencyHeader() {
        textview_selected_currency?.apply {
            isClickable = false
        }
    }

    private fun setupLayout() {
        if (!presenter.shouldShowDropdown()) {
            constraint_layout_to_row.gone()
            divider_to.gone()
        }

        // BTC Field
        amountCrypto.apply {
            hint = "0${defaultDecimalSeparator}00"
            addTextChangedListener(btcTextWatcher)
            disableSoftKeyboard()
        }

        // Fiat Field
        amountFiat.apply {
            hint = "0${defaultDecimalSeparator}00"
            addTextChangedListener(fiatTextWatcher)
            disableSoftKeyboard()
        }

        // Units
        currencyCrypto.text = presenter.getCryptoUnit()
        currencyFiat.text = presenter.getFiatUnit()

        // QR Code
        image_qr.apply {
            setOnClickListener { showClipboardWarning() }
            setOnLongClickListener { consume { onShareClicked() } }
        }

        // Receive address
        textview_receiving_address.setOnClickListener { showClipboardWarning() }

        val toListener: (View) -> Unit = {
            val currency = CurrencyState.getInstance().cryptoCurrency
            AccountChooserActivity.startForResult(
                    this,
                    if (currency == CryptoCurrencies.BTC) {
                        AccountMode.Bitcoin
                    } else {
                        AccountMode.BitcoinCash
                    },
                    if (currency == CryptoCurrencies.BTC) {
                        REQUEST_CODE_RECEIVE_BITCOIN
                    } else {
                        REQUEST_CODE_RECEIVE_BITCOIN_CASH
                    },
                    getString(R.string.to)
            )
        }

        toAddressTextView.setOnClickListener(toListener)
        toArrowImage.setOnClickListener(toListener)

        textChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { presenter.onBitcoinAmountChanged(getBtcAmount()) }
                .emptySubscribe()

        fromAddressTextView.setHint(R.string.contact_select)

        textview_whats_this.setOnClickListener {
            IntroducingContactsPromptDialog.newInstance().apply {
                setDismissButtonListener {
                    PrefsUtil(activity)
                            .setValue(PrefsUtil.KEY_CONTACTS_INTRODUCTION_COMPLETE, true)
                    dialog.dismiss()
                    hideContactsIntroduction()
                    showDialog(fragmentManager)
                }
            }
        }

        from_container.fromAddressTextView.setOnClickListener {
            presenter.clearSelectedContactId()
            presenter.onSendToContactClicked()
        }

        from_container.fromArrowImage.setOnClickListener {
            presenter.clearSelectedContactId()
            presenter.onSendToContactClicked()
        }

        button_request.setOnClickListener {
            // TODO: This may or may not need enabling again in the future  
//            if (presenter.selectedContactId == null) {
//                showToast(R.string.contact_select_first, ToastCustom.TYPE_ERROR)
//            } else if (!presenter.isValidAmount(getBtcAmount())) {
//                showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR)
//            } else {
//                listener?.onTransactionNotesRequested(
//                        presenter.getConfirmationDetails(),
//                        PaymentRequestType.REQUEST,
//                        presenter.selectedContactId!!,
//                        presenter.currencyHelper.getLongAmount(
//                                amountCrypto.text.toString()),
//                        presenter.getSelectedAccountPosition()
//                )
//            }

            onShareClicked()
        }

        @Suppress("ConstantConditionIf")
        if (!BuildConfig.CONTACTS_ENABLED) {
            from_container.gone()
            textview_whats_this.gone()
            divider4.gone()
        }
    }

    private val btcTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            var editable = s
            amountCrypto.removeTextChangedListener(this)
            editable = EditTextFormatUtil.formatEditable(
                    editable,
                    presenter.getMaxCryptoDecimalLength(),
                    amountCrypto,
                    defaultDecimalSeparator
            )

            amountCrypto.addTextChangedListener(this)

            if (textChangeAllowed) {
                textChangeAllowed = false
                presenter.updateFiatTextField(editable.toString())
                textChangeSubject.onNext(editable.toString())
                textChangeAllowed = true
            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // No-op
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // No-op
        }
    }

    private val fiatTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable) {
            var editable = s
            amountFiat.removeTextChangedListener(this)
            val maxLength = 2
            editable = EditTextFormatUtil.formatEditable(
                    editable,
                    maxLength,
                    amountFiat,
                    defaultDecimalSeparator
            )

            amountFiat.addTextChangedListener(this)

            if (textChangeAllowed) {
                textChangeAllowed = false
                presenter.updateBtcTextField(editable.toString())
                textChangeSubject.onNext(editable.toString())
                textChangeAllowed = true
            }
        }

        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
            // No-op
        }

        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            // No-op
        }
    }

    override fun getBtcAmount() = amountCrypto.getTextString()

    override fun updateReceiveAddress(address: String) {
        if (!isRemoving) {
            textview_receiving_address.text = address
        }
    }

    override fun hideContactsIntroduction() {
        fromArrowImage.visible()
        textview_whats_this.gone()
    }

    override fun showContactsIntroduction() {
        fromArrowImage.invisible()
        textview_whats_this.visible()
    }

    override fun getContactName() = toAddressTextView.text.toString()

    override fun updateFiatTextField(text: String) {
        amountFiat.setText(text)
    }

    override fun updateBtcTextField(text: String) {
        amountCrypto.setText(text)
    }

    override fun onResume() {
        super.onResume()

        if (!handlingActivityResult)
            presenter.onResume(selectedAccountPosition)

        handlingActivityResult = false

        closeKeypad()
        setupToolbar()
        LocalBroadcastManager.getInstance(context!!)
                .registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun showQrLoading() {
        image_qr.invisible()
        textview_receiving_address.invisible()
        progressbar.visible()
    }

    override fun showQrCode(bitmap: Bitmap?) {
        if (!isRemoving) {
            progressbar.invisible()
            image_qr.visible()
            textview_receiving_address.visible()
            image_qr.setImageBitmap(bitmap)
        }
    }

    private fun displayBitcoinLayout() {
        divider1.visible()
        amount_container.visible()
        divider3.visible()

        if (isContactsEnabled) {
            from_container.visible()
            textview_whats_this.visible()
            divider4.visible()
            button_request.visible()
        }

        if (presenter.shouldShowDropdown()) {
            to_container.visible()
            divider_to.visible()
        } else {
            to_container.gone()
            divider_to.gone()
        }
    }

    private fun displayEtherLayout() {
        if (custom_keyboard.isVisible) {
            custom_keyboard.hideKeyboard()
        }
        divider1.gone()
        amount_container.gone()
        divider_to.gone()
        to_container.gone()
        divider3.gone()

        if (isContactsEnabled) {
            from_container.gone()
            textview_whats_this.gone()
            divider4.gone()
            button_request.gone()
        }
    }

    private fun displayBitcoinCashLayout() {
        if (custom_keyboard.isVisible) {
            custom_keyboard.hideKeyboard()
        }
        divider1.gone()
        amount_container.gone()
        divider3.visible()

        if (presenter.shouldShowDropdown()) {
            to_container.visible()
            divider_to.visible()
        } else {
            to_container.gone()
            divider_to.gone()
        }
    }

    override fun setSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        currency_header.setCurrentlySelectedCurrency(cryptoCurrency)
        when (cryptoCurrency) {
            CryptoCurrencies.BTC -> displayBitcoinLayout()
            CryptoCurrencies.ETHER -> displayEtherLayout()
            CryptoCurrencies.BCH -> displayBitcoinCashLayout()
        }
    }

    override fun startContactSelectionActivity() {
        AccountChooserActivity.startForResult(
                this,
                AccountMode.ContactsOnly,
                REQUEST_CODE_CHOOSE_CONTACT,
                getString(R.string.from)
        )
    }

    override fun updateReceiveLabel(label: String) {
        toAddressTextView.text = label
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        handlingActivityResult = true

        // Set receiving account
        if (resultCode == Activity.RESULT_OK
            && requestCode == REQUEST_CODE_RECEIVE_BITCOIN
            && data != null
        ) {

            try {
                val type: Class<*> = Class.forName(data.getStringExtra(EXTRA_SELECTED_OBJECT_TYPE))
                val any = ObjectMapper().readValue(data.getStringExtra(EXTRA_SELECTED_ITEM), type)

                when (any) {
                    is LegacyAddress -> presenter.onLegacyAddressSelected(any)
                    is Account -> presenter.onAccountSelected(any)
                    else -> throw IllegalArgumentException("No method for handling $type available")
                }
            } catch (e: ClassNotFoundException) {
                Timber.e(e)
                presenter.onSelectDefault(selectedAccountPosition)
            } catch (e: IOException) {
                Timber.e(e)
                presenter.onSelectDefault(selectedAccountPosition)
            }

        } else if (resultCode == Activity.RESULT_OK
            && requestCode == REQUEST_CODE_RECEIVE_BITCOIN_CASH
            && data != null
        ) {

            try {
                val type: Class<*> = Class.forName(data.getStringExtra(EXTRA_SELECTED_OBJECT_TYPE))
                val any = ObjectMapper().readValue(data.getStringExtra(EXTRA_SELECTED_ITEM), type)

                when (any) {
                    is LegacyAddress -> presenter.onLegacyBchAddressSelected(any)
                    is GenericMetadataAccount -> presenter.onBchAccountSelected(any)
                    else -> throw IllegalArgumentException("No method for handling $type available")
                }
            } catch (e: ClassNotFoundException) {
                Timber.e(e)
                presenter.onSelectBchDefault()
            } catch (e: IOException) {
                Timber.e(e)
                presenter.onSelectBchDefault()
            }

            // Choose contact for request
        } else if (resultCode == Activity.RESULT_OK
            && requestCode == REQUEST_CODE_CHOOSE_CONTACT
            && data != null
        ) {
            try {
                val contact: Contact = data.getStringExtra(EXTRA_SELECTED_ITEM).toKotlinObject()
                presenter.selectedContactId = contact.id
                from_container.fromAddressTextView.text = contact.name
            } catch (e: IOException) {
                throw RuntimeException(e)
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun showBottomSheet(uri: String) {
        receiveIntentHelper.getIntentDataList(uri, getQrBitmap())?.let {
            val adapter = ShareReceiveIntentAdapter(it).apply {
                setItemClickedListener { bottomSheetDialog?.dismiss() }
            }

            val sheetView = View.inflate(activity, R.layout.bottom_sheet_receive, null)
            sheetView.findViewById<RecyclerView>(R.id.recycler_view).apply {
                this.adapter = adapter
                layoutManager = LinearLayoutManager(context)
            }

            bottomSheetDialog = BottomSheetDialog(context!!, R.style.BottomSheetDialog).apply {
                setContentView(sheetView)
            }

            adapter.notifyDataSetChanged()
        }

        bottomSheetDialog?.apply { show() }
        if (bottomSheetDialog == null) {
            toast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
        }
    }

    private fun onShareClicked() {
        activity?.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.receive_address_to_share)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ -> requestStoragePermissionIfNeeded() }
                    .setNegativeButton(R.string.no, null)
                    .show()
        }
    }

    private fun requestStoragePermissionIfNeeded() {
        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
                .with(coordinator_layout_receive, R.string.request_write_storage_permission)
                .withButton(android.R.string.ok) { requestStoragePermissionIfNeeded() }
                .build()

        val grantedPermissionListener = object : BasePermissionListener() {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                presenter.onShowBottomSheetSelected()
            }
        }

        val compositePermissionListener =
                CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)

        Dexter.withActivity(requireActivity())
                .withPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                .withListener(compositePermissionListener)
                .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
                .check()
    }

    override fun showWatchOnlyWarning() {
        activity?.run {
            val dialogView = layoutInflater.inflate(R.layout.alert_watch_only_spend, null)
            val alertDialog = AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setView(dialogView.rootView)
                    .setCancelable(false)
                    .create()

            dialogView.confirm_cancel.setOnClickListener {
                presenter.onSelectDefault(selectedAccountPosition)
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

    override fun getQrBitmap(): Bitmap = (image_qr.drawable as BitmapDrawable).bitmap

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        toast(message, toastType)
    }

    fun getSelectedAccountPosition(): Int = presenter.getSelectedAccountPosition()

    fun onBackPressed() = handleBackPressed()

    private fun showClipboardWarning() {
        val address = textview_receiving_address.text
        activity?.run {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.receive_address_to_clipboard)
                    .setCancelable(false)
                    .setPositiveButton(R.string.yes) { _, _ ->
                        val clipboard =
                                getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Send address", address)
                        toast(R.string.copied_to_clipboard)
                        clipboard.primaryClip = clip
                    }
                    .setNegativeButton(R.string.no, null)
                    .show()
        }
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
                    onExitConfirmToast()
                }

                backPressed = System.currentTimeMillis()
            }
        }
    }

    private fun onExitConfirmToast() {
        toast(R.string.exit_confirm)
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(context!!).unregisterReceiver(broadcastReceiver)
        currency_header?.close()
    }

    override fun finishPage() {
        listener?.onReceiveFragmentClose()
    }

    private fun setCustomKeypad() {
        custom_keyboard.apply {
            setCallback(this@ReceiveFragment)
            setDecimalSeparator(defaultDecimalSeparator)
            // Enable custom keypad and disables default keyboard from popping up
            enableOnView(amount_container.amountCrypto)
            enableOnView(amount_container.amountFiat)
        }

        amount_container.amountCrypto.apply {
            setText("")
            requestFocus()
        }
    }

    private fun closeKeypad() {
        custom_keyboard.setNumpadVisibility(View.GONE)
    }

    private fun isKeyboardVisible(): Boolean = custom_keyboard.isVisible

    override fun createPresenter() = receivePresenter

    override fun getMvpView() = this

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnReceiveFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("${context!!} must implement OnReceiveFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onKeypadClose() {
        // Show bottom nav if applicable
        if (activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView.restoreBottomNavigation()
            (activity as MainActivity).bottomNavigationView.isBehaviorTranslationEnabled = true
        }

        val height = activity!!.resources.getDimension(R.dimen.action_bar_height).toInt()
        // Resize activity to default
        scrollview.apply {
            setPadding(0, 0, 0, 0)
            layoutParams = CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(0, height, 0, height) }

            postDelayed({ smoothScrollTo(0, 0) }, 100)
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
        val height = activity!!.resources.getDimension(R.dimen.action_bar_height).toInt()
        scrollview.apply {
            setPadding(0, 0, 0, custom_keyboard.height)
            layoutParams = CoordinatorLayout.LayoutParams(
                    CoordinatorLayout.LayoutParams.MATCH_PARENT,
                    CoordinatorLayout.LayoutParams.MATCH_PARENT
            ).apply { setMargins(0, height, 0, 0) }

            scrollTo(0, bottom)
        }
    }

    interface OnReceiveFragmentInteractionListener {

        fun onReceiveFragmentClose()

        fun onTransactionNotesRequested(
                paymentConfirmationDetails: PaymentConfirmationDetails,
                paymentRequestType: PaymentRequestType,
                contactId: String,
                satoshis: Long,
                accountPosition: Int
        )

    }

    companion object {

        private const val REQUEST_CODE_RECEIVE_BITCOIN = 800
        private const val REQUEST_CODE_RECEIVE_BITCOIN_CASH = 801
        private const val REQUEST_CODE_CHOOSE_CONTACT = 802

        private const val ARG_SELECTED_ACCOUNT_POSITION = "ARG_SELECTED_ACCOUNT_POSITION"
        private const val COOL_DOWN_MILLIS = 2 * 1000

        @JvmStatic
        fun newInstance(selectedAccountPosition: Int) = ReceiveFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_SELECTED_ACCOUNT_POSITION, selectedAccountPosition)
            }
        }

    }

}

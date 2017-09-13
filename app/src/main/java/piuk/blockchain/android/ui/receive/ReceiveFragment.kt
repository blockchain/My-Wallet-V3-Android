package piuk.blockchain.android.ui.receive

import android.Manifest
import android.app.Activity
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.CoordinatorLayout
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import com.fasterxml.jackson.databind.ObjectMapper
import info.blockchain.wallet.contacts.data.Contact
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.payload.data.LegacyAddress
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.alert_watch_only_spend.view.*
import kotlinx.android.synthetic.main.fragment_receive.*
import kotlinx.android.synthetic.main.include_amount_row.*
import kotlinx.android.synthetic.main.include_amount_row.view.*
import kotlinx.android.synthetic.main.include_from_row.*
import kotlinx.android.synthetic.main.include_from_row.view.*
import kotlinx.android.synthetic.main.include_to_row.*
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.R
import piuk.blockchain.android.data.access.AccessState
import piuk.blockchain.android.data.contacts.models.PaymentRequestType
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.PaymentConfirmationDetails
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.base.BaseAuthActivity
import piuk.blockchain.android.ui.base.BaseFragment
import piuk.blockchain.android.ui.chooser.AccountChooserActivity
import piuk.blockchain.android.ui.chooser.AccountChooserActivity.EXTRA_SELECTED_ITEM
import piuk.blockchain.android.ui.chooser.AccountChooserActivity.EXTRA_SELECTED_OBJECT_TYPE
import piuk.blockchain.android.ui.contacts.IntroducingContactsPromptDialog
import piuk.blockchain.android.ui.customviews.NumericKeyboardCallback
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.util.EditTextFormatUtil
import piuk.blockchain.android.util.PermissionUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.extensions.*
import piuk.blockchain.android.util.helperfunctions.consume
import piuk.blockchain.android.util.helperfunctions.setOnTabSelectedListener
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.io.IOException
import java.text.DecimalFormatSymbols
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@Suppress("MemberVisibilityCanPrivate")
class ReceiveFragment : BaseFragment<ReceiveView, ReceivePresenter>(), ReceiveView, NumericKeyboardCallback {

    @Inject lateinit var receivePresenter: ReceivePresenter
    private var bottomSheetDialog: BottomSheetDialog? = null
    private var listener: OnReceiveFragmentInteractionListener? = null

    private var textChangeAllowed = true
    private var backPressed: Long = 0
    private var textChangeSubject = PublishSubject.create<String>()
    private var defaultAccountPosition = -1

    private val intentFilter = IntentFilter(BalanceFragment.ACTION_INTENT)
    private val defaultDecimalSeparator = DecimalFormatSymbols.getInstance().decimalSeparator.toString()
    private val receiveIntentHelper by unsafeLazy { ReceiveIntentHelper(context) }
    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == BalanceFragment.ACTION_INTENT) {
                presenter?.let {
                    // Update UI with new Address + QR
                    if (tabs_receive.selectedTabPosition == 0) {
                        presenter.onSelectDefault(defaultAccountPosition)
                    }
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

        if (arguments != null) {
            defaultAccountPosition = arguments.getInt(ARG_SELECTED_ACCOUNT_POSITION)
        }
    }

    override fun onCreateView(
            inflater: LayoutInflater?,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ) = container?.inflate(R.layout.fragment_receive)

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        onViewReady()
        setupLayout()
        setCustomKeypad()

        scrollview.post { scrollview.scrollTo(0, 0) }
        presenter.onSelectDefault(defaultAccountPosition)
    }

    private fun setupToolbar() {
        if ((activity as AppCompatActivity).supportActionBar != null) {
            (activity as BaseAuthActivity).setupToolbar(
                    (activity as MainActivity).supportActionBar, R.string.receive_bitcoin)
        } else {
            finishPage()
        }
    }

    override fun startContactSelectionActivity() {
        AccountChooserActivity.startForResult(
                this,
                AccountChooserActivity.REQUEST_CODE_CHOOSE_CONTACT,
                PaymentRequestType.CONTACT,
                getString(R.string.from)
        )
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
        currencyCrypto.text = presenter.currencyHelper.btcUnit
        currencyFiat.text = presenter.currencyHelper.fiatUnit

        // QR Code
        image_qr.apply {
            setOnClickListener { showClipboardWarning() }
            setOnLongClickListener { consume { onShareClicked() } }
        }

        toAddressTextView.setOnClickListener {
            AccountChooserActivity.startForResult(
                    this,
                    AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_RECEIVE,
                    PaymentRequestType.REQUEST,
                    getString(R.string.to)
            )
        }

        toArrowImage.setOnClickListener {
            AccountChooserActivity.startForResult(
                    this,
                    AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_RECEIVE,
                    PaymentRequestType.REQUEST,
                    getString(R.string.to)
            )
        }

        textChangeSubject.debounce(300, TimeUnit.MILLISECONDS)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext { presenter.onBitcoinAmountChanged(getBtcAmount()) }
                .subscribe(IgnorableDefaultObserver())

        fromAddressTextView.setHint(R.string.contact_select)

        textview_whats_this.setOnClickListener {
            IntroducingContactsPromptDialog.newInstance().apply {
                setDismissButtonListener {
                    PrefsUtil(activity).setValue(PrefsUtil.KEY_CONTACTS_INTRODUCTION_COMPLETE, true)
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
            if (presenter.selectedContactId == null) {
                showToast(R.string.contact_select_first, ToastCustom.TYPE_ERROR)
            } else if (!presenter.isValidAmount(getBtcAmount())) {
                showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR)
            } else {
                listener?.onTransactionNotesRequested(
                        presenter.getConfirmationDetails(),
                        PaymentRequestType.REQUEST,
                        presenter.selectedContactId!!,
                        presenter.currencyHelper.getLongAmount(
                                amountCrypto.text.toString()),
                        presenter.getSelectedAccountPosition()
                )
            }
        }

        @Suppress("ConstantConditionIf")
        if (!BuildConfig.CONTACTS_ENABLED) {
            button_request.gone()
            from_container.gone()
            textview_whats_this.gone()
            divider4.gone()
        }

        tabs_receive.apply {
            addTab(tabs_receive.newTab().setText("BITCOIN"))
            addTab(tabs_receive.newTab().setText("ETHER"))
            setOnTabSelectedListener {
                if (it == 0) presenter.onSelectDefault(defaultAccountPosition) else presenter.onEthSelected()
            }
        }
    }

    private val btcTextWatcher = object : TextWatcher {
        override fun afterTextChanged(s: Editable?) {
            Timber.d("afterTextChanged")
            var editable = s
            amountCrypto.removeTextChangedListener(this)
            editable = EditTextFormatUtil.formatEditable(
                    editable,
                    presenter.currencyHelper.maxBtcDecimalLength,
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
            Timber.d("afterTextChanged")
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
        edittext_receiving_address.setText(address)
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
        closeKeypad()
        setupToolbar()
        LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, intentFilter)
    }

    override fun showQrLoading() {
        image_qr.invisible()
        edittext_receiving_address.invisible()
        progressbar.visible()
    }

    override fun showQrCode(bitmap: Bitmap?) {
        progressbar.invisible()
        image_qr.visible()
        edittext_receiving_address.visible()
        image_qr.setImageBitmap(bitmap)
    }

    override fun displayBitcoinLayout() {
        divider1.visible()
        amount_container.visible()
        divider_to.visible()
        to_container.visible()
        divider3.visible()

        @Suppress("ConstantConditionIf")
        if (BuildConfig.CONTACTS_ENABLED) {
            from_container.visible()
            textview_whats_this.visible()
            divider4.visible()
            button_request.visible()
        }
    }

    override fun hideBitcoinLayout() {
        if (custom_keyboard.isVisible) {
            custom_keyboard.hideKeyboard()
        }
        divider1.gone()
        amount_container.gone()
        divider_to.gone()
        to_container.gone()
        divider3.gone()

        @Suppress("ConstantConditionIf")
        if (BuildConfig.CONTACTS_ENABLED) {
            from_container.gone()
            textview_whats_this.gone()
            divider4.gone()
            button_request.gone()
        }
    }

    override fun updateReceiveLabel(label: String) {
        toAddressTextView.text = label
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Set receiving account
        if (resultCode == Activity.RESULT_OK
                && requestCode == AccountChooserActivity.REQUEST_CODE_CHOOSE_RECEIVING_ACCOUNT_FROM_RECEIVE
                && data != null) {

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
                presenter.onSelectDefault(defaultAccountPosition)
            } catch (e: IOException) {
                Timber.e(e)
                presenter.onSelectDefault(defaultAccountPosition)
            }

            // Choose contact for request
        } else if (resultCode == Activity.RESULT_OK
                && requestCode == AccountChooserActivity.REQUEST_CODE_CHOOSE_CONTACT
                && data != null) {

            try {
                val contact = ObjectMapper().readValue(
                        data.getStringExtra(EXTRA_SELECTED_ITEM),
                        Contact::class.java)
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

            bottomSheetDialog = BottomSheetDialog(context, R.style.BottomSheetDialog).apply {
                setContentView(sheetView)
            }

            adapter.notifyDataSetChanged()
        }

        bottomSheetDialog?.apply { show() }
    }

    private fun onShareClicked() {
        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_share)
                .setCancelable(false)
                .setPositiveButton(R.string.yes) { _, _ ->
                    if (ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        PermissionUtil.requestWriteStoragePermissionFromFragment(activity.findViewById(R.id.coordinator_layout), this)
                    } else {
                        presenter.onShowBottomSheetSelected()
                    }
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    override fun shouldShowRequestPermissionRationale(permission: String): Boolean {
        return permission == Manifest.permission.WRITE_EXTERNAL_STORAGE
                || super.shouldShowRequestPermissionRationale(permission)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == PermissionUtil.PERMISSION_REQUEST_WRITE_STORAGE) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                presenter.onShowBottomSheetSelected()
            } else {
                // Permission request was denied.
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    override fun showWatchOnlyWarning() {
        val dialogView = layoutInflater.inflate(R.layout.alert_watch_only_spend, null)
        val alertDialog = AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setView(dialogView.rootView)
                .setCancelable(false)
                .create()

        dialogView.confirm_cancel.setOnClickListener {
            presenter.onSelectDefault(defaultAccountPosition)
            presenter.setWarnWatchOnlySpend(!dialogView.confirm_dont_ask_again.isChecked)
            alertDialog.dismiss()
        }

        dialogView.confirm_continue.setOnClickListener {
            presenter.setWarnWatchOnlySpend(!dialogView.confirm_dont_ask_again.isChecked)
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    override fun getQrBitmap(): Bitmap = (image_qr.drawable as BitmapDrawable).bitmap

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        toast(message, toastType)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        menu?.clear()
        inflater?.inflate(R.menu.menu_receive, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item!!.itemId) {
        R.id.action_share -> consume { onShareClicked() }
        else -> super.onOptionsItemSelected(item)
    }

    fun getSelectedAccountPosition(): Int = presenter.getSelectedAccountPosition()

    fun onBackPressed() {
        handleBackPressed()
    }

    private fun showClipboardWarning() {
        AlertDialog.Builder(activity, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes) { _, _ ->
                    val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Send address", edittext_receiving_address.getTextString())
                    toast(R.string.copied_to_clipboard)
                    clipboard.primaryClip = clip
                }
                .setNegativeButton(R.string.no, null)
                .show()
    }

    private fun handleBackPressed() {
        if (isKeyboardVisible()) {
            closeKeypad()
        } else {
            if (backPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
                AccessState.getInstance().logout(context)
                return
            } else {
                onExitConfirmToast()
            }

            backPressed = System.currentTimeMillis()
        }
    }

    private fun onExitConfirmToast() {
        toast(R.string.exit_confirm)
    }

    override fun onPause() {
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(broadcastReceiver)
        super.onPause()
    }

    fun finishPage() {
        listener?.onReceiveFragmentClose()
    }

    private fun setCustomKeypad() {
        custom_keyboard.apply {
            setCallback(mvpView)
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
        }

        val height = activity.resources.getDimension(R.dimen.action_bar_height).toInt()
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
        // Hide bottom nav if applicable
        if (activity is MainActivity) {
            (activity as MainActivity).bottomNavigationView.hideBottomNavigation()
        }
    }

    override fun onKeypadOpenCompleted() {
        // Resize activity around view
        val height = activity.resources.getDimension(R.dimen.action_bar_height).toInt()
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

        private val ARG_SELECTED_ACCOUNT_POSITION = "selected_account_position"
        private val COOL_DOWN_MILLIS = 2 * 1000

        @JvmStatic
        fun newInstance(selectedAccountPosition: Int) = ReceiveFragment().apply {
            arguments = Bundle().apply {
                putInt(ARG_SELECTED_ACCOUNT_POSITION, selectedAccountPosition)
            }
        }
    }

}

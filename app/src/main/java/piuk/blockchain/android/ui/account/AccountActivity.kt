package piuk.blockchain.android.ui.account

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Rect
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AlertDialog
import android.support.v7.widget.AppCompatEditText
import android.support.v7.widget.LinearLayoutManager
import android.text.InputFilter
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.widget.CheckBox
import com.google.zxing.BarcodeFormat
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import com.karumi.dexter.listener.single.CompositePermissionListener
import com.karumi.dexter.listener.single.SnackbarOnDeniedPermissionListener
import info.blockchain.wallet.payload.data.LegacyAddress
import kotlinx.android.synthetic.main.activity_accounts.currency_header
import kotlinx.android.synthetic.main.activity_accounts.linear_layout_root
import kotlinx.android.synthetic.main.activity_accounts.recyclerview_accounts
import kotlinx.android.synthetic.main.toolbar_general.toolbar_general
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.account.AccountPresenter.Companion.ADDRESS_LABEL_MAX_LENGTH
import piuk.blockchain.android.ui.account.AccountPresenter.Companion.KEY_WARN_TRANSFER_ALL
import piuk.blockchain.android.ui.account.adapter.AccountAdapter
import piuk.blockchain.android.ui.account.adapter.AccountHeadersListener
import piuk.blockchain.android.ui.backup.transfer.ConfirmFundsTransferDialogFragment
import piuk.blockchain.android.ui.balance.BalanceFragment
import piuk.blockchain.android.ui.zxing.CaptureActivity
import piuk.blockchain.android.ui.zxing.Intents
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.toast
import timber.log.Timber
import java.util.*
import javax.inject.Inject

class AccountActivity : BaseMvpActivity<AccountView, AccountPresenter>(), AccountView,
    AccountHeadersListener {

    @Suppress("MemberVisibilityCanBePrivate")
    @Inject lateinit var accountPresenter: AccountPresenter

    override val locale: Locale = Locale.getDefault()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (BalanceFragment.ACTION_INTENT == intent.action) {
                onViewReady()
                // Check if we need to hide/show the transfer funds icon in the Toolbar
                presenter.checkTransferableLegacyFunds(false, false)
            }
        }
    }

    private var transferFundsMenuItem: MenuItem? = null
    private val accountsAdapter: AccountAdapter by unsafeLazy {
        AccountAdapter(this)
    }
    private var progress: MaterialProgressDialog? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)
        setupToolbar(toolbar_general, R.string.drawer_addresses)

        with(currency_header) {
            hideEthereum()
            setCurrentlySelectedCurrency(presenter.cryptoCurrency)
            setSelectionListener { presenter.cryptoCurrency = it }
        }

        with(recyclerview_accounts) {
            layoutManager = LinearLayoutManager(this@AccountActivity)
            itemAnimator = null
            setHasFixedSize(true)
            adapter = accountsAdapter
        }

        onViewReady()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.menu_account, menu)
        transferFundsMenuItem = menu.findItem(R.id.action_transfer_funds)
        // Auto popup
        presenter.checkTransferableLegacyFunds(true, true)

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        android.R.id.home -> consume { onBackPressed() }
        R.id.action_transfer_funds -> consume {
            showProgressDialog(R.string.please_wait)
            // Not auto popup
            presenter.checkTransferableLegacyFunds(false, true)
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        if (currency_header.isOpen()) {
            currency_header.close()
        } else {
            super.onBackPressed()
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            // Notify touchOutsideViewListeners if user tapped outside a given view
            if (currency_header != null) {
                val viewRect = Rect()
                currency_header.getGlobalVisibleRect(viewRect)
                if (!viewRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    if (currency_header.isOpen()) {
                        currency_header.close()
                        return false
                    }
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun startScanForResult() {
        Intent(this, CaptureActivity::class.java).apply {
            putExtra(Intents.Scan.FORMATS, EnumSet.allOf(BarcodeFormat::class.java))
            putExtra(Intents.Scan.MODE, Intents.Scan.QR_CODE_MODE)
        }.run { startActivityForResult(this, IMPORT_PRIVATE_REQUEST_CODE) }
    }

    override fun onCreateNewClicked() {
        createNewAccount()
    }

    override fun onImportAddressClicked() {
        importAddress()
    }

    override fun onAccountClicked(cryptoCurrency: CryptoCurrencies, correctedPosition: Int) {
        onRowClick(cryptoCurrency, correctedPosition)
    }

    private fun importAddress() {
        val deniedPermissionListener = SnackbarOnDeniedPermissionListener.Builder
                .with(linear_layout_root, R.string.request_camera_permission)
                .withButton(android.R.string.ok) { importAddress() }
                .build()

        val grantedPermissionListener = object : BasePermissionListener() {
            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                onScanButtonClicked()
            }
        }

        val compositePermissionListener =
                CompositePermissionListener(deniedPermissionListener, grantedPermissionListener)

        Dexter.withActivity(this)
                .withPermission(Manifest.permission.CAMERA)
                .withListener(compositePermissionListener)
                .withErrorListener { error -> Timber.wtf("Dexter permissions error $error") }
                .check()
    }

    private fun onRowClick(cryptoCurrency: CryptoCurrencies, position: Int) {
        AccountEditActivity.startForResult(
                this,
                getAccountPosition(cryptoCurrency, position),
                if (position >= presenter.accountSize) position - presenter.accountSize else -1,
                cryptoCurrency,
                EDIT_ACTIVITY_REQUEST_CODE
        )
    }

    private fun getAccountPosition(cryptoCurrency: CryptoCurrencies, position: Int): Int =
            if (cryptoCurrency == CryptoCurrencies.BTC) {
                if (position < presenter.accountSize) position else -1
            } else {
                position
            }

    private fun onScanButtonClicked() {
        SecondPasswordHandler(this).validate(object : SecondPasswordHandler.ResultListener {
            override fun onNoSecondPassword() {
                presenter.onScanButtonClicked()
            }

            override fun onSecondPasswordValidated(validateSecondPassword: String) {
                presenter.doubleEncryptionPassword = validateSecondPassword
                presenter.onScanButtonClicked()
            }
        })
    }

    private fun createNewAccount() {
        SecondPasswordHandler(this).validate(object : SecondPasswordHandler.ResultListener {
            override fun onNoSecondPassword() {
                promptForAccountLabel()
            }

            override fun onSecondPasswordValidated(validateSecondPassword: String) {
                presenter.doubleEncryptionPassword = validateSecondPassword
                promptForAccountLabel()
            }
        })
    }

    private fun promptForAccountLabel() {
        val editText = AppCompatEditText(this).apply {
            inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH))
            setHint(R.string.name)
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.label)
                .setMessage(R.string.assign_display_name)
                .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
                .setCancelable(false)
                .setPositiveButton(R.string.save_name) { _, _ ->
                    if (!editText.getTextString().trim { it <= ' ' }.isEmpty()) {
                        addAccount(editText.getTextString().trim { it <= ' ' })
                    } else {
                        toast(R.string.label_cant_be_empty, ToastCustom.TYPE_ERROR)
                    }
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    private fun addAccount(accountLabel: String) {
        presenter.createNewAccount(accountLabel)
    }

    override fun updateAccountList(displayAccounts: List<AccountItem>) {
        accountsAdapter.items = displayAccounts
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(BalanceFragment.ACTION_INTENT)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
        onViewReady()
    }

    override fun onPause() {
        super.onPause()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK
            && requestCode == IMPORT_PRIVATE_REQUEST_CODE
            && data != null && data.getStringExtra(CaptureActivity.SCAN_RESULT) != null
        ) {

            val strResult = data.getStringExtra(CaptureActivity.SCAN_RESULT)
            presenter.onAddressScanned(strResult)
            setResult(resultCode)
        } else if (resultCode == Activity.RESULT_OK && requestCode == EDIT_ACTIVITY_REQUEST_CODE) {
            onViewReady()
            setResult(resultCode)
        }
    }

    override fun showBip38PasswordDialog(data: String) {
        val password = AppCompatEditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.bip38_password_entry)
                .setView(ViewUtils.getAlertDialogPaddedView(this, password))
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    presenter.importBip38Address(data, password.getTextString())
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    override fun showWatchOnlyWarningDialog(address: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.warning)
                .setCancelable(false)
                .setMessage(getString(R.string.watch_only_import_warning))
                .setPositiveButton(R.string.dialog_continue) { _, _ ->
                    presenter.confirmImportWatchOnly(address)
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
    }

    override fun showRenameImportedAddressDialog(address: LegacyAddress) {
        val editText = AppCompatEditText(this).apply {
            inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            filters = arrayOf<InputFilter>(InputFilter.LengthFilter(ADDRESS_LABEL_MAX_LENGTH))
            setHint(R.string.name)
        }

        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.label_address)
                .setView(ViewUtils.getAlertDialogPaddedView(this, editText))
                .setCancelable(false)
                .setPositiveButton(R.string.save_name) { _, _ ->
                    val label = editText.getTextString()
                    if (!label.trim { it <= ' ' }.isEmpty()) {
                        address.label = label
                    }

                    remoteSaveNewAddress(address)
                }
                .setNegativeButton(R.string.polite_no) { _, _ -> remoteSaveNewAddress(address) }
                .show()
    }

    override fun showToast(@StringRes message: Int, @ToastCustom.ToastType toastType: String) {
        toast(message, toastType)
    }

    override fun broadcastIntent(intent: Intent) {
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    private fun remoteSaveNewAddress(legacy: LegacyAddress) {
        presenter.updateLegacyAddress(legacy)
    }

    override fun onShowTransferableLegacyFundsWarning(isAutoPopup: Boolean) {
        val checkBox = CheckBox(this)
        checkBox.isChecked = false
        checkBox.setText(R.string.dont_ask_again)

        val builder = AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.transfer_funds)
                .setMessage(getString(R.string.transfer_recommend) + "\n")
                .setPositiveButton(R.string.transfer) { _, _ ->
                    transferSpendableFunds()
                    if (checkBox.isChecked) {
                        prefsUtil.setValue(KEY_WARN_TRANSFER_ALL, false)
                    }
                }
                .setNegativeButton(R.string.not_now) { _, _ ->
                    if (checkBox.isChecked) {
                        prefsUtil.setValue(KEY_WARN_TRANSFER_ALL, false)
                    }
                }

        if (isAutoPopup) {
            builder.setView(ViewUtils.getAlertDialogPaddedView(this, checkBox))
        }

        val alertDialog = builder.create()
        if (!isFinishing) {
            alertDialog.show()
        }

        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).apply {
            setTextColor(ContextCompat.getColor(this@AccountActivity, R.color.primary_gray_dark))
        }
    }

    @SuppressLint("CommitTransaction")
    private fun transferSpendableFunds() {
        ConfirmFundsTransferDialogFragment.newInstance()
                .show(supportFragmentManager, ConfirmFundsTransferDialogFragment.TAG)
    }

    override fun onSetTransferLegacyFundsMenuItemVisible(visible: Boolean) {
        transferFundsMenuItem?.isVisible = visible
    }

    override fun showProgressDialog(@StringRes message: Int) {
        dismissProgressDialog()
        if (!isFinishing) {
            progress = MaterialProgressDialog(this).apply {
                setMessage(message)
                setCancelable(false)
                show()
            }
        }
    }

    override fun dismissProgressDialog() {
        if (progress?.isShowing == true) {
            progress!!.dismiss()
            progress = null
        }
    }

    override fun hideCurrencyHeader() {
        currency_header?.apply {
            gone()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        dismissProgressDialog()
    }

    override fun createPresenter() = accountPresenter

    override fun getView() = this

    companion object {

        private const val IMPORT_PRIVATE_REQUEST_CODE = 2006
        private const val EDIT_ACTIVITY_REQUEST_CODE = 2007

    }

}

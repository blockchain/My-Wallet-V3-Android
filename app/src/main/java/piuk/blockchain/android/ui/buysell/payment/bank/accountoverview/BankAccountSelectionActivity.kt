package piuk.blockchain.android.ui.buysell.payment.bank.accountoverview

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.buysell.confirmation.sell.CoinifySellConfirmationActivity
import piuk.blockchain.android.ui.buysell.createorder.models.SellConfirmationDisplayModel
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.adapter.BankAccountSelectionAdapter
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.adapter.BankAccountSelectionListener
import piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.models.BankAccountState
import piuk.blockchain.android.ui.buysell.payment.bank.addaccount.AddBankAccountActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.toast
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_bank_account_selection.button_bank_selection_retry as buttonRetry
import kotlinx.android.synthetic.main.activity_bank_account_selection.recycler_view_bank_accounts as recyclerView
import kotlinx.android.synthetic.main.activity_bank_account_selection.text_view_account_description as textViewDescription
import kotlinx.android.synthetic.main.activity_bank_account_selection.text_view_account_load_failure as textViewFailureMessage
import kotlinx.android.synthetic.main.activity_bank_account_selection.tool_bar_bank_account_selection as toolBar

class BankAccountSelectionActivity :
    BaseMvpActivity<BankAccountSelectionView, BankAccountSelectionPresenter>(),
    BankAccountSelectionView,
    BankAccountSelectionListener {

    @Inject
    lateinit var presenter: BankAccountSelectionPresenter
    private var progressDialog: MaterialProgressDialog? = null
    private val dataViews by unsafeLazy { listOf(recyclerView, textViewDescription) }
    private val failureViews by unsafeLazy { listOf(textViewFailureMessage, buttonRetry) }
    private val accountAdapter by unsafeLazy { BankAccountSelectionAdapter(this) }
    private val displayModel by unsafeLazy {
        intent.getParcelableExtra(EXTRA_DISPLAY_MODEL) as SellConfirmationDisplayModel
    }
    private var actionSelected = false

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank_account_selection)
        setupToolbar(toolBar as Toolbar, R.string.buy_sell_account_selection_title)

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = accountAdapter

        buttonRetry.setOnClickListener { onViewReady() }

        onViewReady()
    }

    override fun renderUiState(uiState: BankAccountState) {
        when (uiState) {
            BankAccountState.Loading -> showProgressDialog()
            BankAccountState.Failure -> handleUiVisibility(true)
            is BankAccountState.Data -> {
                handleUiVisibility(false)
                accountAdapter.items = uiState.displayData
            }
            BankAccountState.DeleteAccountFailure ->
                toast(R.string.buy_sell_account_selection_delete_failure, ToastCustom.TYPE_ERROR)
        }

        if (uiState !== BankAccountState.Loading) dismissProgressDialog()
    }

    private fun handleUiVisibility(shouldShowError: Boolean) {
        dataViews.forEach { it.goneIf { shouldShowError } }
        failureViews.forEach { it.goneIf { !shouldShowError } }
    }

    private fun showProgressDialog() {
        dismissProgressDialog()
        if (!isFinishing) {
            progressDialog = MaterialProgressDialog(this).apply {
                setMessage(getString(R.string.please_wait))
                setCancelable(false)
                show()
            }
        }
    }

    private fun dismissProgressDialog() {
        if (progressDialog?.isShowing == true) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    override fun onBankAccountSelected(bankAccountId: Int) {
        if (!actionSelected) {
            actionSelected = true
            CoinifySellConfirmationActivity.start(this, displayModel, bankAccountId)
            finish()
        }
    }

    override fun onAddAccountSelected() {
        if (!actionSelected) {
            actionSelected = true
            AddBankAccountActivity.start(this, displayModel)
            finish()
        }
    }

    override fun onBankAccountLongPressed(bankAccountId: Int) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.buy_sell_account_selection_delete_title)
            .setMessage(R.string.buy_sell_account_selection_delete_message)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                presenter.deleteBankAccount(bankAccountId)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    override fun createPresenter(): BankAccountSelectionPresenter = presenter

    override fun getView(): BankAccountSelectionView = this

    companion object {

        private const val EXTRA_DISPLAY_MODEL =
            "piuk.blockchain.android.ui.buysell.payment.bank.accountoverview.EXTRA_DISPLAY_MODEL"

        fun startForResult(
            activity: Activity,
            displayModel: SellConfirmationDisplayModel,
            requestCode: Int
        ) {
            Intent(activity, BankAccountSelectionActivity::class.java)
                .putExtra(EXTRA_DISPLAY_MODEL, displayModel)
                .run { activity.startActivityForResult(this, requestCode) }
        }
    }
}

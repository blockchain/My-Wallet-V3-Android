package piuk.blockchain.android.ui.buysell.payment.bank.addaccount

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.buysell.createorder.models.SellConfirmationDisplayModel
import piuk.blockchain.android.ui.buysell.payment.bank.addaddress.AddAddressActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.toast
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_add_bank_account.button_confirm as buttonConfirm
import kotlinx.android.synthetic.main.activity_add_bank_account.edit_text_bic as editTextBic
import kotlinx.android.synthetic.main.activity_add_bank_account.edit_text_iban as editTextIban
import kotlinx.android.synthetic.main.toolbar_general.toolbar_general as toolBar

class AddBankAccountActivity : BaseMvpActivity<AddBankAccountView, AddBankAccountPresenter>(),
    AddBankAccountView {

    @Inject lateinit var presenter: AddBankAccountPresenter
    override val iban: String
        get() = editTextIban.getTextString()
    override val bic: String
        get() = editTextBic.getTextString()
    private val displayModel by unsafeLazy { intent.getParcelableExtra(EXTRA_DISPLAY_MODEL) as SellConfirmationDisplayModel }

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_bank_account)
        setupToolbar(toolBar, R.string.buy_sell_add_account_title)

        buttonConfirm.setOnClickListener { presenter.onConfirmClicked() }

        editTextIban.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                consume { editTextBic.requestFocus() }
            } else false
        }

        editTextBic.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                consume {
                    ViewUtils.hideKeyboard(this)
                    presenter.onConfirmClicked()
                }
            } else false
        }

        onViewReady()
    }

    override fun goToAddAddress(iban: String, bic: String) {
        AddAddressActivity.start(this, iban, bic, displayModel)
        finish()
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun createPresenter(): AddBankAccountPresenter = presenter

    override fun getView(): AddBankAccountView = this

    companion object {

        private const val EXTRA_DISPLAY_MODEL =
                "piuk.blockchain.android.ui.buysell.payment.bank.addaccount.accountoverview.EXTRA_DISPLAY_MODEL"

        fun start(
                activity: Activity,
                displayModel: SellConfirmationDisplayModel
        ) {
            Intent(activity, AddBankAccountActivity::class.java)
                    .putExtra(EXTRA_DISPLAY_MODEL, displayModel)
                    .addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    .run { activity.startActivity(this) }
        }

        fun startForResult(
                activity: Activity,
                displayModel: SellConfirmationDisplayModel,
                requestCode: Int
        ) {
            Intent(activity, AddBankAccountActivity::class.java)
                    .putExtra(EXTRA_DISPLAY_MODEL, displayModel)
                    .run { activity.startActivityForResult(this, requestCode) }
        }

    }
}
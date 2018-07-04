package piuk.blockchain.android.ui.buysell.payment.bank.addaddress

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.inputmethod.EditorInfo
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.buysell.confirmation.sell.CoinifySellConfirmationActivity
import piuk.blockchain.android.ui.buysell.createorder.models.SellConfirmationDisplayModel
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.ViewUtils
import piuk.blockchain.androidcoreui.utils.extensions.getTextString
import piuk.blockchain.androidcoreui.utils.extensions.toast
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_add_address.button_confirm as buttonConfirm
import kotlinx.android.synthetic.main.activity_add_address.constraint_layout_add_address as constraintLayoutRoot
import kotlinx.android.synthetic.main.activity_add_address.edit_text_city as editTextCity
import kotlinx.android.synthetic.main.activity_add_address.edit_text_country as editTextCountry
import kotlinx.android.synthetic.main.activity_add_address.edit_text_name as editTextName
import kotlinx.android.synthetic.main.activity_add_address.edit_text_street_name as editTextStreet
import kotlinx.android.synthetic.main.activity_add_address.edit_text_postcode as editTextPostCode
import kotlinx.android.synthetic.main.activity_add_address.text_input_layout_country as inputLayoutCountry
import kotlinx.android.synthetic.main.toolbar_general.toolbar_general as toolBar

class AddAddressActivity : BaseMvpActivity<AddAddressView, AddAddressPresenter>(), AddAddressView {

    @Inject lateinit var presenter: AddAddressPresenter
    override val iban: String by unsafeLazy { intent.getStringExtra(EXTRA_IBAN) }
    override val bic: String by unsafeLazy { intent.getStringExtra(EXTRA_BIC) }
    private val displayModel by unsafeLazy { intent.getParcelableExtra(EXTRA_DISPLAY_MODEL) as SellConfirmationDisplayModel }
    override val accountHolderName: String
        get() = editTextName.getTextString()
    override val streetAndNumber: String
        get() = editTextStreet.getTextString()
    override val city: String
        get() = editTextCity.getTextString()
    override val postCode: String
        get() = editTextPostCode.getTextString()
    private var progressDialog: MaterialProgressDialog? = null

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_address)
        setupToolbar(toolBar, R.string.buy_sell_add_account_title)

        buttonConfirm.setOnClickListener { presenter.onConfirmClicked() }

        editTextCountry.setOnClickListener {
            ViewUtils.hideKeyboard(this)

            CountryDialog(this, object : CountryDialog.CountryCodeSelectionListener {
                override fun onCountryCodeSelected(code: String) {
                    presenter.onCountryCodeChanged(code)
                }
            }).show()
        }

        editTextName.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                consume { editTextStreet.requestFocus() }
            } else false
        }

        editTextStreet.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                consume { editTextCity.requestFocus() }
            } else false
        }

        editTextCity.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                consume { editTextPostCode.requestFocus() }
            } else false
        }

        editTextPostCode.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                consume { ViewUtils.hideKeyboard(this) }
            } else false
        }

        onViewReady()
    }

    override fun showCountrySelected(country: String) {
        editTextCountry.setText(country)
    }

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun goToConfirmation(bankAccountId: Int) {
        CoinifySellConfirmationActivity.start(this, displayModel, bankAccountId)
        finish()
    }

    override fun showErrorDialog(errorDescription: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(errorDescription)
                .setPositiveButton(android.R.string.ok, null)
                .show()
    }

    override fun showProgressDialog() {
        if (!isFinishing) {
            dismissProgressDialog()
            progressDialog = MaterialProgressDialog(this).apply {
                setMessage(getString(R.string.please_wait))
                setCancelable(false)
                show()
            }
        }
    }

    override fun dismissProgressDialog() {
        if (progressDialog?.isShowing == true) {
            progressDialog!!.dismiss()
            progressDialog = null
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    override fun createPresenter(): AddAddressPresenter = presenter

    override fun getView(): AddAddressView = this

    companion object {

        private const val EXTRA_IBAN =
                "piuk.blockchain.android.ui.buysell.payment.bank.addaddress.EXTRA_IBAN"
        private const val EXTRA_BIC =
                "piuk.blockchain.android.ui.buysell.payment.bank.addaddress.EXTRA_BIC"
        private const val EXTRA_DISPLAY_MODEL =
                "piuk.blockchain.android.ui.buysell.payment.bank.addaddress.EXTRA_DISPLAY_MODEL"

        fun start(
                activity: Activity,
                iban: String,
                bic: String,
                displayModel: SellConfirmationDisplayModel
        ) {
            Intent(activity, AddAddressActivity::class.java)
                    .apply {
                        putExtra(EXTRA_IBAN, iban)
                        putExtra(EXTRA_BIC, bic)
                        putExtra(EXTRA_DISPLAY_MODEL, displayModel)
                        addFlags(Intent.FLAG_ACTIVITY_FORWARD_RESULT)
                    }
                    .run { activity.startActivity(this) }
        }

    }
}
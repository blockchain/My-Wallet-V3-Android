package piuk.blockchain.android.ui.shapeshift.confirmation

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import kotlinx.android.synthetic.main.activity_confirmation_shapeshift.*
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import com.blockchain.ui.password.SecondPasswordHandler
import piuk.blockchain.android.ui.shapeshift.inprogress.TradeInProgressActivity
import piuk.blockchain.android.ui.shapeshift.models.ShapeShiftData
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.extensions.toast
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class ShapeShiftConfirmationActivity :
    BaseMvpActivity<ShapeShiftConfirmationView, ShapeShiftConfirmationPresenter>(),
    ShapeShiftConfirmationView,
    SecondPasswordHandler.ResultListener {

    override val locale: Locale = Locale.getDefault()

    @Suppress("MemberVisibilityCanPrivate", "unused")
    @Inject
    lateinit var confirmationPresenter: ShapeShiftConfirmationPresenter

    override val shapeShiftData: ShapeShiftData by unsafeLazy {
        intent.getParcelableExtra<ShapeShiftData>(EXTRA_SHAPESHIFT_DATA)
    }

    private var progressDialog: MaterialProgressDialog? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirmation_shapeshift)
        setupToolbar(toolbar_general, R.string.confirm)

        checkbox_terms.setOnClickListener { presenter.onAcceptTermsClicked() }
        textView_terms_conditions.setOnClickListener { openShapeShiftTerms() }
        button_confirm.setOnClickListener { presenter.onConfirmClicked() }

        val stringFirstPart = getString(R.string.morph_confirmation_agree)
        val stringSecondPart = getString(R.string.morph_confirmation_terms)
        val terms = SpannableString(stringFirstPart + stringSecondPart)
        textView_terms_conditions.text = terms.apply {
            setSpan(
                ForegroundColorSpan(
                    ContextCompat.getColor(
                        this@ShapeShiftConfirmationActivity,
                        R.color.primary_blue_accent
                    )
                ),
                stringFirstPart.length,
                terms.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        onViewReady()
    }

    override fun onSupportNavigateUp() =
        consume { onBackPressed() }

    override fun showProgressDialog(@StringRes message: Int) {
        dismissProgressDialog()
        progressDialog = MaterialProgressDialog(
            this
        ).apply {
            setCancelable(false)
            setMessage(message)
            if (!isFinishing) show()
        }
    }

    override fun dismissProgressDialog() {
        progressDialog?.apply {
            if (!isFinishing) dismiss()
            progressDialog = null
        }
    }

    override fun setButtonState(enabled: Boolean) {
        button_confirm.isEnabled = enabled
    }

    override fun updateCounter(timeRemaining: String) {
        textview_time_remaining.text = getString(R.string.morph_time_remaining, timeRemaining)
    }

    override fun updateDeposit(label: String, amount: String) {
        textview_deposit_title.text = label
        textview_deposit_amount.text = amount
    }

    override fun updateReceive(label: String, amount: String) {
        textview_receive_title.text = label
        textview_receive_amount.text = amount
    }

    override fun updateTotalAmount(label: String, amount: String) {
        textview_total_spent_title.text = label
        textview_total_spent_amount.text = amount
    }

    override fun updateExchangeRate(exchangeRate: String) {
        textview_rate_value.text = exchangeRate
    }

    override fun updateTransactionFee(displayString: String) {
        textview_transaction_fee_amount.text = displayString
    }

    override fun updateNetworkFee(displayString: String) {
        textview_network_fee_amount.text = displayString
    }

    override fun showSecondPasswordDialog() {
        secondPasswordHandler.validate(this)
    }

    override fun showTimeExpiring() {
        textview_time_remaining.setTextColor(
            ContextCompat.getColor(this, R.color.product_red_medium)
        )
    }

    override fun showQuoteExpiredDialog() {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(R.string.morph_quote_expired_error_message)
            .setPositiveButton(android.R.string.ok) { _, _ -> finishPage() }
            .setCancelable(false)
            .show()
    }

    override fun launchProgressPage(depositAddress: String) {
        TradeInProgressActivity.start(this, depositAddress)
    }

    override fun onNoSecondPassword() {
        throw IllegalStateException("No Second Password callback triggered, but this shouldn't be possible")
    }

    override fun onSecondPasswordValidated(validatedSecondPassword: String) {
        presenter.onSecondPasswordVerified(validatedSecondPassword)
    }

    override fun showToast(message: Int, toastType: String) = toast(message, toastType)

    override fun finishPage() = finish()

    override fun createPresenter() = confirmationPresenter

    override fun getView() = this

    private fun openShapeShiftTerms() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(SHAPESHIFT_TERMS_LINK)))
        } catch (e: ActivityNotFoundException) {
            Timber.e(e)
        }
    }

    companion object {

        private const val SHAPESHIFT_TERMS_LINK =
            "https://info.shapeshift.io/sites/default/files/ShapeShift_Terms_Conditions%20v1.1.pdf"
        private const val EXTRA_SHAPESHIFT_DATA = "piuk.blockchain.android.EXTRA_SHAPESHIFT_DATA"

        @JvmStatic
        fun start(context: Context, shapeShiftData: ShapeShiftData) {
            val intent = Intent(context, ShapeShiftConfirmationActivity::class.java).apply {
                putExtra(EXTRA_SHAPESHIFT_DATA, shapeShiftData)
                // Do not store this in the back stack
                flags = Intent.FLAG_ACTIVITY_NO_HISTORY
            }

            context.startActivity(intent)
        }
    }
}
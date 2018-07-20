package piuk.blockchain.android.ui.buysell.confirmation.buy

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.view.View
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.rxkotlin.subscribeBy
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.buysell.createorder.models.BuyConfirmationDisplayModel
import piuk.blockchain.android.ui.buysell.createorder.models.OrderType
import piuk.blockchain.android.ui.buysell.details.awaitingtransfer.CoinifyAwaitingBankTransferActivity
import piuk.blockchain.android.ui.buysell.details.models.AwaitingFundsModel
import piuk.blockchain.android.ui.buysell.payment.card.ISignThisActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.visible
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_coinify_confirmation.button_bank as buttonBank
import kotlinx.android.synthetic.main.activity_coinify_confirmation.button_card as buttonCard
import kotlinx.android.synthetic.main.activity_coinify_confirmation.button_confirm as buttonConfirm
import kotlinx.android.synthetic.main.activity_coinify_confirmation.check_box_rate_disclaimer as checkBoxDisclaimer
import kotlinx.android.synthetic.main.activity_coinify_confirmation.text_view_btc_to_be_received_detail as textViewToBeReceivedDetail
import kotlinx.android.synthetic.main.activity_coinify_confirmation.text_view_payment_fee_detail as textViewSendFeeDetail
import kotlinx.android.synthetic.main.activity_coinify_confirmation.text_view_receive_amount_detail as textViewReceiveDetail
import kotlinx.android.synthetic.main.activity_coinify_confirmation.text_view_send_amount_detail as textViewSendAmountDetail
import kotlinx.android.synthetic.main.activity_coinify_confirmation.text_view_time_remaining as textViewTimeRemaining
import kotlinx.android.synthetic.main.activity_coinify_confirmation.text_view_total_cost_detail as textViewTotalCostDetail
import kotlinx.android.synthetic.main.activity_coinify_confirmation.text_view_transaction_fee_detail as textViewReceiveFeeDetail
import kotlinx.android.synthetic.main.toolbar_general.toolbar_general as toolbar

class CoinifyBuyConfirmationActivity :
    BaseMvpActivity<CoinifyBuyConfirmationView, CoinifyBuyConfirmationPresenter>(),
    CoinifyBuyConfirmationView {

    @Inject
    lateinit var presenter: CoinifyBuyConfirmationPresenter
    override val locale: Locale = Locale.getDefault()
    override val orderType by unsafeLazy { intent.getSerializableExtra(EXTRA_ORDER_TYPE) as OrderType }
    override val displayableQuote by unsafeLazy {
        intent.getParcelableExtra(EXTRA_DISPLAY_MODEL) as BuyConfirmationDisplayModel
    }
    private var progressDialog: MaterialProgressDialog? = null
    private val methodSelectionViews by unsafeLazy {
        listOf(buttonCard, buttonBank)
    }
    private val confirmationViews by unsafeLazy {
        listOf(buttonConfirm, textViewTimeRemaining)
    }

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coinify_confirmation)

        when (orderType) {
            OrderType.Buy -> R.string.buy_sell_confirmation_title_preview_buy
            OrderType.BuyCard, OrderType.BuyBank -> R.string.buy_sell_confirmation_title_buy
            OrderType.Sell -> throw IllegalArgumentException("$orderType not supported on this page")
        }.run { setupToolbar(toolbar, this) }

        renderUi()

        RxView.clicks(buttonConfirm)
            .throttleFirst(500, TimeUnit.MILLISECONDS)
            .subscribeBy(onNext = { presenter.onConfirmClicked() })

        RxView.clicks(buttonCard)
            .throttleFirst(500, TimeUnit.MILLISECONDS)
            .subscribeBy(onNext = { presenter.onCardClicked() })

        RxView.clicks(buttonBank)
            .throttleFirst(500, TimeUnit.MILLISECONDS)
            .subscribeBy(onNext = { presenter.onBankClicked() })

        onViewReady()
    }

    @SuppressLint("SetTextI18n")
    private fun renderUi() {
        with(displayableQuote) {
            val currencyIn = currencyToReceive.toUpperCase()
            textViewReceiveDetail.text = "$amountToReceive $currencyIn"
            textViewReceiveFeeDetail.text = "$orderFee $currencyIn"
            textViewToBeReceivedDetail.text = "$totalAmountToReceiveFormatted $currencyIn"
            textViewSendAmountDetail.text = amountToSend
            textViewSendFeeDetail.text = paymentFee
            textViewTotalCostDetail.text = totalCostFormatted
        }

        confirmationViews.forEach { it.goneIf { view.orderType == OrderType.Buy } }
        methodSelectionViews.forEach { it.goneIf { view.orderType != OrderType.Buy } }

        if (view.orderType == OrderType.BuyBank) {
            buttonConfirm.setText(R.string.submit)
            buttonConfirm.isEnabled = false
            checkBoxDisclaimer.visible()
            checkBoxDisclaimer.setOnCheckedChangeListener { _, isChecked ->
                buttonConfirm.isEnabled = isChecked
            }
        } else {
            presenter.startCountdownTimer()
            checkBoxDisclaimer.gone()
        }
    }

    override fun updateCounter(timeRemaining: String) {
        textViewTimeRemaining.text = getString(R.string.shapeshift_time_remaining, timeRemaining)
    }

    override fun showTimeExpiring() {
        textViewTimeRemaining.setTextColor(getResolvedColor(R.color.product_red_medium))
    }

    override fun showQuoteExpiredDialog() {
        if (textViewTimeRemaining.visibility == View.VISIBLE) {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.buy_sell_confirmation_order_expired)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    setResult(Activity.RESULT_CANCELED)
                    finish()
                }
                .setCancelable(false)
                .show()
        }
    }

    override fun showOverCardLimitDialog(localisedCardLimit: String, cardLimit: Double) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.buy_sell_confirmation_amount_too_high_title)
            .setMessage(
                getString(
                    R.string.buy_sell_confirmation_amount_too_high_message,
                    localisedCardLimit
                )
            )
            .setPositiveButton(R.string.buy_sell_confirmation_amount_too_high_use_transfer) { _, _ ->
                presenter.onBankClicked()
            }
            .setNegativeButton(R.string.buy_sell_confirmation_amount_too_high_use_card) { _, _ ->
                setResult(
                    Activity.RESULT_CANCELED,
                    Intent().apply { putExtra(EXTRA_CARD_LIMIT, cardLimit) }
                )
                finish()
            }
            .setCancelable(false)
            .show()
    }

    override fun launchCardConfirmation() {
        startForResult(
            this,
            REQUEST_CODE_CONFIRM_BUY_ORDER,
            OrderType.BuyCard,
            displayableQuote
        )
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun launchBankConfirmation() {
        startForResult(
            this,
            REQUEST_CODE_CONFIRM_BUY_ORDER,
            OrderType.BuyBank,
            displayableQuote
        )
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun launchTransferDetailsPage(tradeId: Int, awaitingFundsModel: AwaitingFundsModel) {
        CoinifyAwaitingBankTransferActivity.start(this, awaitingFundsModel)
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun launchCardPaymentWebView(
        redirectUrl: String,
        paymentId: String,
        fromCurrency: String,
        cost: Double
    ) {
        ISignThisActivity.start(this, redirectUrl, paymentId, fromCurrency, cost)
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun showErrorDialog(errorMessage: String) {
        AlertDialog.Builder(this, R.style.AlertDialogStyle)
            .setTitle(R.string.app_name)
            .setMessage(errorMessage)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    override fun displayProgressDialog() {
        if (!isFinishing) {
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

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    override fun onBackPressed() {
        // Allow user to go back without clearing previous activity so that they can make changes
        setResult(Activity.RESULT_CANCELED)
        super.onBackPressed()
    }

    override fun createPresenter(): CoinifyBuyConfirmationPresenter = presenter

    override fun getView(): CoinifyBuyConfirmationView = this

    companion object {

        private const val EXTRA_ORDER_TYPE =
            "piuk.blockchain.android.ui.buysell.confirmation.EXTRA_ORDER_TYPE"
        private const val EXTRA_DISPLAY_MODEL =
            "piuk.blockchain.android.ui.buysell.confirmation.EXTRA_DISPLAY_MODEL"
        const val EXTRA_CARD_LIMIT =
            "piuk.blockchain.android.ui.buysell.confirmation.EXTRA_CARD_LIMIT"

        const val REQUEST_CODE_CONFIRM_BUY_ORDER = 803

        fun startForResult(
            activity: Activity,
            requestCode: Int,
            orderType: OrderType,
            displayModel: BuyConfirmationDisplayModel
        ) {
            Intent(activity, CoinifyBuyConfirmationActivity::class.java)
                .apply { putExtra(EXTRA_ORDER_TYPE, orderType) }
                .apply { putExtra(EXTRA_DISPLAY_MODEL, displayModel) }
                .run { activity.startActivityForResult(this, requestCode) }
        }
    }
}
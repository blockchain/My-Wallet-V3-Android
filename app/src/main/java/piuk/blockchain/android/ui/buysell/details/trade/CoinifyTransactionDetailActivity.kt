package piuk.blockchain.android.ui.buysell.details.trade

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import kotlinx.android.synthetic.main.toolbar_general.*
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.android.ui.buysell.details.models.BuySellDetailsModel
import piuk.blockchain.android.ui.buysell.payment.card.ISignThisActivity
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.toast
import piuk.blockchain.androidcoreui.utils.extensions.visible
import javax.inject.Inject
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.button_finish_payment as buttonFinishPayment
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_amount_text as textViewAmountDetail
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_amount_title as textViewAmountTitle
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_bank_disclaimer as textViewBankDisclaimer
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_currency_received_text as textViewCurrencyReceivedDetail
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_currency_received_title as textViewCurrencyReceivedTitle
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_order_amount as textViewOrderAmountHeader
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_payment_fee_text as textViewPaymentFeeDetail
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_payment_fee_title as textViewPaymentFeeTitle
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_total_text as textViewTotalDetail
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_total_title as textViewTotalTitle
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_trade_id_text as textViewTradeId
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.text_view_transaction_date as textViewDateHeader
import kotlinx.android.synthetic.main.activity_coinify_transaction_detail.view_divider_total as dividerTotal

class CoinifyTransactionDetailActivity :
    BaseMvpActivity<CoinifyTransactionDetailView, CoinifyTransactionDetailPresenter>(),
    CoinifyTransactionDetailView {

    @Inject
    lateinit var presenter: CoinifyTransactionDetailPresenter
    override val orderDetails by unsafeLazy { intent.getParcelableExtra(EXTRA_DETAILS_MODEL) as BuySellDetailsModel }
    private var progressDialog: MaterialProgressDialog? = null

    init {
        Injector.INSTANCE.presenterComponent.inject(this)
    }

    private val bankSellInProgressViewsToShow by unsafeLazy {
        listOf<View>(textViewBankDisclaimer)
    }
    private val bankSellInProgressViewsToHide by unsafeLazy {
        listOf<View>(
            dividerTotal,
            textViewTotalTitle,
            textViewTotalDetail,
            textViewPaymentFeeTitle,
            textViewPaymentFeeDetail,
            textViewAmountTitle,
            textViewAmountDetail
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_coinify_transaction_detail)
        // Check Intent for validity
        require(intent.hasExtra(EXTRA_DETAILS_MODEL)) {
            "Intent does not contain BuySellDetailsModel. " +
                "Please start this Activity via the static factory method start()."
        }

        renderUi(orderDetails)

        onViewReady()
    }

    private fun renderUi(model: BuySellDetailsModel) {
        setupToolbar(toolbar_general, model.pageTitle)

        textViewOrderAmountHeader.text = model.headlineAmount
        textViewDateHeader.text = model.date
        textViewTradeId.text = model.tradeIdDisplay
        textViewCurrencyReceivedTitle.text = model.currencyReceivedTitle
        textViewCurrencyReceivedDetail.text = model.detailAmount
        textViewAmountDetail.text = model.amountSent
        textViewPaymentFeeDetail.text = model.paymentFee
        textViewTotalDetail.text = model.totalCost

        if (model.isSell) {
            bankSellInProgressViewsToShow.forEach { it.visible() }
            bankSellInProgressViewsToHide.forEach { it.gone() }
        }

        if (model.isAwaitingCardPayment) {
            buttonFinishPayment.visible()
            buttonFinishPayment.setOnClickListener { presenter.onFinishCardPayment() }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean =
        if (orderDetails.isAwaitingCardPayment) {
            consume { menuInflater.inflate(R.menu.menu_coinify_transaction_detail, menu) }
        } else true

    override fun onOptionsItemSelected(item: MenuItem?): Boolean = when (item?.itemId ?: -1) {
        R.id.action_cancel -> consume { presenter.cancelTrade(orderDetails.tradeId) }
        android.R.id.home -> consume { finish() }
        else -> false
    }

    override fun showProgressDialog() {
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

    override fun showToast(message: Int, toastType: String) {
        toast(message, toastType)
    }

    override fun finishPage() {
        finish()
    }

    override fun launchCardPayment(
        redirectUrl: String,
        paymentId: String,
        fromCurrency: String,
        cost: Double
    ) {
        ISignThisActivity.start(this, redirectUrl, paymentId, fromCurrency, cost)
        finish()
    }

    override fun createPresenter(): CoinifyTransactionDetailPresenter = presenter

    override fun getView(): CoinifyTransactionDetailView = this

    companion object {

        private const val EXTRA_DETAILS_MODEL =
            "piuk.blockchain.android.ui.buysell.details.EXTRA_DETAILS_MODEL"

        internal fun start(context: Context, buySellDetailsModel: BuySellDetailsModel) {
            Intent(context, CoinifyTransactionDetailActivity::class.java).apply {
                putExtra(EXTRA_DETAILS_MODEL, buySellDetailsModel)
            }.run { context.startActivity(this) }
        }
    }
}
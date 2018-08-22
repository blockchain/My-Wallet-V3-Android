package com.blockchain.morph.ui.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import android.widget.ImageView
import android.widget.TextView
import com.blockchain.morph.ui.R
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.extensions.toast
import java.util.Locale

class TradeDetailActivity : BaseMvpActivity<TradeDetailView, TradeDetailPresenter>(),
    TradeDetailView {

    override val locale: Locale = Locale.getDefault()

    private val tradeDetailPresenter: TradeDetailPresenter by inject()

    override val depositAddress: String by lazy { intent.getStringExtra(EXTRA_DEPOSIT_ADDRESS) }

    private var progressDialog: MaterialProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shapeshift_detail)
        setupToolbar(R.id.toolbar_general, R.string.morph_in_progress_title)

        onViewReady()
    }

    private val orderIdAmount: TextView get() = findViewById(R.id.textview_order_id_amount)
    private val transactionFeeAmount: TextView get() = findViewById(R.id.textview_transaction_fee_amount)
    private val exchangeRate: TextView get() = findViewById(R.id.textview_rate_value)
    private val receiveAmount: TextView get() = findViewById(R.id.textview_receive_amount)
    private val receiveTitle: TextView get() = findViewById(R.id.textview_receive_title)
    private val depositAmount: TextView get() = findViewById(R.id.textview_deposit_amount)
    private val depositTitle: TextView get() = findViewById(R.id.textview_deposit_title)
    private val progressImage: ImageView get() = findViewById(R.id.imageview_progress)
    private val currentStep: TextView get() = findViewById(R.id.textview_current_step)
    private val currentStage: TextView get() = findViewById(R.id.textview_current_stage)

    override fun onSupportNavigateUp() =
        consume { onBackPressed() }

    override fun updateUi(uiState: TradeDetailUiState) {
        setupToolbar(R.id.toolbar_general, uiState.title)
        currentStage.setText(uiState.heading)
        currentStep.text = uiState.message
        progressImage.setImageDrawable(ContextCompat.getDrawable(this, uiState.icon))
        receiveAmount.setTextColor(ContextCompat.getColor(this, uiState.receiveColor))
    }

    override fun updateDeposit(label: String, amount: String) {
        depositTitle.text = label
        depositAmount.text = amount
    }

    override fun updateReceive(label: String, amount: String) {
        receiveTitle.text = label
        receiveAmount.text = amount
    }

    override fun updateExchangeRate(exchangeRate: String) {
        this.exchangeRate.text = exchangeRate
    }

    override fun updateTransactionFee(displayString: String) {
        transactionFeeAmount.text = displayString
    }

    override fun updateOrderId(displayString: String) {
        orderIdAmount.text = displayString
        orderIdAmount.setOnClickListener {
            val clipboard =
                getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Send address", displayString)
            clipboard.primaryClip = clip
            toast(R.string.copied_to_clipboard)
        }
    }

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

    override fun showToast(message: Int, type: String) = toast(message, type)

    override fun finishPage() = finish()

    override fun createPresenter() = tradeDetailPresenter

    override fun getView() = this

    companion object {

        private const val EXTRA_DEPOSIT_ADDRESS = "piuk.blockchain.android.EXTRA_DEPOSIT_ADDRESS"

        fun start(context: Context, depositAddress: String) {
            val intent = Intent(context, TradeDetailActivity::class.java).apply {
                putExtra(EXTRA_DEPOSIT_ADDRESS, depositAddress)
            }
            context.startActivity(intent)
        }
    }
}
package piuk.blockchain.android.ui.shapeshift.detail

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.annotation.StringRes
import android.support.v4.content.ContextCompat
import kotlinx.android.synthetic.main.activity_shapeshift_detail.*
import kotlinx.android.synthetic.main.toolbar_general.*
import org.koin.android.ext.android.inject
import piuk.blockchain.android.R
import piuk.blockchain.android.injection.Injector
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcoreui.ui.base.BaseMvpActivity
import piuk.blockchain.androidcoreui.ui.customviews.MaterialProgressDialog
import piuk.blockchain.androidcoreui.utils.extensions.toast
import java.util.Locale

class ShapeShiftDetailActivity : BaseMvpActivity<ShapeShiftDetailView, ShapeShiftDetailPresenter>(),
    ShapeShiftDetailView {

    override val locale: Locale = Locale.getDefault()

    private val shapeShiftDetailPresenter: ShapeShiftDetailPresenter by inject()

    override val depositAddress: String by lazy { intent.getStringExtra(EXTRA_DEPOSIT_ADDRESS) }

    private var progressDialog: MaterialProgressDialog? = null

    init {
        Injector.getInstance().presenterComponent.inject(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shapeshift_detail)
        setupToolbar(toolbar_general, R.string.shapeshift_in_progress_title)

        onViewReady()
    }

    override fun onSupportNavigateUp() =
        consume { onBackPressed() }

    override fun updateUi(uiState: TradeDetailUiState) {
        setupToolbar(toolbar_general, uiState.title)
        textview_current_stage.setText(uiState.heading)
        textview_current_word_step.text = uiState.message
        imageview_progress.setImageDrawable(ContextCompat.getDrawable(this, uiState.icon))
        textview_receive_amount.setTextColor(ContextCompat.getColor(this, uiState.receiveColor))
    }

    override fun updateDeposit(label: String, amount: String) {
        textview_deposit_title.text = label
        textview_deposit_amount.text = amount
    }

    override fun updateReceive(label: String, amount: String) {
        textview_receive_title.text = label
        textview_receive_amount.text = amount
    }

    override fun updateExchangeRate(exchangeRate: String) {
        textview_rate_value.text = exchangeRate
    }

    override fun updateTransactionFee(displayString: String) {
        textview_transaction_fee_amount.text = displayString
    }

    override fun updateOrderId(displayString: String) {
        textview_order_id_amount.text = displayString
        textview_order_id_amount.setOnClickListener {
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

    override fun createPresenter() = shapeShiftDetailPresenter

    override fun getView() = this

    companion object {

        private const val EXTRA_DEPOSIT_ADDRESS = "piuk.blockchain.android.EXTRA_DEPOSIT_ADDRESS"

        fun start(context: Context, depositAddress: String) {
            val intent = Intent(context, ShapeShiftDetailActivity::class.java).apply {
                putExtra(EXTRA_DEPOSIT_ADDRESS, depositAddress)
            }
            context.startActivity(intent)
        }
    }
}
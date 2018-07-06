package piuk.blockchain.android.ui.buysell.details.recurring

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.v7.widget.Toolbar
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.buysell.details.models.RecurringTradeDisplayModel
import piuk.blockchain.androidcore.utils.helperfunctions.consume
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BaseAuthActivity
import kotlinx.android.synthetic.main.activity_recurring_order_detail.text_view_amount_text as textViewAmount
import kotlinx.android.synthetic.main.activity_recurring_order_detail.text_view_duration_text as textViewDuration
import kotlinx.android.synthetic.main.activity_recurring_order_detail.text_view_frequency_text as textViewFrequency
import kotlinx.android.synthetic.main.activity_recurring_order_detail.tool_bar_recurring_trade as toolBar

class RecurringTradeDetailActivity : BaseAuthActivity() {

    private val model by unsafeLazy {
        intent.getParcelableExtra(EXTRA_RECURRING_TRADE_MODEL) as RecurringTradeDisplayModel
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recurring_order_detail)
        setupToolbar(toolBar as Toolbar, R.string.buy_sell_recurring_order_title)

        textViewAmount
        textViewFrequency
        textViewDuration

        with(model) {
            textViewAmount.text = amountString
            textViewFrequency.text = frequencyString
            val formattedString = SpannableString(String.format(durationStringToFormat, duration))
            formattedString.setSpan(
                ForegroundColorSpan(Color.BLACK),
                0,
                duration.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            textViewDuration.text = formattedString
        }
    }

    override fun onSupportNavigateUp(): Boolean = consume { finish() }

    companion object {

        private const val EXTRA_RECURRING_TRADE_MODEL =
            "piuk.blockchain.android.ui.buysell.details.recurring.EXTRA_RECURRING_TRADE_MODEL"

        fun start(context: Context, recurringTradeModel: RecurringTradeDisplayModel) {
            Intent(context, RecurringTradeDetailActivity::class.java)
                .apply { putExtra(EXTRA_RECURRING_TRADE_MODEL, recurringTradeModel) }
                .run { context.startActivity(this) }
        }
    }
}
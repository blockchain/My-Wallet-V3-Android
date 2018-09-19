package piuk.blockchain.android.ui.buysell.overview.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_balance.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.buysell.overview.models.BuySellDisplayable
import piuk.blockchain.android.ui.buysell.overview.models.BuySellTransaction
import piuk.blockchain.androidcoreui.utils.DateUtil
import piuk.blockchain.androidbuysell.models.coinify.TradeState
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.utils.extensions.context
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import java.util.Calendar

internal class BuySellTransactionDelegate(
    private val listener: CoinifyTxFeedListener
) : AdapterDelegate<BuySellDisplayable> {

    override fun isForViewType(items: List<BuySellDisplayable>, position: Int): Boolean =
        items[position] is BuySellTransaction

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BuySellTransactionViewHolder(parent.inflate(R.layout.item_balance))

    override fun onBindViewHolder(
        items: List<BuySellDisplayable>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        holder as BuySellTransactionViewHolder

        val displayable = items[position]
        holder.bind(displayable as BuySellTransaction, listener, displayable.transactionId)
    }

    private class BuySellTransactionViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        // Utils
        private val dateUtil by unsafeLazy { DateUtil(context) }
        // Views
        private val date = itemView.date
        private val direction = itemView.direction
        private val result = itemView.result
        private val root = itemView

        init {
            itemView.watch_only.gone()
            itemView.tx_note.gone()
            itemView.double_spend_warning.gone()
        }

        fun bind(
            transaction: BuySellTransaction,
            listener: CoinifyTxFeedListener,
            transactionId: Int
        ) {

            root.setOnClickListener { listener.onTransactionClicked(transactionId) }

            val time = transaction.time
            val calendar = Calendar.getInstance()
            val timeZone = calendar.timeZone
            val offset = timeZone.getOffset(time.time)
            val offsetTime = time.time + offset

            date.text = dateUtil.formatted(offsetTime / 1000)
            result.text = transaction.displayAmount
            direction.setText(transaction.tradeStateString)

            when (transaction.tradeState) {
                TradeState.AwaitingTransferIn, TradeState.Processing, TradeState.Reviewing ->
                    onProcessing(transaction.isSellTransaction)
                TradeState.Completed -> onCompleted(transaction.isSellTransaction)
                TradeState.Cancelled, TradeState.Rejected, TradeState.Expired -> onFailed()
                TradeState.Refunded -> onRefunded()
            }
        }

        private fun onProcessing(isSellTransaction: Boolean) {
            if (isSellTransaction) {
                direction.setTextColor(context.getResolvedColor(R.color.product_red_sent_50))
                result.setBackgroundResource(R.drawable.rounded_view_red_50)
            } else {
                direction.setTextColor(context.getResolvedColor(R.color.product_green_received_50))
                result.setBackgroundResource(R.drawable.rounded_view_green_50)
            }
        }

        private fun onCompleted(isSellTransaction: Boolean) {
            if (isSellTransaction) {
                direction.setTextColor(context.getResolvedColor(R.color.product_red_sent))
                result.setBackgroundResource(R.drawable.rounded_view_red)
            } else {
                direction.setTextColor(context.getResolvedColor(R.color.product_green_received))
                result.setBackgroundResource(R.drawable.rounded_view_green)
            }
        }

        private fun onFailed() {
            direction.setTextColor(context.getResolvedColor(R.color.product_red_medium))
            result.setBackgroundResource(R.drawable.rounded_view_failed)
        }

        private fun onRefunded() {
            direction.setTextColor(context.getResolvedColor(R.color.product_gray_transferred))
            result.setBackgroundResource(R.drawable.rounded_view_transferred)
        }
    }
}
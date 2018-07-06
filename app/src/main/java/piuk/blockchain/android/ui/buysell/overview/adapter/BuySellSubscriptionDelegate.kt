package piuk.blockchain.android.ui.buysell.overview.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_buy_sell_subcription.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.buysell.overview.models.BuySellDisplayable
import piuk.blockchain.android.ui.buysell.overview.models.RecurringBuyOrder
import piuk.blockchain.androidcoreui.utils.extensions.inflate

internal class BuySellSubscriptionDelegate(
    private val listener: CoinifyTxFeedListener
) : AdapterDelegate<BuySellDisplayable> {

    override fun isForViewType(items: List<BuySellDisplayable>, position: Int): Boolean =
        items[position] is RecurringBuyOrder

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BuySellSubscriptionViewHolder(parent.inflate(R.layout.item_buy_sell_subcription))

    override fun onBindViewHolder(
        items: List<BuySellDisplayable>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        holder as BuySellSubscriptionViewHolder
        val (displayString, subscriptionId) = items[position] as RecurringBuyOrder
        holder.bind(listener, displayString, subscriptionId)
    }

    private class BuySellSubscriptionViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        private val frequency = itemView.text_view_recurring_frequency

        fun bind(
            listener: CoinifyTxFeedListener,
            displayString: String,
            subscriptionId: Int
        ) {
            frequency.text = displayString
            itemView.setOnClickListener { listener.onSubscriptionClicked(subscriptionId) }
        }
    }
}
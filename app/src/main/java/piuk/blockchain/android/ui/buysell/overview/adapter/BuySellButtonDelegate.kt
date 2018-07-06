package piuk.blockchain.android.ui.buysell.overview.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_buy_sell_buttons.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.buysell.overview.models.BuySellButtons
import piuk.blockchain.android.ui.buysell.overview.models.BuySellDisplayable
import piuk.blockchain.androidcoreui.utils.extensions.inflate

internal class BuySellButtonDelegate(
    private val listener: CoinifyTxFeedListener
) : AdapterDelegate<BuySellDisplayable> {

    override fun isForViewType(items: List<BuySellDisplayable>, position: Int): Boolean =
        items[position] is BuySellButtons

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BuySellButtonViewHolder(parent.inflate(R.layout.item_buy_sell_buttons))

    override fun onBindViewHolder(
        items: List<BuySellDisplayable>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        holder as BuySellButtonViewHolder
        holder.bind(listener)
    }

    private class BuySellButtonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val buy = itemView.button_buy
        private val sell = itemView.button_sell

        fun bind(listener: CoinifyTxFeedListener) {
            buy.setOnClickListener { listener.onBuyClicked() }
            sell.setOnClickListener { listener.onSellClicked() }
        }
    }
}
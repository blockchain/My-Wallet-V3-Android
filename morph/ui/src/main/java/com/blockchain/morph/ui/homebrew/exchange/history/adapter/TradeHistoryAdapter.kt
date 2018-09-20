package com.blockchain.morph.ui.homebrew.exchange.history.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.extensions.toDrawable
import com.blockchain.morph.ui.homebrew.exchange.extensions.toStatusString
import com.blockchain.morph.ui.homebrew.exchange.model.Trade
import kotlinx.android.synthetic.main.list_item_trade_history.view.*
import piuk.blockchain.androidcoreui.utils.extensions.autoNotify
import piuk.blockchain.androidcoreui.utils.extensions.context
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedDrawable
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import kotlin.properties.Delegates

class TradeHistoryAdapter(val listener: (Trade) -> Unit) :
    RecyclerView.Adapter<TradeHistoryAdapter.ViewHolder>() {

    /**
     * Observes the items list and automatically notifies the adapter of changes to the data based
     * on the comparison we make here, which is a simple equality check.
     */
    var items: List<Trade> by Delegates.observable(emptyList()) { _, oldList, newList ->
        autoNotify(oldList, newList) { o, n -> o == n }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(
            parent.inflate(R.layout.list_item_trade_history)
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(items[position], listener)

    override fun getItemCount() = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tradeStatus: TextView = itemView.trade_status
        private val tradeQuantity: TextView = itemView.trade_quantity
        private val tradePrice: TextView = itemView.trade_price
        private val tradeCreatedAt: TextView = itemView.trade_created_at

        @SuppressLint("SetTextI18n")
        fun bind(trade: Trade, listener: (Trade) -> Unit) = with(itemView) {
            tradeStatus.text = trade.state.toStatusString(context)
            tradeStatus.setCompoundDrawablesWithIntrinsicBounds(
                trade.state.toDrawable(context),
                null,
                null,
                null
            )
            tradeQuantity.text = "-${trade.depositQuantity}"
            tradePrice.text = trade.quantity
            tradeCreatedAt.text = trade.createdAt
            setOnClickListener { listener(trade) }
        }
    }
}
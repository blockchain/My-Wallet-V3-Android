package com.blockchain.morph.ui.homebrew.exchange.history

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.blockchain.morph.ui.R
import com.blockchain.morph.ui.homebrew.exchange.model.Trade
import kotlinx.android.synthetic.main.list_item_trade_history.view.*
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class TradeHistoryAdapter(private val trades: List<Trade>, val listener: (Trade) -> Unit) :
    RecyclerView.Adapter<TradeHistoryAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(parent.inflate(R.layout.list_item_trade_history))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(trades[position], listener)

    override fun getItemCount() = trades.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tradeStatus: TextView = itemView.trade_status
        private val tradeQuantity: TextView = itemView.trade_quantity
        private val tradePrice: TextView = itemView.trade_price
        private val tradeCreatedAt: TextView = itemView.trade_created_at

        fun bind(trade: Trade, listener: (Trade) -> Unit) = with(itemView) {
            tradeStatus.text = trade.state
            tradeQuantity.text = trade.quantity
            tradePrice.text = trade.price
            tradeCreatedAt.text = trade.createdAt
            setOnClickListener { listener(trade) }
        }
    }
}
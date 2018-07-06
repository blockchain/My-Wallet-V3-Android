package piuk.blockchain.android.ui.buysell.overview.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.buysell.overview.models.BuySellDisplayable
import piuk.blockchain.android.ui.buysell.overview.models.EmptyTransactionList
import piuk.blockchain.androidcoreui.utils.extensions.inflate

internal class BuySellEmptyListDelegate : AdapterDelegate<BuySellDisplayable> {

    override fun isForViewType(items: List<BuySellDisplayable>, position: Int): Boolean =
        items[position] is EmptyTransactionList

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        BuySellEmptyViewHolder(parent.inflate(R.layout.item_buy_sell_empty_transactions))

    override fun onBindViewHolder(
        items: List<BuySellDisplayable>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        holder as BuySellEmptyViewHolder
    }

    private class BuySellEmptyViewHolder(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView)
}
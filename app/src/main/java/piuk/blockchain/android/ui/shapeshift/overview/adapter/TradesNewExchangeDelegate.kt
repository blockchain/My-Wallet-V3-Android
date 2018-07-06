package piuk.blockchain.android.ui.shapeshift.overview.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.item_shapeshift_row_new_exchange.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.extensions.inflate

class TradesNewExchangeDelegate<in T>(
    private val listClickListener: TradesListClickListener
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is ShapeshiftNewExchangeDisplayable

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        HeaderViewHolder(parent.inflate(R.layout.item_shapeshift_row_new_exchange))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {

        val viewHolder = holder as HeaderViewHolder

        viewHolder.layout.setOnClickListener { listClickListener.onNewExchangeClicked() }
    }

    private class HeaderViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var layout: RelativeLayout = itemView.relative_layout
    }
}

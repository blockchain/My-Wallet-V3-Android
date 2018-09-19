package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.item_balance.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.androidcoreui.utils.DateUtil
import piuk.blockchain.androidcore.data.transactions.models.Displayable
import piuk.blockchain.androidcoreui.utils.extensions.context
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.goneIf
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.visible

class DisplayableDelegate<in T>(
    activity: Activity,
    private var showCrypto: Boolean,
    private val listClickListener: TxFeedClickListener
) : AdapterDelegate<T> {

    private val dateUtil = DateUtil(activity)

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is Displayable

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        TxViewHolder(parent.inflate(R.layout.item_balance))

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {

        val viewHolder = holder as TxViewHolder
        val tx = items[position] as Displayable

        viewHolder.timeSince.text = dateUtil.formatted(tx.timeStamp)

        tx.formatting()
            .applyTransactionFormatting(viewHolder)

        tx.note?.let {
            viewHolder.note.text = it
            viewHolder.note.visible()
        } ?: viewHolder.note.gone()

        viewHolder.result.text = if (showCrypto) {
            tx.totalDisplayableCrypto
        } else {
            tx.totalDisplayableFiat
        }

        viewHolder.watchOnly.goneIf(!tx.watchOnly)
        viewHolder.doubleSpend.goneIf(!tx.doubleSpend)

        // TODO: Move this click listener to the ViewHolder to avoid unnecessary object instantiation during binding
        viewHolder.result.setOnClickListener {
            showCrypto = !showCrypto
            listClickListener.onValueClicked(showCrypto)
        }

        // TODO: Move this click listener to the ViewHolder to avoid unnecessary object instantiation during binding
        viewHolder.itemView.setOnClickListener {
            listClickListener.onTransactionClicked(
                getRealTxPosition(viewHolder.adapterPosition, items), position
            )
        }
    }

    fun onViewFormatUpdated(showCrypto: Boolean) {
        this.showCrypto = showCrypto
    }

    private fun getRealTxPosition(position: Int, items: List<T>): Int {
        val diff = items.size - items.count { it is Displayable }
        return position - diff
    }

    private class TxViewHolder internal constructor(
        itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var result: TextView = itemView.result
        internal var timeSince: TextView = itemView.date
        internal var direction: TextView = itemView.direction
        internal var watchOnly: TextView = itemView.watch_only
        internal var doubleSpend: ImageView = itemView.double_spend_warning
        internal var note: TextView = itemView.tx_note
    }

    private fun DisplayableFormatting.applyTransactionFormatting(viewHolder: DisplayableDelegate.TxViewHolder) {
        viewHolder.direction.setText(text)
        viewHolder.result.setBackgroundResource(valueBackground)
        viewHolder.direction.setTextColor(
            viewHolder.context.getResolvedColor(
                directionColour
            )
        )
    }
}

package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import info.blockchain.wallet.multiaddress.TransactionSummary
import kotlinx.android.synthetic.main.item_balance.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.DateUtil
import piuk.blockchain.androidcore.data.currency.CryptoCurrency
import piuk.blockchain.androidcore.data.transactions.models.Displayable
import piuk.blockchain.androidcoreui.utils.extensions.getContext
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

        when (tx.direction) {
            TransactionSummary.Direction.TRANSFERRED -> displayTransferred(viewHolder, tx)
            TransactionSummary.Direction.RECEIVED -> displayReceived(viewHolder, tx)
            TransactionSummary.Direction.SENT -> displaySent(viewHolder, tx)
            else -> throw IllegalStateException("Tx direction isn't SENT, RECEIVED or TRANSFERRED")
        }

        tx.note?.let {
            viewHolder.note.text = it
            viewHolder.note.visible()
        } ?: viewHolder.note.gone()

        if (showCrypto) {
            viewHolder.result.text = tx.totalDisplayableCrypto
        } else {
            viewHolder.result.text = tx.totalDisplayableFiat
        }

        viewHolder.watchOnly.goneIf { !tx.watchOnly }
        viewHolder.doubleSpend.goneIf { !tx.doubleSpend }

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

    fun onViewFormatUpdated(isBtc: Boolean) {
        this.showCrypto = isBtc
    }

    private fun getResolvedColor(viewHolder: RecyclerView.ViewHolder, @ColorRes color: Int): Int =
        viewHolder.getContext().getResolvedColor(color)

    private fun displayTransferred(viewHolder: TxViewHolder, tx: Displayable) {
        viewHolder.direction.setText(R.string.MOVED)
        viewHolder.result.setBackgroundResource(
            getColorForConfirmations(
                tx,
                R.drawable.rounded_view_transferred_50,
                R.drawable.rounded_view_transferred
            )
        )

        viewHolder.direction.setTextColor(
            getResolvedColor(
                viewHolder, getColorForConfirmations(
                    tx,
                    R.color.product_gray_transferred_50,
                    R.color.product_gray_transferred
                )
            )
        )
    }

    private fun displayReceived(viewHolder: TxViewHolder, tx: Displayable) {
        viewHolder.direction.setText(R.string.RECEIVED)
        viewHolder.result.setBackgroundResource(
            getColorForConfirmations(
                tx,
                R.drawable.rounded_view_green_50,
                R.drawable.rounded_view_green
            )
        )

        viewHolder.direction.setTextColor(
            getResolvedColor(
                viewHolder, getColorForConfirmations(
                    tx,
                    R.color.product_green_received_50,
                    R.color.product_green_received
                )
            )
        )
    }

    private fun displaySent(viewHolder: TxViewHolder, tx: Displayable) {
        viewHolder.direction.setText(R.string.SENT)
        viewHolder.result.setBackgroundResource(
            getColorForConfirmations(
                tx,
                R.drawable.rounded_view_red_50,
                R.drawable.rounded_view_red
            )
        )

        viewHolder.direction.setTextColor(
            getResolvedColor(
                viewHolder, getColorForConfirmations(
                    tx,
                    R.color.product_red_sent_50,
                    R.color.product_red_sent
                )
            )
        )
    }

    private fun getColorForConfirmations(
        tx: Displayable,
        @DrawableRes colorLight: Int,
        @DrawableRes colorDark: Int
    ) = if (tx.confirmations < getRequiredConfirmations(tx)) colorLight else colorDark

    private fun getRequiredConfirmations(tx: Displayable) =
        if (tx.cryptoCurrency == CryptoCurrency.BTC) CONFIRMATIONS_BTC else CONFIRMATIONS_ETH

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

    companion object {

        private const val CONFIRMATIONS_BTC = 3
        private const val CONFIRMATIONS_ETH = 12
    }
}
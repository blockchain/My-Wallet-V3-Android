package piuk.blockchain.android.ui.balance.adapter

import android.app.Activity
import android.graphics.Color
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.style.RelativeSizeSpan
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import info.blockchain.wallet.contacts.data.FacilitatedTransaction
import info.blockchain.wallet.multiaddress.TransactionSummary
import kotlinx.android.synthetic.main.item_balance.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.util.DateUtil
import piuk.blockchain.android.util.MonetaryUtil
import piuk.blockchain.android.util.PrefsUtil
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.getContext
import piuk.blockchain.android.util.extensions.gone
import piuk.blockchain.android.util.extensions.inflate
import piuk.blockchain.android.util.extensions.visible

class TransactionSummaryDelegate<in T>(
        val activity: Activity,
        var btcExchangeRate: Double,
        var isBtc: Boolean,
        val listClickListener: BalanceListClickListener
) : AdapterDelegate<T> {

    val stringUtils = StringUtils(activity)
    val prefsUtil = PrefsUtil(activity)
    val monetaryUtil = MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC))
    val dateUtil = DateUtil(activity)
    var transactionDisplayMap = mutableMapOf<String, ContactTransactionDisplayModel>()

    override fun isForViewType(items: List<T>, position: Int): Boolean =
            items[position] is TransactionSummary

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
            TxViewHolder(parent.inflate(R.layout.item_balance))

    override fun onBindViewHolder(
            items: List<T>,
            position: Int,
            holder: RecyclerView.ViewHolder,
            payloads: List<*>
    ) {

        val viewHolder = holder as TxViewHolder
        val tx = items[position] as TransactionSummary

        val fiatString = prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
        val btcBalance = tx.total.toLong() / 1e8
        val fiatBalance = btcExchangeRate * btcBalance

        viewHolder.result.setTextColor(Color.WHITE)
        viewHolder.timeSince.text = dateUtil.formatted(tx.time)

        when (tx.direction) {
            TransactionSummary.Direction.TRANSFERRED -> displayTransferred(viewHolder, tx)
            TransactionSummary.Direction.RECEIVED -> displayReceived(viewHolder, tx)
            TransactionSummary.Direction.SENT -> displaySent(viewHolder, tx)
            else -> throw IllegalStateException("Tx direction isn't SENT, RECEIVED or TRANSFERRED")
        }

        viewHolder.note.gone()
        viewHolder.contactNameLayout.gone()

        transactionDisplayMap[tx.hash]?.apply {
            viewHolder.note.text = note
            viewHolder.contactName.text = contactName
            viewHolder.note.visible()
            viewHolder.contactNameLayout.visible()

            if (state == FacilitatedTransaction.STATE_PAYMENT_BROADCASTED
                    && role == FacilitatedTransaction.ROLE_PR_RECEIVER) {
                viewHolder.result.setText(R.string.paid)
            }
        }

        viewHolder.result.text = getDisplaySpannable(tx.total.toDouble(), fiatBalance, fiatString)
        viewHolder.watchOnly.visibility = if (tx.isWatchOnly) View.VISIBLE else View.GONE
        viewHolder.doubleSpend.visibility = if (tx.isDoubleSpend) View.VISIBLE else View.GONE

        viewHolder.result.setOnClickListener {
            isBtc = !isBtc
            listClickListener.onValueClicked(isBtc)
        }

        viewHolder.itemView.setOnClickListener {
            listClickListener.onTransactionClicked(
                    getRealTxPosition(viewHolder.adapterPosition, items), position)
        }
    }

    fun onViewFormatUpdated(isBtc: Boolean, btcFormat: Int) {
        this.isBtc = isBtc
        monetaryUtil.updateUnit(btcFormat)
    }

    fun onPriceUpdated(btcExchangeRate: Double) {
        this.btcExchangeRate = btcExchangeRate
    }

    fun onContactsMapUpdated(
            transactionDisplayMap: MutableMap<String, ContactTransactionDisplayModel>
    ) {
        this.transactionDisplayMap = transactionDisplayMap
    }

    private fun getResolvedColor(viewHolder: RecyclerView.ViewHolder, @ColorRes color: Int): Int {
        return ContextCompat.getColor(viewHolder.getContext(), color)
    }

    private fun displayTransferred(viewHolder: TxViewHolder, tx: TransactionSummary) {
        viewHolder.direction.setText(R.string.MOVED)
        viewHolder.result.setBackgroundResource(getColorForConfirmations(
                tx,
                R.drawable.rounded_view_transferred_50,
                R.drawable.rounded_view_transferred
        ))

        viewHolder.direction.setTextColor(
                getResolvedColor(viewHolder, getColorForConfirmations(
                        tx,
                        R.color.product_gray_transferred_50,
                        R.color.product_gray_transferred
                ))
        )
    }

    private fun displayReceived(viewHolder: TxViewHolder, tx: TransactionSummary) {
        viewHolder.direction.setText(R.string.RECEIVED)
        viewHolder.result.setBackgroundResource(getColorForConfirmations(
                tx,
                R.drawable.rounded_view_green_50,
                R.drawable.rounded_view_green
        ))

        viewHolder.direction.setTextColor(
                getResolvedColor(viewHolder, getColorForConfirmations(
                        tx,
                        R.color.product_green_received_50,
                        R.color.product_green_received
                ))
        )
    }

    private fun displaySent(viewHolder: TxViewHolder, tx: TransactionSummary) {
        viewHolder.direction.setText(R.string.SENT)
        viewHolder.result.setBackgroundResource(getColorForConfirmations(
                tx,
                R.drawable.rounded_view_red_50,
                R.drawable.rounded_view_red
        ))

        viewHolder.direction.setTextColor(
                getResolvedColor(viewHolder, getColorForConfirmations(
                        tx,
                        R.color.product_red_sent_50,
                        R.color.product_red_sent
                ))
        )
    }

    private fun getDisplaySpannable(btcAmount: Double, fiatAmount: Double, fiatString: String): Spannable {
        val spannable: Spannable
        if (isBtc) {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    "${monetaryUtil.getDisplayAmountWithFormatting(Math.abs(btcAmount))} ${getDisplayUnits()}")
            spannable.setSpan(
                    RelativeSizeSpan(0.67f),
                    spannable.length - getDisplayUnits().length,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            spannable = Spannable.Factory.getInstance().newSpannable(
                    "${monetaryUtil.getFiatFormat(fiatString).format(Math.abs(fiatAmount))} $fiatString")
            spannable.setSpan(
                    RelativeSizeSpan(0.67f),
                    spannable.length - 3,
                    spannable.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        return spannable
    }

    private fun getDisplayUnits(): String =
            monetaryUtil.getBtcUnits()[prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)]

    private fun getColorForConfirmations(
            tx: TransactionSummary,
            @DrawableRes colorLight: Int,
            @DrawableRes colorDark: Int
    ) = if (tx.confirmations < 3) colorLight else colorDark

    private fun getRealTxPosition(position: Int, items: List<T>): Int {
        val diff = items.size - items.count { it is TransactionSummary }
        return position - diff
    }

    private class TxViewHolder internal constructor(
            itemView: View
    ) : RecyclerView.ViewHolder(itemView) {

        internal var result: TextView = itemView.result
        internal var timeSince: TextView = itemView.date
        internal var direction: TextView = itemView.direction
        internal var watchOnly: TextView = itemView.watch_only
        internal var note: TextView = itemView.note
        internal var doubleSpend: ImageView = itemView.double_spend_warning
        internal var contactName: TextView = itemView.contact_name
        internal var contactNameLayout: LinearLayout = itemView.contact_name_layout

        init {
            contactNameLayout.gone()
        }
    }
}
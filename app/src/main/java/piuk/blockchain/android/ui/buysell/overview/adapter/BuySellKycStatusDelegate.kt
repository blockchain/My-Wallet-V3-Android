package piuk.blockchain.android.ui.buysell.overview.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_buy_sell_kyc_in_progress.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.buysell.overview.models.BuySellDisplayable
import piuk.blockchain.android.ui.buysell.overview.models.KycStatus
import piuk.blockchain.androidcoreui.utils.extensions.getContext
import piuk.blockchain.androidcoreui.utils.extensions.getResolvedColor
import piuk.blockchain.androidcoreui.utils.extensions.inflate

internal class BuySellKycStatusDelegate(
    private val listener: CoinifyTxFeedListener
) : AdapterDelegate<BuySellDisplayable> {

    override fun isForViewType(items: List<BuySellDisplayable>, position: Int): Boolean =
        items[position] is KycStatus

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        KycInProgressViewHolder(parent.inflate(R.layout.item_buy_sell_kyc_in_progress))

    override fun onBindViewHolder(
        items: List<BuySellDisplayable>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        holder as KycInProgressViewHolder
        holder.bind(listener, items[position] as KycStatus)
    }

    private class KycInProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val button = itemView.button_buy_now_card
        private val header = itemView.text_view_in_review_header
        private val message = itemView.text_view_in_review_message

        fun bind(
            listener: CoinifyTxFeedListener,
            kycStatus: KycStatus
        ) {
            itemView.setOnClickListener { listener.onKycReviewClicked(kycStatus) }
            button.setOnClickListener { listener.onKycReviewClicked(kycStatus) }

            when (kycStatus) {
                is KycStatus.InReview -> renderInReview(kycStatus.limitString)
                is KycStatus.Denied -> renderDenied()
                is KycStatus.NotYetCompleted -> renderNotYetCompleted(kycStatus.limitString)
            }
        }

        private fun renderInReview(limit: String) {
            header.setText(R.string.buy_sell_overview_in_review_title)
            message.text =
                getContext().getString(R.string.buy_sell_overview_in_review_message, limit)
            button.setText(R.string.buy_sell_overview_in_review_button)
        }

        private fun renderDenied() {
            header.setTextColor(getContext().getResolvedColor(R.color.product_red_medium))
            header.setText(R.string.buy_sell_overview_denied_title)
            message.setText(R.string.buy_sell_overview_denied_message)
            button.setText(R.string.buy_sell_overview_denied_button)
        }

        private fun renderNotYetCompleted(limit: String) {
            header.setText(R.string.buy_sell_overview_pending_title)
            message.text = getContext().getString(R.string.buy_sell_overview_pending_message, limit)
            button.setText(R.string.buy_sell_overview_pending_button)
        }
    }
}
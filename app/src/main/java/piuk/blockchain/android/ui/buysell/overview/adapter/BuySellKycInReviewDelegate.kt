package piuk.blockchain.android.ui.buysell.overview.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.item_buy_sell_kyc_in_progress.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.buysell.overview.models.BuySellDisplayable
import piuk.blockchain.android.ui.buysell.overview.models.KycInProgress
import piuk.blockchain.androidcoreui.utils.extensions.inflate

internal class BuySellKycInReviewDelegate(
    private val listener: CoinifyTxFeedListener
) : AdapterDelegate<BuySellDisplayable> {

    override fun isForViewType(items: List<BuySellDisplayable>, position: Int): Boolean =
        items[position] is KycInProgress

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        KycInProgressViewHolder(parent.inflate(R.layout.item_buy_sell_kyc_in_progress))

    override fun onBindViewHolder(
        items: List<BuySellDisplayable>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        holder as KycInProgressViewHolder
        holder.bind(listener)
    }

    private class KycInProgressViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val button = itemView.button_buy_now_card

        fun bind(listener: CoinifyTxFeedListener) {
            itemView.setOnClickListener { listener.onKycReviewClicked() }
            button.setOnClickListener { listener.onKycReviewClicked() }
        }
    }
}
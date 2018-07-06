package piuk.blockchain.android.ui.dashboard.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import kotlinx.android.synthetic.main.item_asset_price_card.view.*
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.adapters.AdapterDelegate
import piuk.blockchain.android.ui.dashboard.AssetPriceCardState
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcoreui.utils.extensions.gone
import piuk.blockchain.androidcoreui.utils.extensions.inflate
import piuk.blockchain.androidcoreui.utils.extensions.invisible
import piuk.blockchain.androidcoreui.utils.extensions.visible

class AssetPriceCardDelegate<in T>(
    private val context: Context,
    private val assetSelector: (CryptoCurrencies) -> Unit
) : AdapterDelegate<T> {

    override fun isForViewType(items: List<T>, position: Int): Boolean =
        items[position] is AssetPriceCardState

    override fun onCreateViewHolder(parent: ViewGroup): RecyclerView.ViewHolder =
        AssetPriceCardViewHolder(parent.inflate(R.layout.item_asset_price_card), assetSelector)

    override fun onBindViewHolder(
        items: List<T>,
        position: Int,
        holder: RecyclerView.ViewHolder,
        payloads: List<*>
    ) {
        (holder as AssetPriceCardViewHolder).bind(items[position] as AssetPriceCardState, context)
    }

    private class AssetPriceCardViewHolder internal constructor(
        itemView: View,
        private val assetSelector: (CryptoCurrencies) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {

        internal var price: TextView = itemView.textview_price
        internal var currency: TextView = itemView.textview_currency
        internal var progressBar: ProgressBar = itemView.progress_bar
        internal var error: TextView = itemView.textview_error
        internal var button: LinearLayout = itemView.button_see_charts
        internal var imageView: ImageView = itemView.imageview_chart_icon

        internal fun bind(state: AssetPriceCardState, context: Context) {
            button.setOnClickListener { assetSelector.invoke(state.currency) }
            itemView.setOnClickListener { assetSelector.invoke(state.currency) }
            currency.text = context.getString(R.string.dashboard_price, state.currency.unit)

            updateChartState(state)
        }

        private fun updateChartState(state: AssetPriceCardState) {
            when (state) {
                is AssetPriceCardState.Data -> renderData(state)
                is AssetPriceCardState.Loading -> renderLoading()
                is AssetPriceCardState.Error -> renderError()
            }
        }

        private fun renderData(data: AssetPriceCardState.Data) {
            progressBar.gone()
            error.gone()
            with(price) {
                visible()
                text = data.priceString
            }
            price.visible()
            imageView.setImageResource(data.icon)
        }

        private fun renderLoading() {
            progressBar.visible()
            price.invisible()
            error.gone()
        }

        private fun renderError() {
            progressBar.gone()
            price.invisible()
            error.visible()
        }
    }
}
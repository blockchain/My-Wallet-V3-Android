package piuk.blockchain.android.ui.shapeshift.overview.adapter

import android.app.Activity
import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import piuk.blockchain.android.ui.adapters.AdapterDelegatesManager
import piuk.blockchain.android.ui.adapters.DelegationAdapter
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcoreui.utils.extensions.autoNotify
import java.math.BigDecimal
import kotlin.properties.Delegates

class TradesAdapter(
    activity: Activity,
    btcExchangeRate: Double,
    ethExchangeRate: Double,
    bchExchangeRate: Double,
    displayMode: CurrencyState.DisplayMode,
    listClickListener: TradesListClickListener
) : DelegationAdapter<Any>(AdapterDelegatesManager(), emptyList()) {

    private val tradesDelegate = TradesDisplayableDelegate<Any>(
        activity,
        btcExchangeRate,
        ethExchangeRate,
        bchExchangeRate,
        displayMode,
        listClickListener
    )

    init {
        // Add all necessary AdapterDelegate objects here
        delegatesManager.addAdapterDelegate(TradesNewExchangeDelegate(listClickListener))
        delegatesManager.addAdapterDelegate(TradesHeaderDelegate())
        delegatesManager.addAdapterDelegate(tradesDelegate)
        setHasStableIds(true)
    }

    /**
     * Observes the items list and automatically notifies the adapter of changes to the data based
     * on the comparison we make here, which is a simple equality check.
     */
    override var items: List<Any> by Delegates.observable(emptyList()) { _, oldList, newList ->
        autoNotify(oldList, newList) { o, n -> o == n }
    }

    /**
     * Required so that [setHasStableIds] = true doesn't break the RecyclerView and show duplicated
     * layouts.
     */
    override fun getItemId(position: Int): Long = items[position].hashCode().toLong()

    fun updateTradeList(trades: List<Any>) {
        val mutableList = trades.toMutableList()
        mutableList.add(0, ShapeshiftNewExchangeDisplayable()) // Header delegate

        if (trades.isNotEmpty()) {
            mutableList.add(1, ShapeshiftHeaderDisplayable()) // Row title delegate
        }

        items = mutableList
    }

    /**
     * Notifies the adapter that the View format (ie, whether or not to show BTC) has been changed.
     * Will rebuild the entire adapter.
     */
    fun onViewFormatUpdated(displayMode: CurrencyState.DisplayMode) {
        tradesDelegate.onViewFormatUpdated(displayMode)
        notifyDataSetChanged()
    }

    /**
     * Notifies the adapter that the BTC & ETH exchange rate for the selected currency has been updated.
     * Will rebuild the entire adapter.
     */
    fun onPriceUpdated(lastBtcPrice: Double, lastEthPrice: Double) {
        tradesDelegate.onPriceUpdated(lastBtcPrice, lastEthPrice)
        notifyDataSetChanged()
    }

    fun updateTrade(trade: Trade, tradeResponse: TradeStatusResponse) {
        val matchingTrade = items.filterIsInstance(Trade::class.java)
            .find { it.quote?.deposit == tradeResponse.address }
        matchingTrade?.quote?.withdrawalAmount = trade.quote?.withdrawalAmount
            ?: tradeResponse.incomingCoin ?: BigDecimal.ZERO
        matchingTrade?.quote?.pair = tradeResponse.pair ?: trade.quote?.pair

        notifyDataSetChanged()
    }
}

interface TradesListClickListener {

    fun onTradeClicked(depositAddress: String)

    fun onValueClicked(displayMode: CurrencyState.DisplayMode)

    fun onNewExchangeClicked()
}

class ShapeshiftNewExchangeDisplayable

class ShapeshiftHeaderDisplayable
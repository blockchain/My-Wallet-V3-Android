package piuk.blockchain.android.ui.shapeshift.overview

import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import piuk.blockchain.androidcoreui.ui.base.View

interface ShapeShiftViewRename : View {

    fun onStateUpdated(shapeshiftState: ShapeShiftState)

    fun onTradeUpdate(trade: Trade, tradeResponse: TradeStatusResponse)

    fun onExchangeRateUpdated(
        btcExchangeRate: Double,
        ethExchangeRate: Double,
        bchExchangeRate: Double,
        isBtc: Boolean
    )

    fun onViewTypeChanged(isBtc: Boolean)

    fun showStateSelection()
}
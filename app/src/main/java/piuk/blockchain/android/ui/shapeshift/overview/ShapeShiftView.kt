package piuk.blockchain.android.ui.shapeshift.overview

import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcoreui.ui.base.View

interface ShapeShiftView : View {

    fun onStateUpdated(shapeshiftState: ShapeShiftState)

    fun onTradeUpdate(trade: Trade, tradeResponse: TradeStatusResponse)

    fun onExchangeRateUpdated(
        btcExchangeRate: Double,
        ethExchangeRate: Double,
        bchExchangeRate: Double,
        displayMode: CurrencyState.DisplayMode
    )

    fun onViewTypeChanged(displayMode: CurrencyState.DisplayMode)

    fun showStateSelection()
}

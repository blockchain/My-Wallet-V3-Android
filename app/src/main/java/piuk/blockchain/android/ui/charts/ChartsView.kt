package piuk.blockchain.android.ui.charts

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcoreui.ui.base.View
import java.util.Locale

interface ChartsView : View {

    val cryptoCurrency: CryptoCurrency

    val locale: Locale

    fun updateChartState(state: ChartsState)

    fun updateSelectedCurrency(cryptoCurrency: CryptoCurrency)

    fun updateCurrentPrice(fiatSymbol: String, price: Double)
}
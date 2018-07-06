package piuk.blockchain.android.ui.charts

import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcoreui.ui.base.View
import java.util.Locale

interface ChartsView : View {

    val cryptoCurrency: CryptoCurrencies

    val locale: Locale

    fun updateChartState(state: ChartsState)

    fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies)

    fun updateCurrentPrice(fiatSymbol: String, price: Double)
}
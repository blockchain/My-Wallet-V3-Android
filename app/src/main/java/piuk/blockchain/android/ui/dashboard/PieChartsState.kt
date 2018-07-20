package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.FiatValue
import java.util.Locale

sealed class PieChartsState {

    data class DataPoint(
        val fiatValue: FiatValue,
        val cryptoValueString: String
    ) {
        val isZero: Boolean = fiatValue.isZero
        val fiatValueString: String = fiatValue.toStringWithSymbol(Locale.getDefault())
    }

    data class Data(
        val bitcoin: DataPoint,
        val bitcoinWatchOnly: DataPoint? = null,
        val ether: DataPoint,
        val bitcoinCash: DataPoint
    ) : PieChartsState() {
        val isZero: Boolean = bitcoin.isZero && bitcoinCash.isZero && ether.isZero
        private val totalValue: FiatValue = bitcoin.fiatValue + bitcoinCash.fiatValue + ether.fiatValue
        val totalValueString: String = totalValue.toStringWithSymbol(Locale.getDefault())
    }

    object Loading : PieChartsState()
    object Error : PieChartsState()
}
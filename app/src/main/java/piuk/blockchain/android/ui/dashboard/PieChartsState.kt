package piuk.blockchain.android.ui.dashboard

import info.blockchain.balance.FiatValue
import java.util.Locale

sealed class PieChartsState {

    data class DataPoint(
        val fiatValue: FiatValue,
        val cryptoValueString: String
    ) {
        val isZero = fiatValue.isZero
        val fiatValueString = fiatValue.toStringWithSymbol(Locale.getDefault())
    }

    data class Coin(
        val spendable: DataPoint,
        val watchOnly: DataPoint
    ) {
        val isZero = spendable.isZero && watchOnly.isZero
    }

    data class Data(
        val bitcoin: Coin,
        val ether: Coin,
        val bitcoinCash: Coin
    ) : PieChartsState() {
        private val totalValue =
            bitcoin.spendable.fiatValue +
                bitcoinCash.spendable.fiatValue +
                ether.spendable.fiatValue

        val totalValueString = totalValue.toStringWithSymbol(Locale.getDefault())

        val isZero = bitcoin.isZero && bitcoinCash.isZero && ether.isZero
    }

    object Loading : PieChartsState()
    object Error : PieChartsState()
}
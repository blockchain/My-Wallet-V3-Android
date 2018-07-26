package piuk.blockchain.androidcore.data.shapeshift.models

import info.blockchain.wallet.shapeshift.ShapeShiftPairs
import info.blockchain.balance.CryptoCurrency

/**
 * For strict type checking and convenience.
 */
enum class CoinPairings(val pairCode: String) {
    BTC_TO_ETH(ShapeShiftPairs.BTC_ETH),
    BTC_TO_BCH(ShapeShiftPairs.BTC_BCH),
    ETH_TO_BTC(ShapeShiftPairs.ETH_BTC),
    ETH_TO_BCH(ShapeShiftPairs.ETH_BCH),
    BCH_TO_BTC(ShapeShiftPairs.BCH_BTC),
    BCH_TO_ETH(ShapeShiftPairs.BCH_ETH);

    companion object {

        fun getPair(fromCurrency: CryptoCurrency, toCurrency: CryptoCurrency): CoinPairings =
            when (fromCurrency) {
                CryptoCurrency.BTC -> when (toCurrency) {
                    CryptoCurrency.ETHER -> BTC_TO_ETH
                    CryptoCurrency.BCH -> BTC_TO_BCH
                    else ->
                        throw IllegalArgumentException("Invalid pairing ${toCurrency.symbol} + ${fromCurrency.symbol}")
                }
                CryptoCurrency.ETHER -> when (toCurrency) {
                    CryptoCurrency.BTC -> ETH_TO_BTC
                    CryptoCurrency.BCH -> ETH_TO_BCH
                    else ->
                        throw IllegalArgumentException("Invalid pairing ${toCurrency.symbol} + ${fromCurrency.symbol}")
                }
                CryptoCurrency.BCH -> when (toCurrency) {
                    CryptoCurrency.BTC -> BCH_TO_BTC
                    CryptoCurrency.ETHER -> BCH_TO_ETH
                    else ->
                        throw IllegalArgumentException("Invalid pairing ${toCurrency.symbol} + ${fromCurrency.symbol}")
                }
            }
    }
}
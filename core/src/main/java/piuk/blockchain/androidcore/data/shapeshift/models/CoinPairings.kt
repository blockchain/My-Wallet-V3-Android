package piuk.blockchain.androidcore.data.shapeshift.models

import info.blockchain.wallet.shapeshift.ShapeShiftPairs
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies

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

        fun getPair(fromCurrency: CryptoCurrencies, toCurrency: CryptoCurrencies): CoinPairings =
            when (fromCurrency) {
                CryptoCurrencies.BTC -> when (toCurrency) {
                    CryptoCurrencies.ETHER -> BTC_TO_ETH
                    CryptoCurrencies.BCH -> BTC_TO_BCH
                    else ->
                        throw IllegalArgumentException("Invalid pairing ${toCurrency.symbol} + ${fromCurrency.symbol}")
                }
                CryptoCurrencies.ETHER -> when (toCurrency) {
                    CryptoCurrencies.BTC -> ETH_TO_BTC
                    CryptoCurrencies.BCH -> ETH_TO_BCH
                    else ->
                        throw IllegalArgumentException("Invalid pairing ${toCurrency.symbol} + ${fromCurrency.symbol}")
                }
                CryptoCurrencies.BCH -> when (toCurrency) {
                    CryptoCurrencies.BTC -> BCH_TO_BTC
                    CryptoCurrencies.ETHER -> BCH_TO_ETH
                    else ->
                        throw IllegalArgumentException("Invalid pairing ${toCurrency.symbol} + ${fromCurrency.symbol}")
                }
            }
    }
}
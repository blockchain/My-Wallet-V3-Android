package com.blockchain.morph

import info.blockchain.balance.CryptoCurrency

/**
 * For strict type checking and convenience.
 */
enum class CoinPairings(val pairCode: String) {
    BTC_TO_ETH(C2cPairStrings.BTC_ETH),
    BTC_TO_BCH(C2cPairStrings.BTC_BCH),
    ETH_TO_BTC(C2cPairStrings.ETH_BTC),
    ETH_TO_BCH(C2cPairStrings.ETH_BCH),
    BCH_TO_BTC(C2cPairStrings.BCH_BTC),
    BCH_TO_ETH(C2cPairStrings.BCH_ETH);

    companion object {

        fun getPair(fromCurrency: CryptoCurrency, toCurrency: CryptoCurrency): CoinPairings =
            when (fromCurrency) {
                CryptoCurrency.BTC -> when (toCurrency) {
                    CryptoCurrency.ETHER -> BTC_TO_ETH
                    CryptoCurrency.BCH -> BTC_TO_BCH
                    else -> null
                }
                CryptoCurrency.ETHER -> when (toCurrency) {
                    CryptoCurrency.BTC -> ETH_TO_BTC
                    CryptoCurrency.BCH -> ETH_TO_BCH
                    else -> null
                }
                CryptoCurrency.BCH -> when (toCurrency) {
                    CryptoCurrency.BTC -> BCH_TO_BTC
                    CryptoCurrency.ETHER -> BCH_TO_ETH
                    else -> null
                }
            } ?: throw IllegalArgumentException("Invalid pairing ${toCurrency.symbol} + ${fromCurrency.symbol}")
    }
}
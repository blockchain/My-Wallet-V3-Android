package com.blockchain.morph

import info.blockchain.balance.CryptoCurrency

enum class CoinPair(
    val pairCode: String,
    val from: CryptoCurrency,
    val to: CryptoCurrency
) {

    BTC_TO_BTC("btc_btc", CryptoCurrency.BTC, CryptoCurrency.BTC),
    BTC_TO_ETH("btc_eth", CryptoCurrency.BTC, CryptoCurrency.ETHER),
    BTC_TO_BCH("btc_bch", CryptoCurrency.BTC, CryptoCurrency.BCH),

    ETH_TO_ETH("eth_eth", CryptoCurrency.ETHER, CryptoCurrency.ETHER),
    ETH_TO_BTC("eth_btc", CryptoCurrency.ETHER, CryptoCurrency.BTC),
    ETH_TO_BCH("eth_bch", CryptoCurrency.ETHER, CryptoCurrency.BCH),

    BCH_TO_BCH("bch_bch", CryptoCurrency.BCH, CryptoCurrency.BCH),
    BCH_TO_BTC("bch_btc", CryptoCurrency.BCH, CryptoCurrency.BTC),
    BCH_TO_ETH("bch_eth", CryptoCurrency.BCH, CryptoCurrency.ETHER);

    val sameInputOutput = from == to

    companion object {

        fun getPair(fromCurrency: CryptoCurrency, toCurrency: CryptoCurrency): CoinPair =
            when (fromCurrency) {
                CryptoCurrency.BTC -> when (toCurrency) {
                    CryptoCurrency.BTC -> BTC_TO_BTC
                    CryptoCurrency.ETHER -> BTC_TO_ETH
                    CryptoCurrency.BCH -> BTC_TO_BCH
                }
                CryptoCurrency.ETHER -> when (toCurrency) {
                    CryptoCurrency.ETHER -> ETH_TO_ETH
                    CryptoCurrency.BTC -> ETH_TO_BTC
                    CryptoCurrency.BCH -> ETH_TO_BCH
                }
                CryptoCurrency.BCH -> when (toCurrency) {
                    CryptoCurrency.BCH -> BCH_TO_BCH
                    CryptoCurrency.BTC -> BCH_TO_BTC
                    CryptoCurrency.ETHER -> BCH_TO_ETH
                }
            }

        fun getPair(pairCode: String): CoinPair {
            pairCode.split('_').let {
                if (it.size == 2) {
                    val from = CryptoCurrency.fromSymbol(it.first())
                    val to = CryptoCurrency.fromSymbol(it.last())
                    if (from != null && to != null) {
                        return getPair(from, to)
                    }
                }
                throw IllegalStateException("Attempt to get invalid pair $pairCode")
            }
        }
    }
}
package com.blockchain.morph

import com.blockchain.morph.CoinPair.BCH_TO_BCH
import com.blockchain.morph.CoinPair.BCH_TO_BTC
import com.blockchain.morph.CoinPair.BCH_TO_ETH
import com.blockchain.morph.CoinPair.BTC_TO_BCH
import com.blockchain.morph.CoinPair.BTC_TO_BTC
import com.blockchain.morph.CoinPair.BTC_TO_ETH
import com.blockchain.morph.CoinPair.ETH_TO_BCH
import com.blockchain.morph.CoinPair.ETH_TO_BTC
import com.blockchain.morph.CoinPair.ETH_TO_ETH
import info.blockchain.balance.CryptoCurrency

enum class CoinPair(
    val pairCode: String,
    val from: CryptoCurrency,
    val to: CryptoCurrency,
    val pairCodeUpper: String = pairCode.toUpperCase().replace("_", "-")
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

    fun inverse() = to to from

    companion object {

        fun fromPairCode(pairCode: String): CoinPair {
            return fromPairCodeOrNull(pairCode) ?: throw IllegalStateException("Attempt to get invalid pair $pairCode")
        }

        fun fromPairCodeOrNull(pairCode: String?): CoinPair? {
            pairCode?.split('_')?.let {
                if (it.size == 2) {
                    val from = CryptoCurrency.fromSymbol(it.first())
                    val to = CryptoCurrency.fromSymbol(it.last())
                    if (from != null && to != null) {
                        return from to to
                    }
                }
            }
            return null
        }
    }
}

infix fun CryptoCurrency.to(other: CryptoCurrency) =
    when (this) {
        CryptoCurrency.BTC -> when (other) {
            CryptoCurrency.BTC -> BTC_TO_BTC
            CryptoCurrency.ETHER -> BTC_TO_ETH
            CryptoCurrency.BCH -> BTC_TO_BCH
        }
        CryptoCurrency.ETHER -> when (other) {
            CryptoCurrency.ETHER -> ETH_TO_ETH
            CryptoCurrency.BTC -> ETH_TO_BTC
            CryptoCurrency.BCH -> ETH_TO_BCH
        }
        CryptoCurrency.BCH -> when (other) {
            CryptoCurrency.BCH -> BCH_TO_BCH
            CryptoCurrency.BTC -> BCH_TO_BTC
            CryptoCurrency.ETHER -> BCH_TO_ETH
        }
    }

package com.blockchain.morph

import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test

class CoinPairTest {

    @Test
    fun `get pair BTC - BCH`() {
        CoinPair.getPair(CryptoCurrency.BTC, CryptoCurrency.BCH) `should be` CoinPair.BTC_TO_BCH
    }

    @Test
    fun `get pair BCH - BTC`() {
        CoinPair.getPair(CryptoCurrency.BCH, CryptoCurrency.BTC) `should be` CoinPair.BCH_TO_BTC
    }

    @Test
    fun `get pair BTC - ETH`() {
        CoinPair.getPair(CryptoCurrency.BTC, CryptoCurrency.ETHER) `should be` CoinPair.BTC_TO_ETH
    }

    @Test
    fun `get pair ETH - BTC`() {
        CoinPair.getPair(CryptoCurrency.ETHER, CryptoCurrency.BTC) `should be` CoinPair.ETH_TO_BTC
    }

    @Test
    fun `get pair BCH - ETH`() {
        CoinPair.getPair(CryptoCurrency.BCH, CryptoCurrency.ETHER) `should be` CoinPair.BCH_TO_ETH
    }

    @Test
    fun `get pair ETH - BCH`() {
        CoinPair.getPair(CryptoCurrency.ETHER, CryptoCurrency.BCH) `should be` CoinPair.ETH_TO_BCH
    }

    @Test
    fun `get pair BTC - BTC`() {
        CoinPair.getPair(CryptoCurrency.BTC, CryptoCurrency.BTC) `should be` CoinPair.BTC_TO_BTC
    }

    @Test
    fun `get pair BCH - BCH`() {
        CoinPair.getPair(CryptoCurrency.BCH, CryptoCurrency.BCH) `should be` CoinPair.BCH_TO_BCH
    }

    @Test
    fun `get pair ETH - ETH`() {
        CoinPair.getPair(CryptoCurrency.ETHER, CryptoCurrency.ETHER) `should be` CoinPair.ETH_TO_ETH
    }

    @Test
    fun `pairCode BTC - BTC`() {
        CoinPair.BTC_TO_BTC.pairCode `should be` "btc_btc"
    }

    @Test
    fun `pairCode BCH - BCH`() {
        CoinPair.BCH_TO_BCH.pairCode `should be` "bch_bch"
    }

    @Test
    fun `pairCode ETH - ETH`() {
        CoinPair.ETH_TO_ETH.pairCode `should be` "eth_eth"
    }

    @Test
    fun `pairCode BTC - BCH`() {
        CoinPair.BTC_TO_BCH.pairCode `should be` "btc_bch"
    }

    @Test
    fun `pairCode BCH - BTC`() {
        CoinPair.BCH_TO_BTC.pairCode `should be` "bch_btc"
    }

    @Test
    fun `pairCode BTC - ETH`() {
        CoinPair.BTC_TO_ETH.pairCode `should be` "btc_eth"
    }

    @Test
    fun `pairCode ETH - BTC`() {
        CoinPair.ETH_TO_BTC.pairCode `should be` "eth_btc"
    }

    @Test
    fun `pairCode BCH - ETH`() {
        CoinPair.BCH_TO_ETH.pairCode `should be` "bch_eth"
    }

    @Test
    fun `pairCode ETH - BCH`() {
        CoinPair.ETH_TO_BCH.pairCode `should be` "eth_bch"
    }

    @Test
    fun `sameInputOutput BTC - BTC`() {
        CoinPair.BTC_TO_BTC.sameInputOutput `should be` true
    }

    @Test
    fun `sameInputOutput BCH - BCH`() {
        CoinPair.BTC_TO_BCH.sameInputOutput `should be` false
    }

    @Test
    fun `sameInputOutput all pairs`() {
        CoinPair.values().forEach {
            it.sameInputOutput `should equal` (it.from == it.to)
        }
    }

    @Test
    fun `all pairs can be created from pair codes`() {
        CoinPair.values()
            .forEach {
                val from = it.from
                val to = it.to

                val formedPairCode = "${from.symbol}_${to.symbol}".toLowerCase()

                formedPairCode `should equal` it.pairCode

                CoinPair.getPair(formedPairCode) `should be` it
                CoinPair.getPair(it.pairCode) `should be` it
            }
    }

    @Test
    fun `from invalid pair code, empty`() {
        {
            CoinPair.getPair("")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair "
    }

    @Test
    fun `from invalid pair code, one code`() {
        {
            CoinPair.getPair("btc")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair btc"
    }

    @Test
    fun `from invalid pair code, no _`() {
        {
            CoinPair.getPair("btcbtc")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair btcbtc"
    }

    @Test
    fun `from invalid pair code, unknown code`() {
        {
            CoinPair.getPair("btc_abc")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair btc_abc"
    }

    @Test
    fun `from invalid pair code, double _`() {
        {
            CoinPair.getPair("btc__btc")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair btc__btc"
    }

    @Test
    fun `from invalid pair code, three parts`() {
        {
            CoinPair.getPair("btc_btc_btc")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair btc_btc_btc"
    }
}
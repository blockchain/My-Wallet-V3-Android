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
        CryptoCurrency.BTC to CryptoCurrency.BCH `should be` CoinPair.BTC_TO_BCH
    }

    @Test
    fun `get pair BCH - BTC`() {
        CryptoCurrency.BCH to CryptoCurrency.BTC `should be` CoinPair.BCH_TO_BTC
    }

    @Test
    fun `get pair BTC - ETH`() {
        CryptoCurrency.BTC to CryptoCurrency.ETHER `should be` CoinPair.BTC_TO_ETH
    }

    @Test
    fun `get pair ETH - BTC`() {
        CryptoCurrency.ETHER to CryptoCurrency.BTC `should be` CoinPair.ETH_TO_BTC
    }

    @Test
    fun `get pair BCH - ETH`() {
        CryptoCurrency.BCH to CryptoCurrency.ETHER `should be` CoinPair.BCH_TO_ETH
    }

    @Test
    fun `get pair ETH - BCH`() {
        CryptoCurrency.ETHER to CryptoCurrency.BCH `should be` CoinPair.ETH_TO_BCH
    }

    @Test
    fun `get pair BTC - BTC`() {
        CryptoCurrency.BTC to CryptoCurrency.BTC `should be` CoinPair.BTC_TO_BTC
    }

    @Test
    fun `get pair BCH - BCH`() {
        CryptoCurrency.BCH to CryptoCurrency.BCH `should be` CoinPair.BCH_TO_BCH
    }

    @Test
    fun `get pair ETH - ETH`() {
        CryptoCurrency.ETHER to CryptoCurrency.ETHER `should be` CoinPair.ETH_TO_ETH
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
    fun `pairCode upper BTC - ETH`() {
        CoinPair.BTC_TO_ETH.pairCodeUpper `should equal` "BTC-ETH"
    }

    @Test
    fun `pairCode upper ETH - BCH`() {
        CoinPair.ETH_TO_BCH.pairCodeUpper `should equal` "ETH-BCH"
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

                CoinPair.fromPairCode(formedPairCode) `should be` it
                CoinPair.fromPairCode(formedPairCode.toUpperCase()) `should be` it
                CoinPair.fromPairCode(it.pairCode) `should be` it
                CoinPair.fromPairCodeOrNull(it.pairCode) `should be` it
            }
    }

    @Test
    fun `from invalid pair code, empty`() {
        {
            CoinPair.fromPairCode("")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair "
    }

    @Test
    fun `from invalid pair code, one code`() {
        {
            CoinPair.fromPairCode("btc")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair btc"
    }

    @Test
    fun `from invalid pair code, no _`() {
        {
            CoinPair.fromPairCode("btcbtc")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair btcbtc"
    }

    @Test
    fun `from invalid pair code, unknown code`() {
        {
            CoinPair.fromPairCode("btc_abc")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair btc_abc"
    }

    @Test
    fun `from invalid pair code, double _`() {
        {
            CoinPair.fromPairCode("btc__btc")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair btc__btc"
    }

    @Test
    fun `from invalid pair code, three parts`() {
        {
            CoinPair.fromPairCode("btc_btc_btc")
        } `should throw the Exception`
            IllegalStateException::class `with message`
            "Attempt to get invalid pair btc_btc_btc"
    }

    @Test
    fun `from invalid pair code or null, single _`() {
        CoinPair.fromPairCodeOrNull("_") `should equal` null
    }

    @Test
    fun `from invalid pair code or null, three parts`() {
        CoinPair.fromPairCodeOrNull("btc_btc_btc") `should equal` null
    }

    @Test
    fun `from invalid pair code or null, null`() {
        CoinPair.fromPairCodeOrNull(null) `should equal` null
    }

    @Test
    fun `inverse pair`() {
        CoinPair.BCH_TO_ETH.inverse() `should be` CoinPair.ETH_TO_BCH
    }

    @Test
    fun `inverse all pairs`() {
        CoinPair.values()
            .forEach {
                it.inverse().apply {
                    to `should be` it.from
                    from `should be` it.to
                }
            }
    }
}

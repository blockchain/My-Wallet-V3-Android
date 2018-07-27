package com.blockchain.morph

import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test

class CoinPairingsTest {

    @Test
    fun `get pair BTC - BCH`() {
        CoinPairings.getPair(CryptoCurrency.BTC, CryptoCurrency.BCH) `should be` CoinPairings.BTC_TO_BCH
    }

    @Test
    fun `get pair BCH - BTC`() {
        CoinPairings.getPair(CryptoCurrency.BCH, CryptoCurrency.BTC) `should be` CoinPairings.BCH_TO_BTC
    }

    @Test
    fun `get pair BTC - ETH`() {
        CoinPairings.getPair(CryptoCurrency.BTC, CryptoCurrency.ETHER) `should be` CoinPairings.BTC_TO_ETH
    }

    @Test
    fun `get pair ETH - BTC`() {
        CoinPairings.getPair(CryptoCurrency.ETHER, CryptoCurrency.BTC) `should be` CoinPairings.ETH_TO_BTC
    }

    @Test
    fun `get pair BCH - ETH`() {
        CoinPairings.getPair(CryptoCurrency.BCH, CryptoCurrency.ETHER) `should be` CoinPairings.BCH_TO_ETH
    }

    @Test
    fun `get pair ETH - BCH`() {
        CoinPairings.getPair(CryptoCurrency.ETHER, CryptoCurrency.BCH) `should be` CoinPairings.ETH_TO_BCH
    }

    @Test
    fun `get pair BTC - BTC should throw`() {
        {
            CoinPairings.getPair(
                CryptoCurrency.BTC,
                CryptoCurrency.BTC
            )
        } `should throw the Exception` Exception::class `with message` "Invalid pairing BTC + BTC"
    }

    @Test
    fun `get pair BCH - BCH should throw`() {
        {
            CoinPairings.getPair(
                CryptoCurrency.BCH,
                CryptoCurrency.BCH
            )
        } `should throw the Exception` Exception::class `with message` "Invalid pairing BCH + BCH"
    }

    @Test
    fun `get pair ETH - ETH should throw`() {
        {
            CoinPairings.getPair(
                CryptoCurrency.ETHER,
                CryptoCurrency.ETHER
            )
        } `should throw the Exception` Exception::class `with message` "Invalid pairing ETH + ETH"
    }

    @Test
    fun `pairCode BTC - BCH`() {
        CoinPairings.BTC_TO_BCH.pairCode `should be` C2cPairStrings.BTC_BCH
    }

    @Test
    fun `pairCode BCH - BTC`() {
        CoinPairings.BCH_TO_BTC.pairCode `should be` C2cPairStrings.BCH_BTC
    }

    @Test
    fun `pairCode BTC - ETH`() {
        CoinPairings.BTC_TO_ETH.pairCode `should be` C2cPairStrings.BTC_ETH
    }

    @Test
    fun `pairCode ETH - BTC`() {
        CoinPairings.ETH_TO_BTC.pairCode `should be` C2cPairStrings.ETH_BTC
    }

    @Test
    fun `pairCode BCH - ETH`() {
        CoinPairings.BCH_TO_ETH.pairCode `should be` C2cPairStrings.BCH_ETH
    }

    @Test
    fun `pairCode ETH - BCH`() {
        CoinPairings.ETH_TO_BCH.pairCode `should be` C2cPairStrings.ETH_BCH
    }
}
package piuk.blockchain.androidcore.data.currency

import org.amshove.kluent.`should be`
import org.junit.Test

class CryptoCurrenciesTests {

    @Test
    fun `lowercase btc`() {
        CryptoCurrencies.fromSymbol("btc") `should be` CryptoCurrencies.BTC
    }

    @Test
    fun `uppercase BTC`() {
        CryptoCurrencies.fromSymbol("BTC") `should be` CryptoCurrencies.BTC
    }

    @Test
    fun `lowercase bch`() {
        CryptoCurrencies.fromSymbol("btc") `should be` CryptoCurrencies.BTC
    }

    @Test
    fun `uppercase BCH`() {
        CryptoCurrencies.fromSymbol("BCH") `should be` CryptoCurrencies.BCH
    }

    @Test
    fun `lowercase eth`() {
        CryptoCurrencies.fromSymbol("eth") `should be` CryptoCurrencies.ETHER
    }

    @Test
    fun `uppercase ETH`() {
        CryptoCurrencies.fromSymbol("ETH") `should be` CryptoCurrencies.ETHER
    }

    @Test
    fun `empty should return null`() {
        CryptoCurrencies.fromSymbol("") `should be` null
    }

    @Test
    fun `not recognised should return null`() {
        CryptoCurrencies.fromSymbol("NONE") `should be` null
    }
}

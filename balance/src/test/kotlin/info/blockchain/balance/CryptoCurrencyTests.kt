package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.junit.Test

class CryptoCurrencyTests {

    @Test
    fun `lowercase btc`() {
        CryptoCurrency.fromSymbol("btc") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `uppercase BTC`() {
        CryptoCurrency.fromSymbol("BTC") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `lowercase bch`() {
        CryptoCurrency.fromSymbol("btc") `should be` CryptoCurrency.BTC
    }

    @Test
    fun `uppercase BCH`() {
        CryptoCurrency.fromSymbol("BCH") `should be` CryptoCurrency.BCH
    }

    @Test
    fun `lowercase eth`() {
        CryptoCurrency.fromSymbol("eth") `should be` CryptoCurrency.ETHER
    }

    @Test
    fun `uppercase ETH`() {
        CryptoCurrency.fromSymbol("ETH") `should be` CryptoCurrency.ETHER
    }

    @Test
    fun `empty should return null`() {
        CryptoCurrency.fromSymbol("") `should be` null
    }

    @Test
    fun `not recognised should return null`() {
        CryptoCurrency.fromSymbol("NONE") `should be` null
    }
}

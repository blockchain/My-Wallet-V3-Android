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

    @Test
    fun `btc dp is 8`() {
        CryptoCurrency.BTC.dp `should be` 8
    }

    @Test
    fun `bch dp is 8`() {
        CryptoCurrency.BCH.dp `should be` 8
    }

    @Test
    fun `ether dp is 18`() {
        CryptoCurrency.ETHER.dp `should be` 18
    }

    @Test
    fun `btc required confirmations is 3`() {
        CryptoCurrency.BTC.requiredConfirmations `should be` 3
    }

    @Test
    fun `bch required confirmations is 3`() {
        CryptoCurrency.BCH.requiredConfirmations `should be` 3
    }

    @Test
    fun `ether required confirmations is 12`() {
        CryptoCurrency.ETHER.requiredConfirmations `should be` 12
    }
}

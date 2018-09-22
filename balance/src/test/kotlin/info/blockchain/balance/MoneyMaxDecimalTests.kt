package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.junit.Test

class MoneyMaxDecimalTests {

    @Test
    fun `FiatValue CAD`() {
        val money: Money = 1.cad()
        money.maxDecimalPlaces `should be` 2
        money.userDecimalPlaces `should be` 2
    }

    @Test
    fun `FiatValue JPY`() {
        val money: Money = 123.jpy()
        money.maxDecimalPlaces `should be` 0
        money.userDecimalPlaces `should be` 0
    }

    @Test
    fun `CryptoValue BTC`() {
        val money: Money = 123.bitcoin()
        money.maxDecimalPlaces `should be` 8
        money.userDecimalPlaces `should be` 8
    }

    @Test
    fun `CryptoValue BCH`() {
        val money: Money = 123.bitcoinCash()
        money.maxDecimalPlaces `should be` 8
        money.userDecimalPlaces `should be` 8
    }

    @Test
    fun `CryptoValue ETH`() {
        val money: Money = 123.ether()
        money.maxDecimalPlaces `should be` 18
        money.userDecimalPlaces `should be` 8
    }
}

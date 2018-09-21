package info.blockchain.balance

import org.amshove.kluent.`should equal`
import org.junit.Test
import java.util.Locale

class MoneyFormattingTests {

    @Test
    fun `FiatValue formatted as Money`() {
        val money: Money = 1.cad()
        money.symbol(Locale.CANADA) `should equal` "$"
        money.toStringWithSymbol(Locale.CANADA) `should equal` "$1.00"
        money.toStringWithoutSymbol(Locale.CANADA) `should equal` "1.00"
    }

    @Test
    fun `FiatValue JPY formatted as Money`() {
        val money: Money = 123.jpy()
        money.symbol(Locale.US) `should equal` "JPY"
        money.toStringWithSymbol(Locale.US) `should equal` "JPY123"
        money.toStringWithoutSymbol(Locale.US) `should equal` "123"
    }

    @Test
    fun `CryptoValue formatted as Money`() {
        val money: Money = 1.23.bitcoin()
        money.symbol(Locale.US) `should equal` "BTC"
        money.toStringWithSymbol(Locale.US) `should equal` "1.23 BTC"
        money.toStringWithoutSymbol(Locale.US) `should equal` "1.23"
    }

    @Test
    fun `CryptoValue Ether formatted as Money`() {
        val money: Money = 1.23.ether()
        money.symbol(Locale.FRANCE) `should equal` "ETH"
        money.toStringWithSymbol(Locale.FRANCE) `should equal` "1,23 ETH"
        money.toStringWithoutSymbol(Locale.FRANCE) `should equal` "1,23"
    }
}

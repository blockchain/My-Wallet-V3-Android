package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test

class MoneyComparisonTest {

    @Test
    fun `fiat - greater than`() {
        val a: Money = 11.usd()
        val b: Money = 10.usd()
        (a > b) `should be` true
    }

    @Test
    fun `fiat - equality`() {
        val a: Money = 10.usd()
        val b: Money = 10.usd()
        (a == b) `should be` true
    }

    @Test
    fun `fiat - greater than equal`() {
        val a: Money = 11.usd()
        val b: Money = 10.usd()
        (a >= b) `should be` true
    }

    @Test
    fun `fiat - not greater than`() {
        val a: Money = 9.gbp()
        val b: Money = 10.gbp()
        (a > b) `should be` false
    }

    @Test
    fun `fiat - can't compare different currencies`() {
        val a: Money = 3.cad()
        val b: Money = 3.usd();
        { a > b } `should throw the Exception` ComparisonException::class `with message` "Can't compare CAD and USD"
    }

    @Test
    fun `crypto - equality`() {
        val a: Money = 10.ether()
        val b: Money = 10.ether()
        (a == b) `should be` true
    }

    @Test
    fun `crypto - greater than`() {
        val a: Money = 11.bitcoin()
        val b: Money = 10.bitcoin()
        (a > b) `should be` true
    }

    @Test
    fun `crypto - not greater than`() {
        val a: Money = 10.bitcoin()
        val b: Money = 10.bitcoin()
        (a > b) `should be` false
    }

    @Test
    fun `crypto - greater than equal`() {
        val a: Money = 10.bitcoinCash()
        val b: Money = 10.bitcoinCash()
        (a >= b) `should be` true
    }

    @Test
    fun `crypto - can't compare different currencies`() {
        val a: Money = 3.ether()
        val b: Money = 3.bitcoin();
        { a > b } `should throw the Exception` ComparisonException::class `with message` "Can't compare ETH and BTC"
    }

    @Test
    fun `can't compare different values - fiat lhs`() {
        val a: Money = 3.usd()
        val b: Money = 3.bitcoin();
        { a > b } `should throw the Exception` ComparisonException::class `with message` "Can't compare USD and BTC"
    }

    @Test
    fun `can't compare different values - crypto lhs`() {
        val a: Money = 3.bitcoin()
        val b: Money = 3.usd();
        { a > b } `should throw the Exception` ComparisonException::class `with message` "Can't compare BTC and USD"
    }

    @Test
    fun `crypto fiat - inequality`() {
        val a: Money = 10.ether()
        val b: Money = 10.usd()
        (a == b) `should be` false
    }
}

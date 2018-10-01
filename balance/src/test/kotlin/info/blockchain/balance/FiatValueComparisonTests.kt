package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test

class FiatValueComparisonTests {

    @Test
    fun `compare greater than true`() {
        val a = 2.usd()
        val b = 1.usd()
        (a > b) `should be` true
    }

    @Test
    fun `compare greater than false`() {
        val a = 2.cad()
        val b = 3.cad()
        (a > b) `should be` false
    }

    @Test
    fun `compare greater than false because equal`() {
        val a = 3.gbp()
        val b = 3.gbp()
        (a > b) `should be` false
    }

    @Test
    fun `compare less than false because equal`() {
        val a = 3.jpy()
        val b = 3.jpy()
        (a < b) `should be` false
    }

    @Test
    fun `compare equal`() {
        val a = 3.usd()
        val b = 3.usd()
        (a == b) `should be` true
    }

    @Test
    fun `can't compare different currencies with greater than`() {
        val a = 3.cad()
        val b = 3.usd();
        { a > b } `should throw the Exception` ComparisonException::class `with message` "Can't compare CAD and USD"
    }

    @Test
    fun `can't compare different currencies with less than`() {
        val a = 3.jpy()
        val b = 3.gbp();
        { a < b } `should throw the Exception` ComparisonException::class `with message` "Can't compare JPY and GBP"
    }

    @Test
    fun `can compare different currencies with equals`() {
        val a = 3.jpy()
        val b = 3.usd()
        (a == b) `should be` false
    }
}

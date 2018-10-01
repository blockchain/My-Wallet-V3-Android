package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test

class CryptoValueComparisonTests {

    @Test
    fun `compare greater than true`() {
        val a = CryptoValue.bitcoinFromSatoshis(2)
        val b = CryptoValue.bitcoinFromSatoshis(1)
        (a > b) `should be` true
    }

    @Test
    fun `compare greater than false`() {
        val a = 2.ether()
        val b = 3.ether()
        (a > b) `should be` false
    }

    @Test
    fun `compare greater than false because equal`() {
        val a = 3.ether()
        val b = 3.ether()
        (a > b) `should be` false
    }

    @Test
    fun `compare less than false because equal`() {
        val a = 3.ether()
        val b = 3.ether()
        (a < b) `should be` false
    }

    @Test
    fun `compare equal`() {
        val a = 3.ether()
        val b = 3.ether()
        (a == b) `should be` true
    }

    @Test
    fun `can't compare different currencies with greater than`() {
        val a = 3.ether()
        val b = 3.bitcoin();
        { a > b } `should throw the Exception` ComparisonException::class `with message` "Can't compare ETH and BTC"
    }

    @Test
    fun `can't compare different currencies with less than`() {
        val a = 3.bitcoin()
        val b = 3.ether();
        { a < b } `should throw the Exception` ComparisonException::class `with message` "Can't compare BTC and ETH"
    }

    @Test
    fun `can compare different currencies with equals`() {
        val a = CryptoValue.bitcoinCashFromSatoshis(3)
        val b = CryptoValue.bitcoinFromSatoshis(3)
        (a == b) `should be` false
    }
}

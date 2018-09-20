package info.blockchain.balance

import org.amshove.kluent.`should equal`
import org.amshove.kluent.`should not be`
import org.amshove.kluent.`should not equal`
import org.junit.Test

class MoneyEqualityTests {

    @Test
    fun `c2c equality`() {
        val m1: Money = CryptoValue.bitcoinFromMajor(1)
        val m2: Money = CryptoValue.bitcoinFromMajor(1)

        m1 `should not be` m2
        m1 `should equal` m2
    }

    @Test
    fun `c2c inequality by value`() {
        val m1: Money = CryptoValue.bitcoinFromMajor(1)
        val m2: Money = CryptoValue.bitcoinFromMajor(2)

        m1 `should not be` m2
        m1 `should not equal` m2
    }

    @Test
    fun `c2c inequality by currency`() {
        val m1: Money = CryptoValue.bitcoinFromMajor(1)
        val m2: Money = CryptoValue.bitcoinCashFromMajor(1)

        m1 `should not be` m2
        m1 `should not equal` m2
    }

    @Test
    fun `c2f inequality`() {
        val m1: Money = CryptoValue.bitcoinFromMajor(1)
        val m2: Money = FiatValue.fromMinor("USD", 1)

        m1 `should not be` m2
        m1 `should not equal` m2
    }

    @Test
    fun `f2f inequality by currency`() {
        val m1: Money = FiatValue.fromMinor("GBP", 1)
        val m2: Money = FiatValue.fromMinor("USD", 1)

        m1 `should not be` m2
        m1 `should not equal` m2
    }

    @Test
    fun `f2f inequality by value`() {
        val m1: Money = FiatValue.fromMinor("CAD", 1)
        val m2: Money = FiatValue.fromMinor("CAD", 2)

        m1 `should not be` m2
        m1 `should not equal` m2
    }

    @Test
    fun `f2f equality`() {
        val m1: Money = FiatValue.fromMinor("CAD", 2)
        val m2: Money = FiatValue.fromMinor("CAD", 2)

        m1 `should not be` m2
        m1 `should equal` m2
    }
}

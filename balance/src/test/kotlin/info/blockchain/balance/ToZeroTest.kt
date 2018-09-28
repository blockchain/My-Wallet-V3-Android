package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class ToZeroTest {

    @Test
    fun `bitcoin to zero`() {
        val zero: CryptoValue = 1.bitcoin().toZero()
        zero `should be` CryptoValue.ZeroBtc
    }

    @Test
    fun `ether to zero`() {
        9.1.ether().toZero() `should be` CryptoValue.ZeroEth
    }

    @Test
    fun `bitcoin to zero via money`() {
        val bitcoin: Money = 1.bitcoin()
        val zero: Money = bitcoin.toZero()
        zero `should be` CryptoValue.ZeroBtc
    }

    @Test
    fun `gbp toZero`() {
        val zero: FiatValue = 1.2.gbp().toZero()
        zero `should equal` 0.gbp()
    }

    @Test
    fun `usd toZero`() {
        9.8.usd().toZero() `should equal` 0.usd()
    }

    @Test
    fun `usd to zero via money`() {
        val usd: Money = 1.usd()
        val zero: Money = usd.toZero()
        zero `should equal` 0.usd()
    }
}

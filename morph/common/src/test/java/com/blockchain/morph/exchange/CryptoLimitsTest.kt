package com.blockchain.morph.exchange

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test

class CryptoLimitsTest {

    @Test
    fun `can create and read limits`() {
        val min = 1.0.bitcoin()
        val max = 2.0.bitcoin()
        CryptoLimits(min, max)
            .apply {
                min `should be` min
                max `should be` max
            }
    }

    @Test
    fun `can apply to value in range`() {
        CryptoLimits(1.0.bitcoin(), 2.0.bitcoin())
            .apply {
                val value = 1.5.bitcoin()
                clamp(value) `should be` value
            }
    }

    @Test
    fun `can apply to value at range minimum`() {
        CryptoLimits(1.0.bitcoin(), 2.0.bitcoin())
            .apply {
                val value = 1.0.bitcoin()
                clamp(value) `should be` value
            }
    }

    @Test
    fun `can apply to value at range maximum`() {
        CryptoLimits(1.0.bitcoin(), 2.0.bitcoin())
            .apply {
                val value = 2.0.bitcoin()
                clamp(value) `should be` value
            }
    }

    @Test
    fun `can apply to value above range maximum`() {
        val upper = 2.0.bitcoinCash()
        CryptoLimits(1.0.bitcoinCash(), upper)
            .clamp(2.1.bitcoinCash()) `should be` upper
    }

    @Test
    fun `can apply to value below range minimum`() {
        val lower = 2.0.ether()
        CryptoLimits(lower, 2.0.ether())
            .clamp(0.9.ether()) `should be` lower
    }

    @Test
    fun `if range is zero width, it returns min`() {
        val minCryptoValue = 2.0.ether()
        CryptoLimits(minCryptoValue, 2.0.ether())
            .clamp(0.9.ether()) `should be` minCryptoValue
    }

    @Test
    fun `if range is invalid, it returns zero`() {
        CryptoLimits(3.0.ether(), 2.0.ether())
            .clamp(2.0.ether()) `should be` CryptoValue.zero(CryptoCurrency.ETHER)
    }

    @Test
    fun `if range has different currencies, it throws exception`() {
        CryptoLimits(3.0.ether(), 2.0.bitcoin())
            .apply {
                { clamp(2.0.ether()) } `should throw the Exception`
                    Exception::class `with message`
                    "Can't compare ETH and BTC"
            }
    }

    @Test
    fun `if value is different to range currencies, it throws exception`() {
        CryptoLimits(1.0.ether(), 2.0.ether())
            .apply {
                { clamp(2.0.bitcoinCash()) } `should throw the Exception`
                    Exception::class `with message`
                    "Can't compare BCH and ETH"
            }
    }
}

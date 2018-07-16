package info.blockchain.balance

import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

class CryptoValueTests {

    @Test
    fun `zero btc`() {
        CryptoValue.ZeroBtc `should equal` CryptoValue(CryptoCurrency.BTC, BigInteger.ZERO)
    }

    @Test
    fun `zero bch`() {
        CryptoValue.ZeroBch `should equal` CryptoValue(CryptoCurrency.BCH, BigInteger.ZERO)
    }

    @Test
    fun `zero eth`() {
        CryptoValue.ZeroEth `should equal` CryptoValue(CryptoCurrency.ETHER, BigInteger.ZERO)
    }

    @Test
    fun `toMajorUnit BTC`() {
        CryptoValue.bitcoinFromSatoshis(12345678901L).toMajorUnit() `should equal` BigDecimal("123.45678901")
    }

    @Test
    fun `toMajorUnit BCH`() {
        CryptoValue.bitcoinCashFromSatoshis(234L).toMajorUnit() `should equal` BigDecimal("0.00000234")
    }

    @Test
    fun `toMajorUnit ETH`() {
        CryptoValue(
            CryptoCurrency.ETHER,
            234L.toBigInteger()
        ).toMajorUnit() `should equal` BigDecimal("0.000000000000000234")
    }

    @Test
    fun `toMajorUnit keeps all trailing 0s`() {
        CryptoValue(
            CryptoCurrency.BTC,
            10000000000L.toBigInteger()
        ).toMajorUnit() `should equal` BigDecimal("100.00000000")
    }

    @Test
    fun `toMajorUnitDouble`() {
        CryptoValue(CryptoCurrency.BTC, 12300001234L.toBigInteger()).toMajorUnitDouble() `should equal` 123.00001234
    }

    @Test
    fun `zero is not positive`() {
        CryptoValue.ZeroBtc.isPositive() `should be` false
    }

    @Test
    fun `1 Satoshi is positive`() {
        CryptoValue.bitcoinFromSatoshis(1).isPositive() `should be` true
    }

    @Test
    fun `2 Satoshis is positive`() {
        CryptoValue.bitcoinFromSatoshis(2).isPositive() `should be` true
    }

    @Test
    fun `-1 Satoshi is not positive`() {
        CryptoValue.bitcoinFromSatoshis(-1).isPositive() `should be` false
    }
}
package info.blockchain.balance

import org.amshove.kluent.`should equal`
import org.junit.Test

class CryptoCurrencyFormatterTest {

    @Test
    fun `format BTC from Crypto Value`() {
        CryptoValue.ZeroBtc.format() `should equal` "0"
        CryptoValue.bitcoinFromMajor(1).format() `should equal` "1.0"
        CryptoValue.bitcoinFromMajor(10_000).format() `should equal` "10,000.0"
        CryptoValue.bitcoinFromMajor(21_000_000).format() `should equal` "21,000,000.0"
    }

    @Test
    fun `format BCH from Crypto Value`() {
        CryptoValue.ZeroBch.format() `should equal` "0"
        CryptoValue.bitcoinCashFromMajor(1).format() `should equal` "1.0"
        CryptoValue.bitcoinCashFromMajor(10_000).format() `should equal` "10,000.0"
        CryptoValue.bitcoinCashFromMajor(21_000_000).format() `should equal` "21,000,000.0"
    }

    @Test
    fun `format Ether from Crypto Value`() {
        CryptoValue.ZeroEth.format() `should equal` "0"
        CryptoValue.etherFromMajor(1).format() `should equal` "1.0"
        CryptoValue.etherFromMajor(10_000).format() `should equal` "10,000.0"
        CryptoValue.etherFromMajor(100_000_000).format() `should equal` "100,000,000.0"
    }

    @Test
    fun `formatWithUnit 0 BTC`() {
        CryptoValue.ZeroBtc.formatWithUnit(FormatPrecision.Short) `should equal` "0 BTC"
    }

    @Test
    fun `formatWithUnit BTC`() {
        CryptoValue.bitcoinFromMajor(1).formatWithUnit() `should equal` "1.0 BTC"
        CryptoValue.bitcoinFromMajor(10_000).formatWithUnit() `should equal` "10,000.0 BTC"
        CryptoValue.bitcoinFromMajor(21_000_000).formatWithUnit() `should equal` "21,000,000.0 BTC"
    }

    @Test
    fun `formatWithUnit BTC fractions`() {
        CryptoValue.bitcoinFromSatoshis(1L).formatWithUnit() `should equal` "0.00000001 BTC"
        CryptoValue.bitcoinFromSatoshis(10L).formatWithUnit() `should equal` "0.0000001 BTC"
        CryptoValue.bitcoinFromSatoshis(100L).formatWithUnit() `should equal` "0.000001 BTC"
        CryptoValue.bitcoinFromSatoshis(1000L).formatWithUnit() `should equal` "0.00001 BTC"
        CryptoValue.bitcoinFromSatoshis(10000L).formatWithUnit() `should equal` "0.0001 BTC"
        CryptoValue.bitcoinFromSatoshis(100000L).formatWithUnit() `should equal` "0.001 BTC"
        CryptoValue.bitcoinFromSatoshis(1000000L).formatWithUnit() `should equal` "0.01 BTC"
        CryptoValue.bitcoinFromSatoshis(10000000L).formatWithUnit() `should equal` "0.1 BTC"
        CryptoValue.bitcoinFromSatoshis(120000000L).formatWithUnit() `should equal` "1.2 BTC"
    }

    @Test
    fun `formatWithUnit 0 BCH`() {
        CryptoValue.ZeroBch.formatWithUnit() `should equal` "0 BCH"
    }

    @Test
    fun `formatWithUnit BCH`() {
        CryptoValue.bitcoinCashFromMajor(1).formatWithUnit() `should equal` "1.0 BCH"
        CryptoValue.bitcoinCashFromMajor(10_000).formatWithUnit() `should equal` "10,000.0 BCH"
        CryptoValue.bitcoinCashFromMajor(21_000_000).formatWithUnit() `should equal` "21,000,000.0 BCH"
    }

    @Test
    fun `formatWithUnit BCH fractions`() {
        CryptoValue.bitcoinCashFromSatoshis(1L).formatWithUnit() `should equal` "0.00000001 BCH"
        CryptoValue.bitcoinCashFromSatoshis(10L).formatWithUnit() `should equal` "0.0000001 BCH"
        CryptoValue.bitcoinCashFromSatoshis(100L).formatWithUnit() `should equal` "0.000001 BCH"
        CryptoValue.bitcoinCashFromSatoshis(1000L).formatWithUnit() `should equal` "0.00001 BCH"
        CryptoValue.bitcoinCashFromSatoshis(10000L).formatWithUnit() `should equal` "0.0001 BCH"
        CryptoValue.bitcoinCashFromSatoshis(100000L).formatWithUnit() `should equal` "0.001 BCH"
        CryptoValue.bitcoinCashFromSatoshis(1000000L).formatWithUnit() `should equal` "0.01 BCH"
        CryptoValue.bitcoinCashFromSatoshis(10000000L).formatWithUnit() `should equal` "0.1 BCH"
        CryptoValue.bitcoinCashFromSatoshis(120000000L).formatWithUnit() `should equal` "1.2 BCH"
    }

    @Test
    fun `formatWithUnit 0 ETH`() {
        CryptoValue.ZeroEth.formatWithUnit() `should equal` "0 ETH"
    }

    @Test
    fun `formatWithUnit ETH`() {
        CryptoValue.etherFromMajor(1).formatWithUnit() `should equal` "1.0 ETH"
        CryptoValue.etherFromMajor(10_000).formatWithUnit() `should equal` "10,000.0 ETH"
        CryptoValue.etherFromMajor(1_000_000_000).formatWithUnit() `should equal` "1,000,000,000.0 ETH"
    }

    @Test
    fun `formatWithUnit ETH fractions too small to display`() {
        CryptoValue.etherFromWei(1L).formatWithUnit() `should equal` "0 ETH"
        CryptoValue.etherFromWei(10L).formatWithUnit() `should equal` "0 ETH"
        CryptoValue.etherFromWei(100L).formatWithUnit() `should equal` "0 ETH"
        CryptoValue.etherFromWei(1_000L).formatWithUnit() `should equal` "0 ETH"
        CryptoValue.etherFromWei(10_000L).formatWithUnit() `should equal` "0 ETH"
        CryptoValue.etherFromWei(100_000L).formatWithUnit() `should equal` "0 ETH"
        CryptoValue.etherFromWei(1_000_000L).formatWithUnit() `should equal` "0 ETH"
        CryptoValue.etherFromWei(10_000_000L).formatWithUnit() `should equal` "0 ETH"
        CryptoValue.etherFromWei(100_000_000L).formatWithUnit() `should equal` "0 ETH"
        CryptoValue.etherFromWei(1_000_000_000L).formatWithUnit() `should equal` "0 ETH"
    }

    @Test
    fun `formatWithUnit ETH with tiny fractions - full precision`() {
        val formatWithUnit =
            { wei: Long -> CryptoValue.etherFromWei(wei).formatWithUnit(FormatPrecision.Full) }
        formatWithUnit(1L) `should equal` "0.000000000000000001 ETH"
        formatWithUnit(10L) `should equal` "0.00000000000000001 ETH"
        formatWithUnit(100L) `should equal` "0.0000000000000001 ETH"
        formatWithUnit(1_000L) `should equal` "0.000000000000001 ETH"
        formatWithUnit(10_000L) `should equal` "0.00000000000001 ETH"
        formatWithUnit(100_000L) `should equal` "0.0000000000001 ETH"
        formatWithUnit(1_000_000L) `should equal` "0.000000000001 ETH"
        formatWithUnit(10_000_000L) `should equal` "0.00000000001 ETH"
        formatWithUnit(100_000_000L) `should equal` "0.0000000001 ETH"
        formatWithUnit(1_000_000_000L) `should equal` "0.000000001 ETH"
        formatWithUnit(10_000_000_000L) `should equal` "0.00000001 ETH"
        formatWithUnit(100_000_000_000L) `should equal` "0.0000001 ETH"
    }

    @Test
    fun `formatWithUnit ETH fractions`() {
        CryptoValue.etherFromWei(10_000_000_000L).formatWithUnit() `should equal` "0.00000001 ETH"
        CryptoValue.etherFromWei(100_000_000_000L).formatWithUnit() `should equal` "0.0000001 ETH"
        CryptoValue.etherFromWei(1_000_000_000_000L).formatWithUnit() `should equal` "0.000001 ETH"
        CryptoValue.etherFromWei(10_000_000_000_000L).formatWithUnit() `should equal` "0.00001 ETH"
        CryptoValue.etherFromWei(100_000_000_000_000L).formatWithUnit() `should equal` "0.0001 ETH"
        CryptoValue.etherFromWei(1_000_000_000_000_000L).formatWithUnit() `should equal` "0.001 ETH"
        CryptoValue.etherFromWei(10_000_000_000_000_000L).formatWithUnit() `should equal` "0.01 ETH"
        CryptoValue.etherFromWei(100_000_000_000_000_000L).formatWithUnit() `should equal` "0.1 ETH"
        CryptoValue.etherFromWei(1_200_000_000_000_000_000).formatWithUnit() `should equal` "1.2 ETH"
    }
}

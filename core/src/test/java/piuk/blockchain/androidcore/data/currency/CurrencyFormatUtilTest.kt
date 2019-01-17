package piuk.blockchain.androidcore.data.currency

import com.blockchain.testutils.usd
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FormatPrecision
import org.amshove.kluent.`should equal`
import java.math.BigDecimal
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatUtilTest {

    private val subject: CurrencyFormatUtil = CurrencyFormatUtil()

    @Test
    fun formatBtc() {
        // Assert
        assertEquals("1.0", subject.formatBtc(BigDecimal.valueOf(1L)))
        assertEquals("10,000.0", subject.formatBtc(BigDecimal.valueOf(10_000L)))
        assertEquals("100,000,000.0", subject.formatBtc(BigDecimal.valueOf(1e8.toLong())))
        assertEquals(
            "10,000,000,000,000.0",
            subject.formatBtc(BigDecimal.valueOf((100_000 * 1e8).toLong()))
        )
        assertEquals("0", subject.formatBtc(BigDecimal.valueOf(0)))
    }

    @Test
    fun `format BTC from Crypto Value`() {
        // Don't inline these, remove tests when method goes. The replacement has own tests.
        subject.format(CryptoValue.ZeroBtc) `should equal` "0"
        subject.format(CryptoValue.bitcoinFromMajor(1)) `should equal` "1.0"
        subject.format(CryptoValue.bitcoinFromMajor(10_000)) `should equal` "10,000.0"
        subject.format(CryptoValue.bitcoinFromMajor(21_000_000)) `should equal` "21,000,000.0"
    }

    @Test
    fun `format BCH from Crypto Value`() {
        subject.format(CryptoValue.ZeroBch) `should equal` "0"
        subject.format(CryptoValue.bitcoinCashFromMajor(1)) `should equal` "1.0"
        subject.format(CryptoValue.bitcoinCashFromMajor(10_000)) `should equal` "10,000.0"
        subject.format(CryptoValue.bitcoinCashFromMajor(21_000_000)) `should equal` "21,000,000.0"
    }

    @Test
    fun `format Ether from Crypto Value`() {
        subject.format(CryptoValue.ZeroEth) `should equal` "0"
        subject.format(CryptoValue.etherFromMajor(1)) `should equal` "1.0"
        subject.format(CryptoValue.etherFromMajor(10_000)) `should equal` "10,000.0"
        subject.format(CryptoValue.etherFromMajor(100_000_000)) `should equal` "100,000,000.0"
    }

    @Test
    fun formatBtcWithUnit() {
        // Assert
        assertEquals("1.0 BTC", subject.formatBtcWithUnit(BigDecimal.valueOf(1L)))
        assertEquals("10,000.0 BTC", subject.formatBtcWithUnit(BigDecimal.valueOf(10_000L)))
        assertEquals(
            "100,000,000.0 BTC",
            subject.formatBtcWithUnit(BigDecimal.valueOf(1e8.toLong()))
        )
        assertEquals(
            "10,000,000,000,000.0 BTC",
            subject.formatBtcWithUnit(BigDecimal.valueOf((100_000 * 1e8).toLong()))
        )
        assertEquals("0 BTC", subject.formatBtcWithUnit(BigDecimal.valueOf(0)))
    }

    @Test
    fun `formatWithUnit 0 BTC`() {
        subject.formatWithUnit(CryptoValue.ZeroBtc) `should equal` "0 BTC"
    }

    @Test
    fun `formatWithUnit BTC`() {
        subject.formatWithUnit(CryptoValue.bitcoinFromMajor(1)) `should equal` "1.0 BTC"
        subject.formatWithUnit(CryptoValue.bitcoinFromMajor(10_000)) `should equal` "10,000.0 BTC"
        subject.formatWithUnit(CryptoValue.bitcoinFromMajor(21_000_000)) `should equal` "21,000,000.0 BTC"
    }

    @Test
    fun `formatWithUnit BTC fractions`() {
        subject.formatWithUnit(CryptoValue.bitcoinFromSatoshis(1L)) `should equal` "0.00000001 BTC"
        subject.formatWithUnit(CryptoValue.bitcoinFromSatoshis(10L)) `should equal` "0.0000001 BTC"
        subject.formatWithUnit(CryptoValue.bitcoinFromSatoshis(100L)) `should equal` "0.000001 BTC"
        subject.formatWithUnit(CryptoValue.bitcoinFromSatoshis(1000L)) `should equal` "0.00001 BTC"
        subject.formatWithUnit(CryptoValue.bitcoinFromSatoshis(10000L)) `should equal` "0.0001 BTC"
        subject.formatWithUnit(CryptoValue.bitcoinFromSatoshis(100000L)) `should equal` "0.001 BTC"
        subject.formatWithUnit(CryptoValue.bitcoinFromSatoshis(1000000L)) `should equal` "0.01 BTC"
        subject.formatWithUnit(CryptoValue.bitcoinFromSatoshis(10000000L)) `should equal` "0.1 BTC"
        subject.formatWithUnit(CryptoValue.bitcoinFromSatoshis(120000000L)) `should equal` "1.2 BTC"
    }

    @Test
    fun `formatWithUnit 0 BCH`() {
        subject.formatWithUnit(CryptoValue.ZeroBch) `should equal` "0 BCH"
    }

    @Test
    fun `formatWithUnit BCH`() {
        subject.formatWithUnit(CryptoValue.bitcoinCashFromMajor(1)) `should equal` "1.0 BCH"
        subject.formatWithUnit(CryptoValue.bitcoinCashFromMajor(10_000)) `should equal` "10,000.0 BCH"
        subject.formatWithUnit(CryptoValue.bitcoinCashFromMajor(21_000_000)) `should equal` "21,000,000.0 BCH"
    }

    @Test
    fun `formatWithUnit BCH fractions`() {
        subject.formatWithUnit(CryptoValue.bitcoinCashFromSatoshis(1L)) `should equal` "0.00000001 BCH"
        subject.formatWithUnit(CryptoValue.bitcoinCashFromSatoshis(10L)) `should equal` "0.0000001 BCH"
        subject.formatWithUnit(CryptoValue.bitcoinCashFromSatoshis(100L)) `should equal` "0.000001 BCH"
        subject.formatWithUnit(CryptoValue.bitcoinCashFromSatoshis(1000L)) `should equal` "0.00001 BCH"
        subject.formatWithUnit(CryptoValue.bitcoinCashFromSatoshis(10000L)) `should equal` "0.0001 BCH"
        subject.formatWithUnit(CryptoValue.bitcoinCashFromSatoshis(100000L)) `should equal` "0.001 BCH"
        subject.formatWithUnit(CryptoValue.bitcoinCashFromSatoshis(1000000L)) `should equal` "0.01 BCH"
        subject.formatWithUnit(CryptoValue.bitcoinCashFromSatoshis(10000000L)) `should equal` "0.1 BCH"
        subject.formatWithUnit(CryptoValue.bitcoinCashFromSatoshis(120000000L)) `should equal` "1.2 BCH"
    }

    @Test
    fun `formatWithUnit 0 ETH`() {
        subject.formatWithUnit(CryptoValue.ZeroEth) `should equal` "0 ETH"
    }

    @Test
    fun `formatWithUnit ETH`() {
        subject.formatWithUnit(CryptoValue.etherFromMajor(1)) `should equal` "1.0 ETH"
        subject.formatWithUnit(CryptoValue.etherFromMajor(10_000)) `should equal` "10,000.0 ETH"
        subject.formatWithUnit(CryptoValue.etherFromMajor(1_000_000_000)) `should equal` "1,000,000,000.0 ETH"
    }

    @Test
    fun `formatWithUnit ETH fractions too small to display`() {
        val formatWithUnit =
            { wei: Long ->
                subject.formatWithUnit(
                    CryptoValue(CryptoCurrency.ETHER, wei.toBigInteger())
                )
            }
        formatWithUnit(1L) `should equal` "0 ETH"
        formatWithUnit(10L) `should equal` "0 ETH"
        formatWithUnit(100L) `should equal` "0 ETH"
        formatWithUnit(1_000L) `should equal` "0 ETH"
        formatWithUnit(10_000L) `should equal` "0 ETH"
        formatWithUnit(100_000L) `should equal` "0 ETH"
        formatWithUnit(1_000_000L) `should equal` "0 ETH"
        formatWithUnit(10_000_000L) `should equal` "0 ETH"
        formatWithUnit(100_000_000L) `should equal` "0 ETH"
        formatWithUnit(1_000_000_000L) `should equal` "0 ETH"
    }

    @Test
    fun `formatWithUnit ETH with tiny fractions - full precision`() {
        val formatWithUnit =
            { wei: Long ->
                subject.formatWithUnit(
                    CryptoValue(CryptoCurrency.ETHER, wei.toBigInteger()),
                    FormatPrecision.Full
                )
            }
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
        val formatWithUnit =
            { wei: Long ->
                subject.formatWithUnit(
                    CryptoValue(CryptoCurrency.ETHER, wei.toBigInteger())
                )
            }
        formatWithUnit(10_000_000_000L) `should equal` "0.00000001 ETH"
        formatWithUnit(100_000_000_000L) `should equal` "0.0000001 ETH"
        formatWithUnit(1_000_000_000_000L) `should equal` "0.000001 ETH"
        formatWithUnit(10_000_000_000_000L) `should equal` "0.00001 ETH"
        formatWithUnit(100_000_000_000_000L) `should equal` "0.0001 ETH"
        formatWithUnit(1_000_000_000_000_000L) `should equal` "0.001 ETH"
        formatWithUnit(10_000_000_000_000_000L) `should equal` "0.01 ETH"
        formatWithUnit(100_000_000_000_000_000L) `should equal` "0.1 ETH"
        formatWithUnit(1_200_000_000_000_000_000) `should equal` "1.2 ETH"
    }

    @Test
    fun formatSatoshi() {
        // Assert
        assertEquals("0.00000001", subject.formatSatoshi(1L))
        assertEquals("0.0001", subject.formatSatoshi(10_000L))
        assertEquals("100,000.0", subject.formatSatoshi((100_000 * 1e8).toLong()))
        assertEquals("1.0", subject.formatSatoshi(1e8.toLong()))
        assertEquals("0", subject.formatSatoshi(0L))
    }

    @Test
    fun formatEth() {
        // Assert
        assertEquals("1.0", subject.formatEth(BigDecimal.valueOf(1L)))
        assertEquals(
            "1,000,000,000,000,000,000.0",
            subject.formatEth(BigDecimal.valueOf(1_000_000_000_000_000_000L))
        )
        assertEquals("0", subject.formatEth(BigDecimal.valueOf(0L)))
    }

    @Test
    fun formatEthWithUnit() {
        // Assert
        assertEquals("1.0 ETH", subject.formatEthWithUnit(BigDecimal.valueOf(1L)))
        assertEquals(
            "1,000,000,000,000,000,000.0 ETH",
            subject.formatEthWithUnit(BigDecimal.valueOf(1_000_000_000_000_000_000L))
        )
        assertEquals("0 ETH", subject.formatEthWithUnit(BigDecimal.valueOf(0L)))
    }

    @Test
    fun formatEthShortWithUnit() {
        // Assert
        assertEquals("1.0 ETH", subject.formatEthShortWithUnit(BigDecimal.valueOf(1L)))
        assertEquals(
            "1,000,000,000,000,000,000.0 ETH",
            subject.formatEthWithUnit(BigDecimal.valueOf(1_000_000_000_000_000_000L))
        )
        assertEquals("0 ETH", subject.formatEthWithUnit(BigDecimal.valueOf(0L)))
    }

    @Test
    fun formatFiat() {
        // Assert
        assertEquals("100,000.00", subject.formatFiat(100_000L.usd()))
        assertEquals("0.00", subject.formatFiat(0.usd()))
    }

    @Test
    fun formatFiatWithSymbol() {
        // Assert
        assertEquals("$100,000.00", subject.formatFiatWithSymbol(100_000.00, "USD", Locale.US))
        assertEquals("$0.00", subject.formatFiatWithSymbol(0.0, "USD", Locale.US))
    }
}
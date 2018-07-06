package piuk.blockchain.androidcore.data.currency

import org.junit.Before
import java.math.BigDecimal
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatUtilTest {

    private lateinit var subject: CurrencyFormatUtil

    @Before
    fun setUp() {
        subject = CurrencyFormatUtil()
    }

    @Test
    fun `getBtcUnit`() {
        // Assert
        assertEquals("BTC", subject.getBtcUnit())
    }

    @Test
    fun `getBchUnit`() {
        // Assert
        assertEquals("BCH", subject.getBchUnit())
    }

    @Test
    fun `getEthUnit`() {
        // Assert
        assertEquals("ETH", subject.getEthUnit())
    }

    @Test
    fun `getBtcMaxFractionDigits`() {
        // Assert
        assertEquals(8, subject.getBtcMaxFractionDigits())
    }

    @Test
    fun `getBchMaxFractionDigits`() {
        // Assert
        assertEquals(8, subject.getBchMaxFractionDigits())
    }

    @Test
    fun `getEthMaxFractionDigits`() {
        // Assert
        assertEquals(18, subject.getEthMaxFractionDigits())
    }

    @Test
    fun `formatBtc`() {
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
    fun `formatBtcWithUnit`() {
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
    fun `formatSatoshi`() {
        // Assert
        assertEquals("0.00000001", subject.formatSatoshi(1L))
        assertEquals("0.0001", subject.formatSatoshi(10_000L))
        assertEquals("100,000.0", subject.formatSatoshi((100_000 * 1e8).toLong()))
        assertEquals("1.0", subject.formatSatoshi(1e8.toLong()))
        assertEquals("0", subject.formatSatoshi(0L))
    }

    @Test
    fun `formatEth`() {
        // Assert
        assertEquals("1.0", subject.formatEth(BigDecimal.valueOf(1L)))
        assertEquals(
            "1,000,000,000,000,000,000.0",
            subject.formatEth(BigDecimal.valueOf(1_000_000_000_000_000_000L))
        )
        assertEquals("0", subject.formatEth(BigDecimal.valueOf(0L)))
    }

    @Test
    fun `formatEthWithUnit`() {
        // Assert
        assertEquals("1.0 ETH", subject.formatEthWithUnit(BigDecimal.valueOf(1L)))
        assertEquals(
            "1,000,000,000,000,000,000.0 ETH",
            subject.formatEthWithUnit(BigDecimal.valueOf(1_000_000_000_000_000_000L))
        )
        assertEquals("0 ETH", subject.formatEthWithUnit(BigDecimal.valueOf(0L)))
    }

    @Test
    fun `formatEthShortWithUnit`() {
        // Assert
        assertEquals("1.0 ETH", subject.formatEthShortWithUnit(BigDecimal.valueOf(1L)))
        assertEquals(
            "1,000,000,000,000,000,000.0 ETH",
            subject.formatEthWithUnit(BigDecimal.valueOf(1_000_000_000_000_000_000L))
        )
        assertEquals("0 ETH", subject.formatEthWithUnit(BigDecimal.valueOf(0L)))
    }

    @Test
    fun `formatWei`() {
        // Assert
        assertEquals("0.000000000000000001", subject.formatWei(1L))
        assertEquals("1.0", subject.formatWei(1000000000000000000L))
        assertEquals("0", subject.formatWei(0L))
    }

    @Test
    fun `formatWeiWithUnit`() {
        // Assert
        assertEquals("0.000000000000000001 ETH", subject.formatWeiWithUnit(1L))
        assertEquals("1.0 ETH", subject.formatWeiWithUnit(1000000000000000000L))
        assertEquals("0 ETH", subject.formatWeiWithUnit(0L))
    }

    @Test
    fun `formatFiat`() {
        // Assert
        assertEquals("100,000.00", subject.formatFiat(BigDecimal.valueOf(100_000L), "USD"))
        assertEquals("0.00", subject.formatFiat(BigDecimal.valueOf(0L), "USD"))
    }

    @Test
    fun `formatFiatWithSymbol`() {
        // Assert
        assertEquals("$100,000.00", subject.formatFiatWithSymbol(100_000.00, "USD", Locale.US))
        assertEquals("$0.00", subject.formatFiatWithSymbol(0.0, "USD", Locale.US))
    }
}
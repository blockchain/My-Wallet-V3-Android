package piuk.blockchain.androidcore.data.currency

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Precision
import org.amshove.kluent.mock
import org.junit.Before
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurrencyFormatManagerTest {

    private lateinit var subject: CurrencyFormatManager
    private val currencyState: CurrencyState = mock()
    private val exchangeRateDataManager: ExchangeRateDataManager = mock()
    private val currencyFormatUtil: CurrencyFormatUtil = mock()
    private val prefsUtil: PrefsUtil = mock()
    private val locale = Locale.US

    @Before
    fun setUp() {
        subject = CurrencyFormatManager(
            currencyState,
            exchangeRateDataManager,
            prefsUtil,
            currencyFormatUtil,
            locale
        )
    }

    // region Current selected crypto currency state methods
    @Test
    fun `getCryptoMaxDecimalLength BTC`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)

        // Act
        val result = subject.getSelectedCoinMaxFractionDigits()

        // Assert
        assertEquals(8, result)
    }

    @Test
    fun `getCryptoMaxDecimalLength BCH`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BCH)

        // Act
        val result = subject.getSelectedCoinMaxFractionDigits()

        // Assert
        assertEquals(8, result)
    }

    @Test
    fun `getCryptoMaxDecimalLength ETH`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)

        // Act
        val result = subject.getSelectedCoinMaxFractionDigits()

        // Assert
        assertEquals(18, result)
    }

    @Test
    fun `getCryptoUnit BTC`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)

        // Act
        val result = subject.getSelectedCoinUnit()

        // Assert
        assertEquals("BTC", result)
    }

    @Test
    fun `getCryptoUnit BCH`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BCH)
        // Act
        val result = subject.getSelectedCoinUnit()

        // Assert
        assertEquals("BCH", result)
    }

    @Test
    fun `getCryptoUnit ETH`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)

        // Act
        val result = subject.getSelectedCoinUnit()

        // Assert
        assertEquals("ETH", result)
    }

    @Test
    fun `getConvertedCoinValue BTC default satoshi denomination`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)

        // Act
        // Assert
        assertTrue(
            BigDecimal.valueOf(0.00000001).compareTo(
                subject.getConvertedCoinValue(BigDecimal.valueOf(1L))
            ) == 0
        )

        assertTrue(
            BigDecimal.valueOf(1.0).compareTo(
                subject.getConvertedCoinValue(BigDecimal.valueOf(1e8.toLong()))
            ) == 0
        )
    }

    @Test
    fun `getConvertedCoinValue BTC satoshi denomination`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)

        // Act
        // Assert
        assertTrue(
            BigDecimal.valueOf(0.00000001).compareTo(
                subject.getConvertedCoinValue(
                    coinValue = BigDecimal.valueOf(1L),
                    convertBtcDenomination = BTCDenomination.SATOSHI
                )
            ) == 0
        )

        assertTrue(
            BigDecimal.valueOf(1.0).compareTo(
                subject.getConvertedCoinValue(
                    BigDecimal.valueOf(1e8.toLong()),
                    convertBtcDenomination = BTCDenomination.SATOSHI
                )
            ) == 0
        )
    }

    @Test
    fun `getConvertedCoinValue BTC btc denomination`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)

        // Act
        // Assert
        assertTrue(
            BigDecimal.valueOf(1L).compareTo(
                subject.getConvertedCoinValue(
                    coinValue = BigDecimal.valueOf(1L),
                    convertBtcDenomination = BTCDenomination.BTC
                )
            ) == 0
        )

        assertTrue(
            BigDecimal.valueOf(100_000_000L).compareTo(
                subject.getConvertedCoinValue(
                    BigDecimal.valueOf(100_000_000L),
                    convertBtcDenomination = BTCDenomination.BTC
                )
            ) == 0
        )
    }

    @Test
    fun `getConvertedCoinValue ETH eth denomination`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)

        // Act
        // Assert
        assertTrue(
            BigDecimal.valueOf(1L).compareTo(
                subject.getConvertedCoinValue(
                    coinValue = BigDecimal.valueOf(1L),
                    convertEthDenomination = ETHDenomination.ETH
                )
            ) == 0
        )

        assertTrue(
            BigDecimal.valueOf(100_000_000L).compareTo(
                subject.getConvertedCoinValue(
                    BigDecimal.valueOf(100_000_000L),
                    convertEthDenomination = ETHDenomination.ETH
                )
            ) == 0
        )
    }

    @Test
    fun `getConvertedCoinValue ETH wei denomination`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)

        // Act
        // Assert
        assertTrue(
            BigDecimal.valueOf(1L).compareTo(
                subject.getConvertedCoinValue(
                    coinValue = BigDecimal.valueOf(1000000000000000000L),
                    convertEthDenomination = ETHDenomination.WEI
                )
            ) == 0
        )

        assertTrue(
            BigDecimal.valueOf(0.000000000000000001).compareTo(
                subject.getConvertedCoinValue(
                    BigDecimal.valueOf(1L),
                    convertEthDenomination = ETHDenomination.WEI
                )
            ) == 0
        )
    }

    @Test
    fun `getFormattedSelectedCoinValue BTC`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)

        // Act
        // Assert
        assertEquals("0.00000001", subject.getFormattedSelectedCoinValue(BigInteger.valueOf(1L)))
    }

    @Test
    fun `getFormattedSelectedCoinValue ETH`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)

        // Act
        // Assert
        assertEquals("0.000000000000000002", subject.getFormattedSelectedCoinValue(BigInteger.valueOf(2L)))
    }

    @Test
    fun `getFormattedSelectedCoinValueWithUnit BTC`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BCH)

        // Act
        // Assert
        assertEquals("0.00000002 BCH", subject.getFormattedSelectedCoinValueWithUnit(BigInteger.valueOf(2L)))
    }

    @Test
    fun `getFormattedSelectedCoinValueWithUnit ETH`() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)

        // Act
        // Assert
        assertEquals("0.000000000000000002 ETH", subject.getFormattedSelectedCoinValueWithUnit(BigInteger.valueOf(2L)))
    }

    // endregion
}
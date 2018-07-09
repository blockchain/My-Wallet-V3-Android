package piuk.blockchain.androidcore.data.currency

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import piuk.blockchain.androidcore.RxTest
import piuk.blockchain.androidcore.utils.PrefsUtil

class CurrencyStateTest : RxTest() {

    private lateinit var subject: CurrencyState
    private val mockPrefs: PrefsUtil = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        subject = CurrencyState.getInstance()
    }

    @Test
    fun getSelectedCryptoCurrencyDefault() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name))
            .thenReturn(CryptoCurrencies.BTC.name)
        subject.init(mockPrefs)
        // Act

        // Assert
        Assert.assertEquals(
            subject.cryptoCurrency,
            CryptoCurrencies.BTC
        )
    }

    @Test
    fun getSelectedCryptoCurrencyEther() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name))
            .thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act

        // Assert
        Assert.assertEquals(
            subject.cryptoCurrency,
            CryptoCurrencies.ETHER
        )
    }

    @Test
    fun getSetSelectedCryptoCurrencyBtc() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name))
            .thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act
        subject.cryptoCurrency = CryptoCurrencies.BTC
        // Assert
        Assert.assertEquals(
            subject.cryptoCurrency,
            CryptoCurrencies.BTC
        )
    }

    @Test
    fun getSetSelectedCryptoCurrencyEther() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name))
            .thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act
        subject.cryptoCurrency = CryptoCurrencies.ETHER
        // Assert
        Assert.assertEquals(
            subject.cryptoCurrency,
            CryptoCurrencies.ETHER
        )
    }

    @Test
    fun isDisplayingCryptoDefault() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name))
            .thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act

        // Assert
        Assert.assertTrue(subject.isDisplayingCryptoCurrency)
    }

    @Test
    fun isDisplayingCryptoFalse() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name))
            .thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act
        subject.isDisplayingCryptoCurrency = false
        // Assert
        Assert.assertFalse(subject.isDisplayingCryptoCurrency)
    }

    @Test
    fun isDisplayingCryptoTrue() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name))
            .thenReturn(CryptoCurrencies.ETHER.name)
        subject.init(mockPrefs)
        // Act
        subject.isDisplayingCryptoCurrency = true
        // Assert
        Assert.assertTrue(subject.isDisplayingCryptoCurrency)
    }

    @Test
    fun toggleCryptoCurrency() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrencies.BTC.name))
            .thenReturn(CryptoCurrencies.BTC.name)
        subject.init(mockPrefs)
        // Act
        // Assert
        Assert.assertEquals(
            subject.cryptoCurrency,
            CryptoCurrencies.BTC
        )
        subject.toggleCryptoCurrency()
        Assert.assertEquals(
            subject.cryptoCurrency,
            CryptoCurrencies.ETHER
        )
        subject.toggleCryptoCurrency()
        Assert.assertEquals(
            subject.cryptoCurrency,
            CryptoCurrencies.BTC
        )
    }
}
package piuk.blockchain.androidcore.data.currency

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.junit.Test
import piuk.blockchain.androidcore.utils.PrefsUtil

class CurrencyStateTest {

    private val mockPrefs: PrefsUtil = mock()
    private val subject: CurrencyState = CurrencyState(mockPrefs)

    @Test
    fun getSelectedCryptoCurrencyDefault() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.BTC.name)
        // Act

        // Assert
        subject.cryptoCurrency `should be` CryptoCurrency.BTC
    }

    @Test
    fun getSelectedCryptoCurrencyEther() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.ETHER.name)
        // Act

        // Assert
        subject.cryptoCurrency `should be` CryptoCurrency.ETHER
    }

    @Test
    fun getSetSelectedCryptoCurrencyBtc() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.ETHER.name)
        // Act
        subject.cryptoCurrency = CryptoCurrency.BTC
        // Assert
        subject.cryptoCurrency `should be` CryptoCurrency.BTC
    }

    @Test
    fun getSetSelectedCryptoCurrencyEther() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.ETHER.name)
        // Act
        subject.cryptoCurrency = CryptoCurrency.ETHER
        // Assert
        subject.cryptoCurrency `should be` CryptoCurrency.ETHER
    }

    @Test
    fun isDisplayingCryptoDefault() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.ETHER.name)
        // Act

        // Assert
        subject.isDisplayingCryptoCurrency `should be` true
        subject.displayMode `should be` CurrencyState.DisplayMode.Crypto
    }

    @Test
    fun isDisplayingCryptoFalse() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.ETHER.name)
        // Act
        subject.isDisplayingCryptoCurrency = false
        // Assert
        subject.isDisplayingCryptoCurrency `should be` false
        subject.displayMode `should be` CurrencyState.DisplayMode.Fiat
    }

    @Test
    fun `fiat display mode`() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.ETHER.name)
        // Act
        subject.displayMode = CurrencyState.DisplayMode.Fiat
        // Assert
        subject.isDisplayingCryptoCurrency `should be` false
        subject.displayMode `should be` CurrencyState.DisplayMode.Fiat
    }

    @Test
    fun isDisplayingCryptoTrue() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.ETHER.name)
        // Act
        subject.isDisplayingCryptoCurrency = true
        // Assert
        subject.isDisplayingCryptoCurrency `should be` true
        subject.displayMode `should be` CurrencyState.DisplayMode.Crypto
    }

    @Test
    fun `crypto display mode`() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.ETHER.name)
        // Act
        subject.displayMode = CurrencyState.DisplayMode.Crypto
        // Assert
        subject.isDisplayingCryptoCurrency `should be` true
        subject.displayMode `should be` CurrencyState.DisplayMode.Crypto
    }

    @Test
    fun `get caches the value`() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.ETHER.name)
        // Act
        subject.cryptoCurrency `should be` CryptoCurrency.ETHER
        subject.cryptoCurrency `should be` CryptoCurrency.ETHER
        // Assert
        verify(mockPrefs).getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name)
        verifyNoMoreInteractions(mockPrefs)
    }

    @Test
    fun `sets the value and the local cache`() {
        // Arrange
        whenever(mockPrefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name))
            .thenReturn(CryptoCurrency.BTC.name)
        // Act
        subject.cryptoCurrency = CryptoCurrency.BCH
        subject.cryptoCurrency `should be` CryptoCurrency.BCH
        // Assert
        verify(mockPrefs).setValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BCH.name)
        verifyNoMoreInteractions(mockPrefs)
    }
}

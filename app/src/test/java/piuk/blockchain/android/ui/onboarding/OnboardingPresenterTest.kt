package piuk.blockchain.android.ui.onboarding

import android.content.Intent
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import org.amshove.kluent.shouldEqual
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper
import piuk.blockchain.android.ui.onboarding.OnboardingActivity.EXTRAS_EMAIL_ONLY
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import java.lang.IllegalStateException

class OnboardingPresenterTest {

    private lateinit var subject: OnboardingPresenter
    private val mockFingerprintHelper: FingerprintHelper = mock()
    private val mockAccessState: AccessState = mock()
    private val mockSettingsDataManager: SettingsDataManager = mock()
    private val mockActivity: OnboardingView = mock()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        subject =
            OnboardingPresenter(mockFingerprintHelper, mockAccessState, mockSettingsDataManager)
        subject.initView(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadySettingsFailureEmailOnly() {
        // Arrange
        val intent: Intent = mock()
        whenever(intent.getBooleanExtra(EXTRAS_EMAIL_ONLY, false)).thenReturn(true)
        whenever(intent.hasExtra(EXTRAS_EMAIL_ONLY)).thenReturn(true)
        whenever(mockActivity.pageIntent).thenReturn(intent)
        whenever(mockSettingsDataManager.getSettings()).thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(mockSettingsDataManager).getSettings()
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockActivity).pageIntent
        verify(mockActivity).showEmailPrompt()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyFingerprintHardwareAvailable() {
        // Arrange
        val mockSettings: Settings = mock()
        whenever(mockSettingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(mockFingerprintHelper.isHardwareDetected()).thenReturn(true)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockSettingsDataManager).getSettings()
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockFingerprintHelper).isHardwareDetected()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verify(mockActivity).pageIntent
        verify(mockActivity).showFingerprintPrompt()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onViewReadyNoFingerprintHardware() {
        // Arrange
        val mockSettings: Settings = mock()
        whenever(mockSettingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings))
        whenever(mockFingerprintHelper.isHardwareDetected()).thenReturn(false)
        // Act
        subject.onViewReady()
        // Assert
        verify(mockSettingsDataManager).getSettings()
        verifyNoMoreInteractions(mockSettingsDataManager)
        verify(mockFingerprintHelper).isHardwareDetected()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verify(mockActivity).pageIntent
        verify(mockActivity).showEmailPrompt()
        verifyNoMoreInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onEnableFingerprintClickedFingerprintEnrolled() {
        // Arrange
        val captor = argumentCaptor<String>()
        val pin = "1234"
        whenever(mockFingerprintHelper.isFingerprintAvailable()).thenReturn(true)
        whenever(mockAccessState.pin).thenReturn(pin)
        // Act
        subject.onEnableFingerprintClicked()
        // Assert
        verify(mockFingerprintHelper).isFingerprintAvailable()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verify(mockAccessState, times(3)).pin
        verifyNoMoreInteractions(mockAccessState)
        verify(mockActivity).showFingerprintDialog(captor.capture())
        verifyNoMoreInteractions(mockActivity)
        captor.firstValue shouldEqual pin
    }

    @Test(expected = IllegalStateException::class)
    @Throws(Exception::class)
    fun onEnableFingerprintClickedNoPinFound() {
        // Arrange
        whenever(mockFingerprintHelper.isFingerprintAvailable()).thenReturn(true)
        whenever(mockAccessState.pin).thenReturn(null)
        // Act
        subject.onEnableFingerprintClicked()
        // Assert
        verify(mockFingerprintHelper).isFingerprintAvailable()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verify(mockAccessState, times(3)).pin
        verifyNoMoreInteractions(mockAccessState)
        verifyZeroInteractions(mockActivity)
    }

    @Test
    @Throws(Exception::class)
    fun onEnableFingerprintClickedNoFingerprintEnrolled() {
        // Arrange
        whenever(mockFingerprintHelper.isFingerprintAvailable()).thenReturn(false)
        whenever(mockFingerprintHelper.isHardwareDetected()).thenReturn(true)
        // Act
        subject.onEnableFingerprintClicked()
        // Assert
        verify(mockFingerprintHelper).isFingerprintAvailable()
        verify(mockFingerprintHelper).isHardwareDetected()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verify(mockActivity).showEnrollFingerprintsDialog()
        verifyNoMoreInteractions(mockActivity)
        verifyZeroInteractions(mockAccessState)
    }

    @Test(expected = IllegalStateException::class)
    @Throws(Exception::class)
    fun onEnableFingerprintClickedNoHardwareMethodCalledAccidentally() {
        // Arrange
        whenever(mockFingerprintHelper.isFingerprintAvailable()).thenReturn(false)
        whenever(mockFingerprintHelper.isHardwareDetected()).thenReturn(false)
        // Act
        subject.onEnableFingerprintClicked()
        // Assert
        verify(mockFingerprintHelper).isFingerprintAvailable()
        verify(mockFingerprintHelper).isHardwareDetected()
        verifyNoMoreInteractions(mockFingerprintHelper)
        verifyZeroInteractions(mockActivity)
        verifyZeroInteractions(mockAccessState)
    }

    @Test
    @Throws(Exception::class)
    fun setFingerprintUnlockEnabledTrue() {
        // Arrange

        // Act
        subject.setFingerprintUnlockEnabled(true)
        // Assert
        verify(mockFingerprintHelper).setFingerprintUnlockEnabled(true)
        verifyNoMoreInteractions(mockFingerprintHelper)
    }

    @Test
    @Throws(Exception::class)
    fun setFingerprintUnlockEnabledFalse() {
        // Arrange

        // Act
        subject.setFingerprintUnlockEnabled(false)
        // Assert
        verify(mockFingerprintHelper).setFingerprintUnlockEnabled(false)
        verify(mockFingerprintHelper).clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)
        verifyNoMoreInteractions(mockFingerprintHelper)
    }

    @Test
    @Throws(Exception::class)
    fun getEmail() {
        // Arrange
        val email = "EMAIL"
        subject.email = email
        // Act
        val result = subject.getEmail()
        // Assert
        result shouldEqual email
    }
}

package piuk.blockchain.androidcore.data.settings

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.Observable
import junit.framework.TestCase.assertEquals
import okhttp3.ResponseBody
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.androidcore.RxTest

class SettingsServiceTest : RxTest() {

    private lateinit var subject: SettingsService
    private val settingsManager: SettingsManager = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        subject = SettingsService(settingsManager)
    }

    @Test
    fun getSettingsObservable() {
        val mockSettings = mock(Settings::class.java)
        whenever(settingsManager.info).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.getSettingsObservable().test()
        // Assert
        verify(settingsManager).info
        verifyNoMoreInteractions(settingsManager)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun initSettings() {
        // Arrange
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        // Act
        subject.initSettings(guid, sharedKey)
        // Assert
        verify(settingsManager).initSettings(guid, sharedKey)
    }

    @Test
    fun getSettings() {
        // Arrange
        val mockSettings = mock(Settings::class.java)
        whenever(settingsManager.info).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.getSettings().test()
        // Assert
        verify(settingsManager).info
        verifyNoMoreInteractions(settingsManager)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    fun updateEmail() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        val email = "EMAIL"
        whenever(settingsManager.updateSetting(SettingsManager.METHOD_UPDATE_EMAIL, email))
            .thenReturn(Observable.just(mockResponse))
        // Act
        val testObserver = subject.updateEmail(email).test()
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_UPDATE_EMAIL, email)
        verifyNoMoreInteractions(settingsManager)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockResponse, testObserver.values()[0])
    }

    @Test
    fun updateSms() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        val phoneNumber = "PHONE_NUMBER"
        whenever(settingsManager.updateSetting(SettingsManager.METHOD_UPDATE_SMS, phoneNumber))
            .thenReturn(Observable.just(mockResponse))
        // Act
        val testObserver = subject.updateSms(phoneNumber).test()
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_UPDATE_SMS, phoneNumber)
        verifyNoMoreInteractions(settingsManager)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockResponse, testObserver.values()[0])
    }

    @Test
    fun verifySms() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        val verificationCode = "VERIFICATION_CODE"
        whenever(settingsManager.updateSetting(SettingsManager.METHOD_VERIFY_SMS, verificationCode))
            .thenReturn(Observable.just(mockResponse))
        // Act
        val testObserver = subject.verifySms(verificationCode).test()
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_VERIFY_SMS, verificationCode)
        verifyNoMoreInteractions(settingsManager)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockResponse, testObserver.values()[0])
    }

    @Test
    fun updateTor() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        whenever(settingsManager.updateSetting(SettingsManager.METHOD_UPDATE_BLOCK_TOR_IPS, 1))
            .thenReturn(Observable.just(mockResponse))
        // Act
        val testObserver = subject.updateTor(true).test()
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_UPDATE_BLOCK_TOR_IPS, 1)
        verifyNoMoreInteractions(settingsManager)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockResponse, testObserver.values()[0])
    }

    @Test
    fun updateNotifications() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        val notificationType = 1337
        whenever(
            settingsManager.updateSetting(
                SettingsManager.METHOD_UPDATE_NOTIFICATION_TYPE,
                notificationType
            )
        ).thenReturn(Observable.just(mockResponse))
        // Act
        val testObserver = subject.updateNotifications(notificationType).test()
        // Assert
        verify(settingsManager).updateSetting(
            SettingsManager.METHOD_UPDATE_NOTIFICATION_TYPE,
            notificationType
        )
        verifyNoMoreInteractions(settingsManager)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockResponse, testObserver.values()[0])
    }

    @Test
    fun enableNotifications() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        whenever(
            settingsManager.updateSetting(
                SettingsManager.METHOD_UPDATE_NOTIFICATION_ON,
                SettingsManager.NOTIFICATION_ON
            )
        ).thenReturn(Observable.just(mockResponse))
        // Act
        val testObserver = subject.enableNotifications(true).test()
        // Assert
        verify(settingsManager).updateSetting(
            SettingsManager.METHOD_UPDATE_NOTIFICATION_ON,
            SettingsManager.NOTIFICATION_ON
        )
        verifyNoMoreInteractions(settingsManager)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockResponse, testObserver.values()[0])
    }

    @Test
    fun updateTwoFactor() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        val authType = 1337
        whenever(settingsManager.updateSetting(SettingsManager.METHOD_UPDATE_AUTH_TYPE, authType))
            .thenReturn(Observable.just(mockResponse))
        // Act
        val testObserver = subject.updateTwoFactor(authType).test()
        // Assert
        verify(settingsManager).updateSetting(SettingsManager.METHOD_UPDATE_AUTH_TYPE, authType)
        verifyNoMoreInteractions(settingsManager)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockResponse, testObserver.values()[0])
    }
}
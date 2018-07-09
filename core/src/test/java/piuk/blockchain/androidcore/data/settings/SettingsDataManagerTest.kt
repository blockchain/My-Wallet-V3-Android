package piuk.blockchain.androidcore.data.settings

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.Observable
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.settings.datastore.SettingsDataStore
import java.util.Arrays

class SettingsDataManagerTest : RxTest() {

    private lateinit var subject: SettingsDataManager
    private val settingsService: SettingsService = mock()
    private val settingsDataStore: SettingsDataStore = mock()
    private val rxBus: RxBus = mock()

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        subject = SettingsDataManager(settingsService, settingsDataStore, rxBus)
    }

    @Test
    @Throws(Exception::class)
    fun initSettings() {
        // Arrange
        val mockSettings = mock(Settings::class.java)
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.initSettings(guid, sharedKey).test()
        // Assert
        verify(settingsService).initSettings(guid, sharedKey)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun getSettings() {
        val mockSettings = mock(Settings::class.java)
        whenever(settingsDataStore.getSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.getSettings().test()
        // Assert
        verify(settingsDataStore).getSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun updateEmail() {
        // Arrange
        val email = "EMAIL"
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.updateEmail(email)).thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.updateEmail(email).test()
        // Assert
        verify(settingsService).updateEmail(email)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun updateSms() {
        // Arrange
        val phoneNumber = "PHONE_NUMBER"
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.updateSms(phoneNumber)).thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.updateSms(phoneNumber).test()
        // Assert
        verify(settingsService).updateSms(phoneNumber)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun verifySms() {
        // Arrange
        val verificationCode = "VERIFICATION_CODE"
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.verifySms(verificationCode))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.verifySms(verificationCode).test()
        // Assert
        verify(settingsService).verifySms(verificationCode)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun updateTor() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.updateTor(true)).thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.updateTor(true).test()
        // Assert
        verify(settingsService).updateTor(true)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun updateTwoFactor() {
        // Arrange
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        val authType = 1337
        whenever(settingsService.updateTwoFactor(authType))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.updateTwoFactor(authType).test()
        // Assert
        verify(settingsService).updateTwoFactor(authType)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun enableNotificationNoneRegistered() {
        // Arrange
        val notifications = emptyList<Int>()
        val notificationType = SettingsManager.NOTIFICATION_TYPE_SMS
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.enableNotifications(true))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsService.updateNotifications(notificationType))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.enableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).enableNotifications(true)
        verify(settingsService).updateNotifications(notificationType)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun enableNotificationEmailRegistered() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_SMS
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.enableNotifications(true))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.enableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).enableNotifications(true)
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun enableNotificationSmsRegistered() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.enableNotifications(true))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.enableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).enableNotifications(true)
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_ALL)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun enableNotificationAllEnabled() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_ALL)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.enableNotifications(true))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.enableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).enableNotifications(true)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun disableNotificationNoneRegistered() {
        // Arrange
        val notifications = emptyList<Int>()
        val notificationType = SettingsManager.NOTIFICATION_TYPE_SMS
        val mockSettings = mock(Settings::class.java)
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun disableNotificationAllRegistered() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_ALL)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun disableNotificationBothRegistered() {
        // Arrange
        val notifications = listOf(
            SettingsManager.NOTIFICATION_TYPE_EMAIL,
            SettingsManager.NOTIFICATION_TYPE_SMS
        )
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_SMS)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun disableNotificationOneRegisteredMatchesPassed() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_EMAIL)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockResponse = mock(ResponseBody::class.java)
        val mockSettings = mock(Settings::class.java)
        whenever(settingsService.enableNotifications(false))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsService.updateNotifications(SettingsManager.NOTIFICATION_TYPE_NONE))
            .thenReturn(Observable.just(mockResponse))
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verify(settingsService).enableNotifications(false)
        verify(settingsService).updateNotifications(SettingsManager.NOTIFICATION_TYPE_NONE)
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun disableNotificationOneRegisteredDoesNotMatchPassed() {
        // Arrange
        val notifications = listOf(SettingsManager.NOTIFICATION_TYPE_SMS)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockSettings = mock(Settings::class.java)
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }

    @Test
    @Throws(Exception::class)
    fun disableNotificationEdgeCase() {
        // Arrange
        val notifications = Arrays.asList(1337, 1338)
        val notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL
        val mockSettings = mock(Settings::class.java)
        whenever(settingsDataStore.fetchSettings()).thenReturn(Observable.just(mockSettings))
        // Act
        val testObserver = subject.disableNotification(notificationType, notifications).test()
        // Assert
        verifyNoMoreInteractions(settingsService)
        verify(settingsDataStore).fetchSettings()
        verifyNoMoreInteractions(settingsDataStore)
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        assertEquals(mockSettings, testObserver.values()[0])
    }
}
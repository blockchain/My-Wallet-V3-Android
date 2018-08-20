package com.blockchain.kycui.mobile.entry

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.nabu.mapFromMetadata
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.utils.extensions.toMoshiSerialisedString

class KycMobileEntryPresenterTest {

    private lateinit var subject: KycMobileEntryPresenter
    private val view: KycMobileEntryView = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val metadataManager: MetadataManager = mock()
    private val settingsDataManager: SettingsDataManager = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycMobileEntryPresenter(
            metadataManager,
            nabuDataManager,
            settingsDataManager
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady no phone number found, should not attempt to update UI`() {
        // Arrange
        val settings = Settings()
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(view.phoneNumber).thenReturn(Observable.empty())
        // Act
        subject.onViewReady()
        // Assert
        verify(view).phoneNumber
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady phone number found, should attempt to update UI`() {
        // Arrange
        val settings: Settings = mock()
        val phoneNumber = "+1234567890"
        whenever(settings.smsNumber).thenReturn(phoneNumber)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(view.phoneNumber).thenReturn(Observable.empty())
        // Act
        subject.onViewReady()
        // Assert
        verify(view).preFillPhoneNumber(phoneNumber)
    }

    @Test
    fun `onViewReady phone number entered, too short to enable button`() {
        // Arrange
        val settings: Settings = mock()
        val phoneNumber = "+1234"
        whenever(settings.smsNumber).thenReturn(phoneNumber)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.empty())
        whenever(view.phoneNumber).thenReturn(Observable.just(phoneNumber))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).setButtonEnabled(false)
    }

    @Test
    fun `onViewReady phone number entered, should enable button`() {
        // Arrange
        val phoneNumber = "+1234567890"
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.empty())
        whenever(view.phoneNumber).thenReturn(Observable.just(phoneNumber))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).setButtonEnabled(true)
    }

    @Test
    fun `onViewReady phone number observable exception, should finish page`() {
        // Arrange
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.empty())
        whenever(view.phoneNumber).thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(view).finishPage()
    }

    @Test
    fun `onContinueClicked throws exception, should trigger toast`() {
        // Arrange
        whenever(view.phoneNumber).thenReturn(Observable.error { Throwable() })
        // Act
        subject.onContinueClicked()
        // Assert
        verify(view).showErrorToast(any())
    }

    @Test
    fun `onContinueClicked, should sanitise input and progress page`() {
        // Arrange
        val phoneNumber = "+1 (234) 567-890"
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(view.phoneNumber).thenReturn(Observable.just(phoneNumber))
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiSerialisedString())))
        whenever(nabuDataManager.addMobileNumber(eq(offlineToken.mapFromMetadata()), any()))
            .thenReturn(Completable.complete())
        // Act
        subject.onContinueClicked()
        // Assert
        verify(nabuDataManager).addMobileNumber(offlineToken.mapFromMetadata(), "+1234567890")
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueSignUp()
    }
}
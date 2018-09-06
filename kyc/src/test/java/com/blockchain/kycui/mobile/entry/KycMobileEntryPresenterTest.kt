package com.blockchain.kycui.mobile.entry

import com.blockchain.android.testutils.rxInit
import com.blockchain.getBlankNabuUser
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kycui.mobile.entry.models.PhoneDisplayModel
import com.blockchain.kycui.mobile.entry.models.PhoneNumber
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.serialization.toMoshiJson
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class KycMobileEntryPresenterTest {

    private lateinit var subject: KycMobileEntryPresenter
    private val view: KycMobileEntryView = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val metadataManager: MetadataManager = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycMobileEntryPresenter(settingsDataManager, nabuDataManager, metadataManager)
        subject.initView(view)
    }

    @Test
    fun `onViewReady no phone number found, should not attempt to update UI`() {
        // Arrange
        val settings = Settings()
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(view.uiStateObservable).thenReturn(Observable.empty())
        // Act
        subject.onViewReady()
        // Assert
        verify(view).uiStateObservable
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady phone number found, should attempt to update UI`() {
        // Arrange
        val settings: Settings = mock()
        val phoneNumber = "+1234567890"
        whenever(settings.smsNumber).thenReturn(phoneNumber)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(view.uiStateObservable).thenReturn(Observable.empty())
        // Act
        subject.onViewReady()
        // Assert
        verify(view).preFillPhoneNumber(phoneNumber)
    }

    @Test
    fun `onViewReady, should sanitise input and progress page`() {
        // Arrange
        val phoneNumber = "+1 (234) 567-890"
        val phoneNumberSanitized = "+1234567890"
        val publishSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.empty())
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(settingsDataManager.updateSms(phoneNumberSanitized)).thenReturn(Observable.empty())
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val jwt = "JWT"
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(nabuDataManager.updateUserWalletInfo(offlineToken.mapFromMetadata(), jwt))
            .thenReturn(Single.just(getBlankNabuUser()))
        // Act
        subject.onViewReady()
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        // Assert
        verify(settingsDataManager).updateSms(phoneNumberSanitized)
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueSignUp(PhoneDisplayModel(phoneNumber, phoneNumberSanitized))
    }

    @Test
    fun `onViewReady, should throw exception and resubscribe for next event`() {
        // Arrange
        val phoneNumber = "+1 (234) 567-890"
        val phoneNumberSanitized = "+1234567890"
        val publishSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.empty())
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(settingsDataManager.updateSms(phoneNumberSanitized))
            .thenReturn(Observable.error { Throwable() })
            .thenReturn(Observable.empty())
        // Act
        subject.onViewReady()
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        // Assert
        verify(view, times(2)).showProgressDialog()
        verify(view, times(2)).dismissProgressDialog()
        verify(view).showErrorToast(any())
        verify(view).continueSignUp(PhoneDisplayModel(phoneNumber, phoneNumberSanitized))
    }

    @Test
    fun `onViewReady, should throw exception and display toast`() {
        // Arrange
        val phoneNumber = "+1 (234) 567-890"
        val phoneNumberSanitized = "+1234567890"
        val publishSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.empty())
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(settingsDataManager.updateSms(phoneNumberSanitized))
            .thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
    }
}
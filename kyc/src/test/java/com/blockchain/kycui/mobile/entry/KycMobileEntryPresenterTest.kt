package com.blockchain.kycui.mobile.entry

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.nabu.NabuApiException
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.kycui.mobile.entry.models.PhoneDisplayModel
import com.blockchain.kycui.mobile.entry.models.PhoneNumber
import com.blockchain.serialization.toMoshiJson
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.MediaType
import okhttp3.ResponseBody
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import retrofit2.Response

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
        val offlineToken = NabuCredentialsMetadata("", "")
        val publishSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.empty())
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.addMobileNumber(eq(offlineToken.mapFromMetadata()), any()))
            .thenReturn(Completable.complete())
        // Act
        subject.onViewReady()
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        // Assert
        verify(nabuDataManager).addMobileNumber(
            offlineToken.mapFromMetadata(),
            phoneNumberSanitized
        )
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueSignUp(PhoneDisplayModel(phoneNumber, phoneNumberSanitized))
    }

    @Test
    fun `onViewReady, should throw exception and resubscribe for next event`() {
        // Arrange
        val phoneNumber = "+1 (234) 567-890"
        val phoneNumberSanitized = "+1234567890"
        val offlineToken = NabuCredentialsMetadata("", "")
        val publishSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.empty())
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.addMobileNumber(eq(offlineToken.mapFromMetadata()), any()))
            .thenReturn(Completable.error { Throwable() })
            .thenReturn(Completable.complete())
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
    fun `onViewReady, should throw AlreadyRegistered exception and display dialog`() {
        // Arrange
        val phoneNumber = "+1 (234) 567-890"
        val offlineToken = NabuCredentialsMetadata("", "")
        val publishSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.empty())
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val responseBody =
            ResponseBody.create(
                MediaType.parse("application/json"),
                "{}"
            )
        whenever(nabuDataManager.addMobileNumber(eq(offlineToken.mapFromMetadata()), any()))
            .thenReturn(Completable.error {
                NabuApiException.fromResponseBody(
                    Response.error<Unit>(409, responseBody)
                )
            })
        // Act
        subject.onViewReady()
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).displayErrorDialog(any())
    }

    @Test
    fun `onViewReady, should throw exception and display toast`() {
        // Arrange
        val phoneNumber = "+1 (234) 567-890"
        val offlineToken = NabuCredentialsMetadata("", "")
        val publishSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.empty())
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.addMobileNumber(eq(offlineToken.mapFromMetadata()), any()))
            .thenReturn(Completable.error { Throwable() })
        // Act
        subject.onViewReady()
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
    }
}
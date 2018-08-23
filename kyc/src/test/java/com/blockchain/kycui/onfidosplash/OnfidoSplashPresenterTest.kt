package com.blockchain.kycui.onfidosplash

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.datamanagers.onfido.OnfidoDataManager
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.kyc.models.onfido.ApplicantResponse
import com.blockchain.serialization.toMoshiJson
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager

class OnfidoSplashPresenterTest {

    private lateinit var subject: OnfidoSplashPresenter
    private val metadataManager: MetadataManager = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val onfidoDataManager: OnfidoDataManager = mock()
    private val view: OnfidoSplashView = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = OnfidoSplashPresenter(
            metadataManager,
            nabuDataManager,
            onfidoDataManager
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady click triggers request, should load next page`() {
        // Arrange
        val publishSubject = PublishSubject.create<Unit>()
        whenever(view.uiState).thenReturn(publishSubject)
        val apiKey = "API_KEY"
        val nabuUser = NabuUser("", "", "", "", false, null, UserState.Created, KycState.None)
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.getOnfidoApiKey(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(apiKey))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(nabuUser))
        val applicantResponse = ApplicantResponse("12345", "", false, "", "", "")
        whenever(
            onfidoDataManager.createApplicant(
                nabuUser.firstName!!,
                nabuUser.lastName!!,
                apiKey
            )
        ).thenReturn(Single.just(applicantResponse))
        // Act
        subject.onViewReady()
        publishSubject.onNext(Unit)
        // Assert
        verify(view).showProgressDialog(true)
        verify(view).dismissProgressDialog()
        verify(view).continueToOnfido(apiKey, applicantResponse.id)
    }

    @Test
    fun `onViewReady, should throw exception and resubscribe for next event`() {
        // Arrange
        val publishSubject = PublishSubject.create<Unit>()
        whenever(view.uiState).thenReturn(publishSubject)
        val apiKey = "API_KEY"
        val nabuUser = NabuUser("", "", "", "", false, null, UserState.Created, KycState.None)
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.getOnfidoApiKey(offlineToken.mapFromMetadata()))
            .thenReturn(Single.error { Throwable() })
            .thenReturn(Single.just(apiKey))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(nabuUser))
        val applicantResponse = ApplicantResponse("12345", "", false, "", "", "")
        whenever(
            onfidoDataManager.createApplicant(
                nabuUser.firstName!!,
                nabuUser.lastName!!,
                apiKey
            )
        ).thenReturn(Single.just(applicantResponse))
        // Act
        subject.onViewReady()
        publishSubject.onNext(Unit)
        publishSubject.onNext(Unit)
        // Assert
        verify(view, times(2)).showProgressDialog(true)
        verify(view, times(2)).dismissProgressDialog()
        verify(view).showErrorToast(any())
        verify(view).continueToOnfido(apiKey, applicantResponse.id)
    }

    @Test
    fun `submitVerification should continue page`() {
        // Arrange
        val applicantId = "APPLICANT_ID"
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(
            nabuDataManager.submitOnfidoVerification(
                offlineToken.mapFromMetadata(),
                applicantId
            )
        )
            .thenReturn(Completable.complete())
        // Act
        subject.submitVerification(applicantId)
        // Assert
        verify(view).showProgressDialog(false)
        verify(view).dismissProgressDialog()
        verify(view).continueToCompletion()
    }

    @Test
    fun `submitVerification results in error should throw toast`() {
        // Arrange
        val applicantId = "APPLICANT_ID"
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(
            nabuDataManager.submitOnfidoVerification(
                offlineToken.mapFromMetadata(),
                applicantId
            )
        )
            .thenReturn(Completable.error { Throwable() })
        // Act
        subject.submitVerification(applicantId)
        // Assert
        verify(view).showProgressDialog(false)
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
    }
}

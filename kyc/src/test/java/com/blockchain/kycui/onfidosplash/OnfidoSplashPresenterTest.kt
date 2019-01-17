package com.blockchain.kycui.onfidosplash

import com.blockchain.android.testutils.rxInit
import com.blockchain.getBlankNabuUser
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.datamanagers.onfido.OnfidoDataManager
import com.blockchain.kyc.models.nabu.SupportedDocuments
import com.blockchain.kyc.models.onfido.ApplicantResponse
import com.blockchain.nabu.NabuToken
import com.blockchain.validOfflineToken
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class OnfidoSplashPresenterTest {

    private lateinit var subject: OnfidoSplashPresenter
    private val nabuToken: NabuToken = mock()
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
            nabuToken,
            nabuDataManager,
            onfidoDataManager
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady click triggers request, should load next page`() {
        // Arrange
        val publishSubject = PublishSubject.create<String>()
        whenever(view.uiState).thenReturn(publishSubject)
        val apiKey = "API_KEY"
        val nabuUser = getBlankNabuUser()
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(nabuDataManager.getOnfidoApiKey(validOfflineToken))
            .thenReturn(Single.just(apiKey))
        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(nabuUser))
        val applicantResponse = ApplicantResponse("12345", "", false, "", "", "")
        whenever(
            onfidoDataManager.createApplicant(
                nabuUser.firstName!!,
                nabuUser.lastName!!,
                apiKey
            )
        ).thenReturn(Single.just(applicantResponse))
        whenever(nabuDataManager.getSupportedDocuments(validOfflineToken, "US"))
            .thenReturn(Single.just(listOf(SupportedDocuments.NATIONAL_IDENTITY_CARD)))
        // Act
        subject.onViewReady()
        publishSubject.onNext("US")
        // Assert
        verify(view).showProgressDialog(true)
        verify(view).dismissProgressDialog()
        verify(view).continueToOnfido(apiKey, applicantResponse.id, listOf(SupportedDocuments.NATIONAL_IDENTITY_CARD))
    }

    @Test
    fun `onViewReady, should throw exception and resubscribe for next event`() {
        // Arrange
        val publishSubject = PublishSubject.create<String>()
        whenever(view.uiState).thenReturn(publishSubject)
        val apiKey = "API_KEY"
        val nabuUser = getBlankNabuUser()
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(nabuDataManager.getOnfidoApiKey(validOfflineToken))
            .thenReturn(Single.error { Throwable() })
            .thenReturn(Single.just(apiKey))
        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(nabuUser))
        val applicantResponse = ApplicantResponse("12345", "", false, "", "", "")
        whenever(
            onfidoDataManager.createApplicant(
                nabuUser.firstName!!,
                nabuUser.lastName!!,
                apiKey
            )
        ).thenReturn(Single.just(applicantResponse))
        whenever(nabuDataManager.getSupportedDocuments(validOfflineToken, "US"))
            .thenReturn(Single.just(listOf(SupportedDocuments.NATIONAL_IDENTITY_CARD)))
        // Act
        subject.onViewReady()
        publishSubject.onNext("US")
        publishSubject.onNext("US")
        // Assert
        verify(view, times(2)).showProgressDialog(true)
        verify(view, times(2)).dismissProgressDialog()
        verify(view).showErrorToast(any())
        verify(view).continueToOnfido(apiKey, applicantResponse.id, listOf(SupportedDocuments.NATIONAL_IDENTITY_CARD))
    }

    @Test
    fun `submitVerification should continue page`() {
        // Arrange
        val applicantId = "APPLICANT_ID"
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(
            nabuDataManager.submitOnfidoVerification(
                validOfflineToken,
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(
            nabuDataManager.submitOnfidoVerification(
                validOfflineToken,
                applicantId
            )
        ).thenReturn(Completable.error { Throwable() })
        // Act
        subject.submitVerification(applicantId)
        // Assert
        verify(view).showProgressDialog(false)
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
    }
}

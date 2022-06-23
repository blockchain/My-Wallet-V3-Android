package piuk.blockchain.android.ui.kyc.mobile.validation

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.NabuUserSync
import com.blockchain.nabu.datamanagers.kyc.KycDataManager
import com.blockchain.nabu.models.responses.nabu.KycQuestionnaireNode
import com.blockchain.outcome.Outcome
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.subjects.PublishSubject
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.ui.kyc.mobile.entry.models.PhoneVerificationModel
import piuk.blockchain.android.ui.kyc.mobile.validation.models.VerificationCode
import piuk.blockchain.android.ui.kyc.questionnaire.toMutableNode
import piuk.blockchain.androidcore.data.settings.PhoneNumber
import piuk.blockchain.androidcore.data.settings.PhoneNumberUpdater

class KycMobileValidationPresenterTest {

    private lateinit var subject: KycMobileValidationPresenter
    private val view: KycMobileValidationView = mock()
    private val phoneNumberUpdater: PhoneNumberUpdater = mock()
    private val nabuUserSync: NabuUserSync = mock {
        on { syncUser() }.thenReturn(Completable.complete())
    }
    private val kycDataManager: KycDataManager = mock {
        onBlocking { getQuestionnaire() }.thenReturn(Outcome.Success(emptyList()))
    }

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computationTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycMobileValidationPresenter(
            nabuUserSync,
            phoneNumberUpdater,
            kycDataManager,
            mock()
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady, should check for questionnaire and navigate to questionnaire if there's questions to be answered`() = runBlocking {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val verificationCode = VerificationCode("VERIFICATION_CODE")
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(view.resendObservable).thenReturn(noResend())
        whenever(phoneNumberUpdater.verifySms(verificationCode.code))
            .thenReturn(Single.just(phoneNumberSanitized))
        val nodes = listOf(
            KycQuestionnaireNode.Selection("s1", "text1", emptyList(), false),
            KycQuestionnaireNode.Selection("s2", "text2", emptyList(), false),
        )
        whenever(kycDataManager.getQuestionnaire()).thenReturn(Outcome.Success(nodes))

        // Act
        subject.onViewReady()
        publishSubject.onNext(
            PhoneVerificationModel(
                phoneNumberSanitized,
                verificationCode
            ) to Unit
        )
        // Assert
        verify(nabuUserSync).syncUser()
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).navigateToQuestionnaire(nodes.toMutableNode())
    }

    @Test
    fun `onViewReady, should check for questionnaire and navigate to veriff if there are no questions to be answered`() {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val verificationCode = VerificationCode("VERIFICATION_CODE")
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(view.resendObservable).thenReturn(noResend())
        whenever(phoneNumberUpdater.verifySms(verificationCode.code))
            .thenReturn(Single.just(phoneNumberSanitized))
        // Act
        subject.onViewReady()
        publishSubject.onNext(
            PhoneVerificationModel(
                phoneNumberSanitized,
                verificationCode
            ) to Unit
        )
        // Assert
        verify(nabuUserSync).syncUser()
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).navigateToVeriff()
    }

    @Test
    fun `on resend`() {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        val resendSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(view.resendObservable).thenReturn(resendSubject)
        whenever(phoneNumberUpdater.updateSms(any()))
            .thenReturn(Single.just(phoneNumberSanitized))
        // Act
        subject.onViewReady()
        resendSubject.onNext(
            PhoneNumber(
                phoneNumberSanitized
            ) to Unit
        )
        // Assert
        verify(phoneNumberUpdater).updateSms(argThat { sanitized == phoneNumberSanitized })
        verify(view).theCodeWasResent()
        verify(nabuUserSync).syncUser()
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view, never()).navigateToVeriff()
    }

    @Test
    fun `onViewReady, should throw exception and resubscribe for next event`() = runBlocking {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val verificationCode = VerificationCode("VERIFICATION_CODE")
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(view.resendObservable).thenReturn(noResend())
        whenever(phoneNumberUpdater.verifySms(verificationCode.code))
            .thenReturn(Single.just(phoneNumberSanitized))
        whenever(nabuUserSync.syncUser())
            .thenReturn(Completable.error { Throwable() })
            .thenReturn(Completable.complete())
        whenever(kycDataManager.getQuestionnaire()).thenReturn(Outcome.Success(emptyList())).thenReturn(Outcome.Success(emptyList()))
        val verificationModel = PhoneVerificationModel(phoneNumberSanitized, verificationCode)

        // Act
        subject.onViewReady()
        publishSubject.onNext(verificationModel to Unit)
        publishSubject.onNext(verificationModel to Unit)
        // Assert
        verify(view, times(2)).showProgressDialog()
        verify(view, times(2)).dismissProgressDialog()
        verify(nabuUserSync, times(2)).syncUser()
        verify(view).displayErrorDialog(any())
        verify(view).navigateToVeriff()
    }

    @Test
    fun `onViewReady, should throw exception and display error dialog`() {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val verificationCode = VerificationCode("VERIFICATION_CODE")
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(view.resendObservable).thenReturn(noResend())
        whenever(phoneNumberUpdater.verifySms(verificationCode.code))
            .thenReturn(Single.just(phoneNumberSanitized))
        whenever(nabuUserSync.syncUser())
            .thenReturn(Completable.error { Throwable() })
        // Act
        subject.onViewReady()
        publishSubject.onNext(
            PhoneVerificationModel(
                phoneNumberSanitized,
                verificationCode
            ) to Unit
        )
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).displayErrorDialog(any())
        verify(nabuUserSync).syncUser()
    }
}

private fun noResend(): Observable<Pair<PhoneNumber, Unit>> = Observable.never()

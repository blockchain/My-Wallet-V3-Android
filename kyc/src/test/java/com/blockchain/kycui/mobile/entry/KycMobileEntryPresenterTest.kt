package com.blockchain.kycui.mobile.entry

import com.blockchain.android.testutils.rxInit
import com.blockchain.nabu.NabuUserSync
import com.blockchain.kycui.mobile.entry.models.PhoneDisplayModel
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.`it returns`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.settings.PhoneNumber
import piuk.blockchain.androidcore.data.settings.PhoneNumberUpdater

class KycMobileEntryPresenterTest {

    private lateinit var subject: KycMobileEntryPresenter
    private val view: KycMobileEntryView = mock()
    private val phoneNumberUpdater: PhoneNumberUpdater = mock()
    private val nabuUserSync: NabuUserSync = mock {
        on { syncUser() } `it returns` Completable.complete()
    }

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycMobileEntryPresenter(phoneNumberUpdater, nabuUserSync)
        subject.initView(view)
    }

    @Test
    fun `onViewReady no phone number found, should not attempt to update UI`() {
        // Arrange
        whenever(phoneNumberUpdater.smsNumber()).thenReturn(Single.just(""))
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
        val phoneNumber = "+1234567890"
        whenever(phoneNumberUpdater.smsNumber()).thenReturn(Single.just(phoneNumber))
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
        whenever(phoneNumberUpdater.smsNumber()).thenReturn(Single.just(""))
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(phoneNumberUpdater.updateSms(any())).thenReturn(Single.just("+1234567890"))
        // Act
        subject.onViewReady()
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        // Assert
        verify(phoneNumberUpdater).updateSms(argThat { sanitized == "+1234567890" })
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueSignUp(PhoneDisplayModel(phoneNumber, phoneNumberSanitized))
        verify(nabuUserSync).syncUser()
    }

    @Test
    fun `onViewReady, should throw exception and resubscribe for next event`() {
        // Arrange
        val phoneNumber = "+1 (234) 567-890"
        val phoneNumberSanitized = "+1234567890"
        val publishSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(phoneNumberUpdater.smsNumber()).thenReturn(Single.just(""))
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(phoneNumberUpdater.updateSms(any()))
            .thenReturn(Single.error { Throwable() })
            .thenReturn(Single.just("+1234567890"))
        // Act
        subject.onViewReady()
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        // Assert
        verify(phoneNumberUpdater, times(2)).updateSms(argThat { sanitized == "+1234567890" })
        verify(view, times(2)).showProgressDialog()
        verify(view, times(2)).dismissProgressDialog()
        verify(view).showErrorToast(any())
        verify(view).continueSignUp(PhoneDisplayModel(phoneNumber, phoneNumberSanitized))
        verify(nabuUserSync).syncUser()
    }

    @Test
    fun `onViewReady, should throw exception and display toast`() {
        // Arrange
        val phoneNumber = "+1 (234) 567-890"
        val publishSubject = PublishSubject.create<Pair<PhoneNumber, Unit>>()
        whenever(phoneNumberUpdater.smsNumber()).thenReturn(Single.just(""))
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(phoneNumberUpdater.updateSms(any()))
            .thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        publishSubject.onNext(PhoneNumber(phoneNumber) to Unit)
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
    }
}
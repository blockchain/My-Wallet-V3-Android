package com.blockchain.kycui.mobile.validation

import com.blockchain.android.testutils.rxInit
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kycui.mobile.entry.models.PhoneVerificationModel
import com.blockchain.kycui.mobile.validation.models.VerificationCode
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.serialization.toMoshiJson
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
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

class KycMobileValidationPresenterTest {

    private lateinit var subject: KycMobileValidationPresenter
    private val view: KycMobileValidationView = mock()
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
        subject = KycMobileValidationPresenter(
            metadataManager,
            nabuDataManager,
            settingsDataManager
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady, should progress page`() {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val jwt = "JWT"
        val verificationCode = VerificationCode("VERIFICATION_CODE")
        val offlineToken = NabuCredentialsMetadata("", "")
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(settingsDataManager.verifySms(verificationCode.code))
            .thenReturn(Observable.just(Settings()))
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(
            nabuDataManager.updateUserWalletInfo(
                offlineToken.mapFromMetadata(),
                jwt
            )
        ).thenReturn(Single.just(getNabuUser()))
        // Act
        subject.onViewReady()
        publishSubject.onNext(
            PhoneVerificationModel(
                phoneNumberSanitized,
                verificationCode
            ) to Unit
        )
        // Assert
        verify(nabuDataManager).updateUserWalletInfo(
            offlineToken.mapFromMetadata(),
            jwt
        )
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).continueSignUp()
    }

    @Test
    fun `onViewReady, should throw exception and resubscribe for next event`() {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val jwt = "JWT"
        val verificationCode = VerificationCode("VERIFICATION_CODE")
        val offlineToken = NabuCredentialsMetadata("", "")
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(settingsDataManager.verifySms(verificationCode.code))
            .thenReturn(Observable.just(Settings()))
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(
            nabuDataManager.updateUserWalletInfo(
                offlineToken.mapFromMetadata(),
                jwt
            )
        ).thenReturn(Single.error { Throwable() })
            .thenReturn(Single.just(getNabuUser()))
        val verificationModel = PhoneVerificationModel(phoneNumberSanitized, verificationCode)

        // Act
        subject.onViewReady()
        publishSubject.onNext(verificationModel to Unit)
        publishSubject.onNext(verificationModel to Unit)
        // Assert
        verify(view, times(2)).showProgressDialog()
        verify(view, times(2)).dismissProgressDialog()
        verify(view).displayErrorDialog(any())
        verify(view).continueSignUp()
    }

    @Test
    fun `onViewReady, should throw exception and display error dialog`() {
        // Arrange
        val phoneNumberSanitized = "+1234567890"
        val jwt = "JWT"
        val verificationCode = VerificationCode("VERIFICATION_CODE")
        val offlineToken = NabuCredentialsMetadata("", "")
        val publishSubject = PublishSubject.create<Pair<PhoneVerificationModel, Unit>>()
        whenever(view.uiStateObservable).thenReturn(publishSubject)
        whenever(settingsDataManager.verifySms(verificationCode.code))
            .thenReturn(Observable.just(Settings()))
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(
            nabuDataManager.updateUserWalletInfo(
                offlineToken.mapFromMetadata(),
                jwt
            )
        ).thenReturn(Single.error { Throwable() })
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
    }

    private fun getNabuUser(): NabuUser = NabuUser(
        "",
        "",
        "",
        "",
        false,
        null,
        UserState.None,
        KycState.None,
        "",
        ""
    )
}
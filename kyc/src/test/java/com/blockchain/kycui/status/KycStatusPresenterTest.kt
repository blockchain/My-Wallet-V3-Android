package com.blockchain.kycui.status

import com.blockchain.android.testutils.rxInit
import com.blockchain.getBlankNabuUser
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.serialization.toMoshiJson
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager

class KycStatusPresenterTest {

    private lateinit var subject: KycStatusPresenter
    private val view: KycStatusView = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val metadataManager: MetadataManager = mock()
    private val notificationTokenManager: NotificationTokenManager = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = KycStatusPresenter(
            metadataManager,
            nabuDataManager,
            notificationTokenManager
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady exception thrown, finish page`() {
        // Arrange
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).finishPage()
    }

    @Test
    fun `onViewReady user loaded`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        val kycState = KycState.UnderReview
        val user = getBlankNabuUser(kycState)
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(user))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).renderUi(kycState)
    }

    @Test
    fun `onClickNotifyUser fails, should display toast`() {
        // Arrange
        whenever(notificationTokenManager.enableNotifications())
            .thenReturn(Completable.error { Throwable() })
        // Act
        subject.onClickNotifyUser()
        // Assert
        verify(notificationTokenManager).enableNotifications()
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showToast(any())
    }

    @Test
    fun `onClickNotifyUser successful, should display dialog`() {
        // Arrange
        whenever(notificationTokenManager.enableNotifications())
            .thenReturn(Completable.complete())
        // Act
        subject.onClickNotifyUser()
        // Assert
        verify(notificationTokenManager).enableNotifications()
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showNotificationsEnabledDialog()
    }

    @Test
    fun `onClickContinue should start exchange activity`() {
        // Act
        subject.onClickContinue()
        // Assert
        verify(view).startExchange()
    }
}
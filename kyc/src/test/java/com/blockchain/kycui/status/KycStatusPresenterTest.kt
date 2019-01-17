package com.blockchain.kycui.status

import com.blockchain.android.testutils.rxInit
import com.blockchain.getBlankNabuUser
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.nabu.NabuToken
import com.blockchain.notifications.NotificationTokenManager
import com.blockchain.validOfflineToken
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class KycStatusPresenterTest {

    private lateinit var subject: KycStatusPresenter
    private val view: KycStatusView = mock()
    private val nabuDataManager: NabuDataManager = mock()
    private val nabuToken: NabuToken = mock()
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
            nabuToken,
            nabuDataManager,
            notificationTokenManager
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady exception thrown, finish page`() {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
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
        val kycState = KycState.UnderReview
        val user = getBlankNabuUser(kycState)
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(nabuDataManager.getUser(validOfflineToken))
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
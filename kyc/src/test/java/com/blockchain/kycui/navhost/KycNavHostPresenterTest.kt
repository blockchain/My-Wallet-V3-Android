package com.blockchain.kycui.navhost

import com.blockchain.android.testutils.rxInit
import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.getBlankNabuUser
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.Address
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuUser
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.serialization.toMoshiJson
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager

class KycNavHostPresenterTest {

    private lateinit var subject: KycNavHostPresenter
    private val view: KycNavHostView = mock()
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
        subject = KycNavHostPresenter(metadataManager, nabuDataManager)
        subject.initView(view)
    }

    @Test
    fun `onViewReady exception thrown`() {
        // Arrange
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).showErrorToastAndFinish(any())
    }

    @Test
    fun `onViewReady no metadata found`() {
        // Arrange
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.error { MetadataNotFoundException("") })
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady metadata found, empty user object`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(getBlankNabuUser()))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, should redirect to country selection`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(
                Single.just(
                    NabuUser(
                        firstName = "FIRST_NAME",
                        lastName = "LAST_NAME",
                        email = null,
                        mobile = null,
                        dob = null,
                        mobileVerified = false,
                        address = null,
                        state = UserState.Created,
                        kycState = KycState.None,
                        insertedAt = null,
                        updatedAt = null
                    )
                )
            )
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).navigateToCountrySelection()
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, should redirect to address`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val nabuUser = NabuUser(
            firstName = "firstName",
            lastName = "lastName",
            email = null,
            mobile = null,
            dob = null,
            mobileVerified = false,
            address = getCompletedAddress(),
            state = UserState.Created,
            kycState = KycState.None,
            insertedAt = null,
            updatedAt = null
        )
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(nabuUser))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).navigateToAddress(nabuUser.toProfileModel(), "countryCode")
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, should redirect to phone entry`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val nabuUser = NabuUser(
            firstName = "firstName",
            lastName = "lastName",
            email = null,
            mobile = "mobile",
            dob = null,
            mobileVerified = false,
            address = getCompletedAddress(),
            state = UserState.Created,
            kycState = KycState.None,
            insertedAt = null,
            updatedAt = null
        )
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(nabuUser))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).navigateToMobileEntry(nabuUser.toProfileModel(), "countryCode")
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, should redirect to Onfido`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val nabuUser = NabuUser(
            firstName = "firstName",
            lastName = "lastName",
            email = null,
            mobile = "mobile",
            dob = null,
            mobileVerified = true,
            address = getCompletedAddress(),
            state = UserState.Active,
            kycState = KycState.None,
            insertedAt = null,
            updatedAt = null
        )
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(nabuUser))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).navigateToOnfido(nabuUser.toProfileModel(), "countryCode")
        verify(view).displayLoading(false)
    }

    @Test
    fun `onViewReady, should redirect to KYC status page`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val nabuUser = NabuUser(
            firstName = "firstName",
            lastName = "lastName",
            email = null,
            mobile = "mobile",
            dob = null,
            mobileVerified = true,
            address = getCompletedAddress(),
            state = UserState.Active,
            kycState = KycState.Pending,
            insertedAt = null,
            updatedAt = null
        )
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(nabuUser))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).displayLoading(true)
        verify(view).navigateToStatus()
    }

    private fun getCompletedAddress(): Address = Address(
        city = "city",
        line1 = "line1",
        line2 = "line2",
        state = "state",
        countryCode = "countryCode",
        postCode = "postCode"
    )
}
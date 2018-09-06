package com.blockchain.kycui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.getBlankNabuUser
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.nabu.metadata.NabuCredentialsMetadata
import com.blockchain.nabu.models.mapFromMetadata
import com.blockchain.serialization.toMoshiJson
import com.google.common.base.Optional
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class KycStatusHelperTest {

    private lateinit var subject: KycStatusHelper
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
        subject = KycStatusHelper(
            nabuDataManager,
            metadataManager,
            settingsDataManager
        )
    }

    @Test
    fun `has account returns false due to error fetching token`() {
        // Arrange
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.error { Throwable() })
        // Act
        val testObserver = subject.hasAccount().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `has account returns true as token was found in metadata`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        // Act
        val testObserver = subject.hasAccount().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `is in kyc region returns false as country code not found`() {
        // Arrange
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.isInKycRegion().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `is in kyc region returns true as country code is in list`() {
        // Arrange
        val countryCode = "UK"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.isInKycRegion().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `get kyc status returns none as error fetching user object`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.error { Throwable() })
        // Act
        val testObserver = subject.getKycStatus().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(KycState.None)
    }

    @Test
    fun `get kyc status returns user object status`() {
        // Arrange
        val kycState = KycState.Verified
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(getBlankNabuUser(kycState)))
        // Act
        val testObserver = subject.getKycStatus().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(kycState)
    }

    @Test
    fun `should display kyc returns false as in wrong region and no account`() {
        // Arrange
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.error { Throwable() })
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.shouldDisplayKyc().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
    }

    @Test
    fun `should display kyc returns true as in correct region but no account`() {
        // Arrange
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.error { Throwable() })
        val countryCode = "UK"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.shouldDisplayKyc().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `should display kyc returns true as in wrong region but has account`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.shouldDisplayKyc().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
    }

    @Test
    fun `get settings kyc state should return hidden as no account and wrong country`() {
        // Arrange
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.error { Throwable() })
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.getSettingsKycState().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(SettingsKycState.Hidden)
    }

    @Test
    fun `get settings kyc state should return unverified`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(getBlankNabuUser(KycState.None)))
        // Act
        val testObserver = subject.getSettingsKycState().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(SettingsKycState.Unverified)
    }

    @Test
    fun `get settings kyc state should return verified`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(getBlankNabuUser(KycState.Verified)))
        // Act
        val testObserver = subject.getSettingsKycState().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(SettingsKycState.Verified)
    }

    @Test
    fun `get settings kyc state should return failed`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(getBlankNabuUser(KycState.Rejected)))
        // Act
        val testObserver = subject.getSettingsKycState().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(SettingsKycState.Failed)
    }

    @Test
    fun `get settings kyc state should return in progress`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(getBlankNabuUser(KycState.Pending)))
        // Act
        val testObserver = subject.getSettingsKycState().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(SettingsKycState.InProgress)
    }

    @Test
    fun `get settings kyc state should return in review`() {
        // Arrange
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(nabuDataManager.getUser(offlineToken.mapFromMetadata()))
            .thenReturn(Single.just(getBlankNabuUser(KycState.UnderReview)))
        // Act
        val testObserver = subject.getSettingsKycState().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(SettingsKycState.UnderReview)
    }

    @Test
    fun `sync phone number fails due to missing metadata but returns complete`() {
        // Arrange
        val jwt = "JWT"
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.absent()))
        // Act
        val testObserver = subject.syncPhoneNumberWithNabu().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `sync phone number fails due to exception, throws correctly`() {
        // Arrange
        val jwt = "JWT"
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.error { Throwable() })
        // Act
        val testObserver = subject.syncPhoneNumberWithNabu().test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(Throwable::class.java)
    }

    @Test
    fun `sync phone number successful`() {
        // Arrange
        val jwt = "JWT"
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        val offlineToken = NabuCredentialsMetadata("", "")
        whenever(
            metadataManager.fetchMetadata(
                NabuCredentialsMetadata.USER_CREDENTIALS_METADATA_NODE
            )
        ).thenReturn(Observable.just(Optional.of(offlineToken.toMoshiJson())))
        whenever(nabuDataManager.updateUserWalletInfo(offlineToken.mapFromMetadata(), jwt))
            .thenReturn(Single.just(getBlankNabuUser(KycState.None)))
        // Act
        val testObserver = subject.syncPhoneNumberWithNabu().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }
}
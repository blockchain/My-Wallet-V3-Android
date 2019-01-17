package com.blockchain.kycui.settings

import com.blockchain.android.testutils.rxInit
import com.blockchain.exceptions.MetadataNotFoundException
import com.blockchain.getBlankNabuUser
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.Kyc2TierState
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.KycTierState
import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kyc.models.nabu.tiers
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.nabu.NabuToken
import com.blockchain.validOfflineToken
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.settings.SettingsDataManager

class KycStatusHelperTest {

    private lateinit var subject: KycStatusHelper
    private val nabuDataManager: NabuDataManager = mock()
    private val nabuToken: NabuToken = mock()
    private val settingsDataManager: SettingsDataManager = mock()
    private val tierService: TierService = mock()

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
            nabuToken,
            settingsDataManager,
            tierService
        )
    }

    @Test
    fun `has account returns false due to error fetching token`() {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(nabuDataManager.getUser(validOfflineToken))
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(nabuDataManager.getUser(validOfflineToken))
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
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
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
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
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
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(nabuDataManager.getUser(validOfflineToken))
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(nabuDataManager.getUser(validOfflineToken))
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(nabuDataManager.getUser(validOfflineToken))
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(nabuDataManager.getUser(validOfflineToken))
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        whenever(nabuDataManager.getUser(validOfflineToken))
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
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error(MetadataNotFoundException("Nabu Token not found")))
        // Act
        val testObserver = subject.syncPhoneNumberWithNabu().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `get settings kyc state should return state from tiers service`() {
        // Arrange
        whenever(tierService.tiers()).thenReturn(Single.just(tiers(KycTierState.Verified, KycTierState.Verified)))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        val countryCode = "US"
        val countryList =
            listOf(NabuCountryResponse("UK", "United Kingdom", emptyList(), listOf("KYC")))
        whenever(nabuDataManager.getCountriesList(Scope.Kyc))
            .thenReturn(Single.just(countryList))
        val settings: Settings = mock()
        whenever(settings.countryCode).thenReturn(countryCode)
        whenever(settingsDataManager.getSettings()).thenReturn(Observable.just(settings))
        // Act
        val testObserver = subject.getKyc2TierStatus().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(Kyc2TierState.Tier2Approved)
    }

    @Test
    fun `sync phone number fails due to exception, throws correctly`() {
        // Arrange
        val jwt = "JWT"
        whenever(nabuDataManager.requestJwt()).thenReturn(Single.just(jwt))
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
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
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(
            nabuDataManager.updateUserWalletInfo(
                validOfflineToken,
                jwt
            )
        )
            .thenReturn(Single.just(getBlankNabuUser(KycState.None)))
        // Act
        val testObserver = subject.syncPhoneNumberWithNabu().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `get user state fails but returns none`() {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.error { Throwable() })
        // Act
        val testObserver = subject.getUserState().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(UserState.None)
    }

    @Test
    fun `get user state successful, returns created`() {
        // Arrange
        whenever(
            nabuToken.fetchNabuToken()
        ).thenReturn(Single.just(validOfflineToken))
        whenever(nabuDataManager.getUser(validOfflineToken))
            .thenReturn(Single.just(getBlankNabuUser().copy(state = UserState.Created)))
        // Act
        val testObserver = subject.getUserState().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(UserState.Created)
    }
}
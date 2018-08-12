package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.models.nabu.NabuOfflineTokenResponse
import com.blockchain.kyc.models.nabu.NabuSessionTokenResponse
import com.blockchain.kyc.models.nabu.UserId
import com.blockchain.kyc.services.nabu.NabuService
import com.blockchain.kyc.stores.NabuSessionTokenStore
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test

class NabuDataManagerTest {

    private lateinit var subject: NabuDataManager
    private val nabuService: NabuService = mock()
    private val nabuTokenStore: NabuSessionTokenStore = mock()
    private val appVersion = "6.14.0"
    private val deviceId = "DEVICE_ID"

    @Before
    fun setUp() {
        subject = NabuDataManager(nabuService, nabuTokenStore, appVersion, deviceId)
    }

    @Test
    fun createUser() {
        // Arrange
        val guid = "GUID"
        val email = "EMAIL"
        val userId = "USER_ID"
        whenever(
            nabuService.createUser(
                guid = guid,
                email = email
            )
        ).thenReturn(Single.just(UserId(userId)))
        // Act
        val testObserver = subject.createUser(guid, email).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(userId)
        verify(nabuService).createUser(
            guid = guid,
            email = email
        )
    }

    @Test
    fun getAuthToken() {
        // Arrange
        val guid = "GUID"
        val email = "EMAIL"
        val userId = "USER_ID"
        val token = "TOKEN"
        val tokenResponse = NabuOfflineTokenResponse(userId, token)
        whenever(
            nabuService.getAuthToken(
                guid = guid,
                email = email,
                userId = userId,
                deviceId = deviceId,
                appVersion = appVersion
            )
        ).thenReturn(Single.just(tokenResponse))
        // Act
        val testObserver = subject.getAuthToken(guid, email, userId).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(tokenResponse)
        verify(nabuService).getAuthToken(
            guid = guid,
            email = email,
            userId = userId,
            deviceId = deviceId,
            appVersion = appVersion
        )
    }

    @Test
    fun getSessionToken() {
        // Arrange
        val guid = "GUID"
        val email = "EMAIL"
        val userId = "USER_ID"
        val offlineToken = "OFFLINE_TOKEN"
        val sessionTokenResponse = NabuSessionTokenResponse("", "", "", true, "", "", "")
        whenever(
            nabuService.getSessionToken(
                guid = guid,
                email = email,
                offlineToken = offlineToken,
                userId = userId,
                deviceId = deviceId,
                appVersion = appVersion
            )
        ).thenReturn(Single.just(sessionTokenResponse))
        // Act
        val testObserver =
            subject.getSessionToken(guid, email, offlineToken, userId).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(sessionTokenResponse)
        verify(nabuService).getSessionToken(
            guid = guid,
            email = email,
            offlineToken = offlineToken,
            userId = userId,
            deviceId = deviceId,
            appVersion = appVersion
        )
    }

    @Test
    fun `isInEeaCountry should return true`() {
        // Arrange
        val countryCode = "UK"
        val countriesList = listOf(
            NabuCountryResponse("GER", "Germany", listOf("EEA")),
            NabuCountryResponse("UK", "United Kingdom", listOf("EEA"))
        )
        whenever(nabuService.getEeaCountries())
            .thenReturn(Single.just(countriesList))
        // Act
        val testObserver = subject.isInEeaCountry(countryCode).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
        verify(nabuService).getEeaCountries()
    }

    @Test
    fun `isInEeaCountry should return false`() {
        // Arrange
        val countryCode = "US"
        val countriesList = listOf(
            NabuCountryResponse("GER", "Germany", listOf("EEA")),
            NabuCountryResponse("UK", "United Kingdom", listOf("EEA"))
        )
        whenever(nabuService.getEeaCountries())
            .thenReturn(Single.just(countriesList))
        // Act
        val testObserver = subject.isInEeaCountry(countryCode).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
        verify(nabuService).getEeaCountries()
    }
}
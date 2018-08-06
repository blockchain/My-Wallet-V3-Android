package com.blockchain.kyc.datamanagers.nabu

import com.blockchain.kyc.models.nabu.NabuCountryResponse
import com.blockchain.kyc.services.nabu.NabuService
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test

class NabuDataManagerTest {

    private lateinit var subject: NabuDataManager
    private val nabuService: NabuService = mock()

    @Before
    fun setUp() {
        subject = NabuDataManager(nabuService)
    }

    @Test
    fun `isInEeaCountry should return true`() {
        // Arrange
        val countryCode = "UK"
        val userAgent = "USER_AGENT"
        val countriesList = listOf(
            NabuCountryResponse("GER", "Germany", listOf("EEA")),
            NabuCountryResponse("UK", "United Kingdom", listOf("EEA"))
        )
        whenever(nabuService.getEeaCountries(userAgent = userAgent))
            .thenReturn(Single.just(countriesList))
        // Act
        val testObserver = subject.isInEeaCountry(countryCode, userAgent).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(true)
        verify(nabuService).getEeaCountries(userAgent = userAgent)
    }

    @Test
    fun `isInEeaCountry should return false`() {
        // Arrange
        val countryCode = "US"
        val userAgent = "USER_AGENT"
        val countriesList = listOf(
            NabuCountryResponse("GER", "Germany", listOf("EEA")),
            NabuCountryResponse("UK", "United Kingdom", listOf("EEA"))
        )
        whenever(nabuService.getEeaCountries(userAgent = userAgent))
            .thenReturn(Single.just(countriesList))
        // Act
        val testObserver = subject.isInEeaCountry(countryCode, userAgent).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(false)
        verify(nabuService).getEeaCountries(userAgent = userAgent)
    }
}
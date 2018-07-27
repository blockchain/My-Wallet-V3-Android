package com.blockchain.kyc.datamanagers

import com.blockchain.kyc.models.onfido.ApplicantResponse
import com.blockchain.kyc.models.onfido.CheckResult
import com.blockchain.kyc.models.onfido.CheckStatus
import com.blockchain.kyc.models.onfido.OnfidoCheckResponse
import com.blockchain.kyc.services.onfido.OnfidoService
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test

class OnfidoDataManagerTest {

    private lateinit var subject: OnfidoDataManager
    private val onfidoService: OnfidoService = mock()

    @Before
    fun setUp() {
        subject = OnfidoDataManager(onfidoService)
    }

    @Test
    fun createApplicant() {
        // Arrange
        val firstName = "FIRST_NAME"
        val lastName = "LAST_NAME"
        val apiToken = "API_TOKEN"
        val response = ApplicantResponse(
            id = "",
            createdAt = "",
            sandbox = true,
            firstName = "",
            lastName = "",
            country = ""
        )
        whenever(
            onfidoService.createApplicant(
                firstName = firstName,
                lastName = lastName,
                apiToken = apiToken
            )
        ).thenReturn(Single.just(response))
        // Act
        val testObserver = subject.createApplicant(firstName, lastName, apiToken).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(response)
        verify(onfidoService).createApplicant(
            firstName = firstName,
            lastName = lastName,
            apiToken = apiToken
        )
    }

    @Test
    fun createCheck() {
        // Arrange
        val applicantId = "12345"
        val apiToken = "API_TOKEN"
        val response = OnfidoCheckResponse(
            id = "",
            createdAt = "",
            sandbox = true,
            href = "",
            type = "",
            status = CheckStatus.Complete,
            result = CheckResult.Clear,
            resultsUri = "",
            redirectUri = null,
            reports = listOf()
        )
        whenever(
            onfidoService.createCheck(
                applicantId = applicantId,
                apiToken = apiToken
            )
        ).thenReturn(Single.just(response))
        // Act
        val testObserver = subject.createCheck(applicantId, apiToken).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertValue(response)
        verify(onfidoService).createCheck(
            applicantId = applicantId,
            apiToken = apiToken
        )
    }
}
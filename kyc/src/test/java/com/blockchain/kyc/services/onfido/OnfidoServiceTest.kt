package com.blockchain.kyc.services.onfido

import com.blockchain.kyc.api.APPLICANTS
import com.blockchain.kyc.api.CHECKS
import com.blockchain.kyc.models.onfido.CheckResultAdapter
import com.blockchain.kyc.models.onfido.CheckStatusAdapter
import com.squareup.moshi.Moshi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import com.blockchain.testutils.MockedRetrofitTest
import com.blockchain.testutils.getStringFromResource
import com.blockchain.testutils.mockWebServerInit

class OnfidoServiceTest {

    private lateinit var subject: OnfidoService
    private val moshi: Moshi = Moshi.Builder()
        .add(CheckResultAdapter())
        .add(CheckStatusAdapter())
        .build()
    private val server: MockWebServer = MockWebServer()

    @get:Rule
    val initMockServer = mockWebServerInit(server)

    @Before
    fun setUp() {
        subject = OnfidoService(
            MockedRetrofitTest(moshi, server).retrofit
        )
    }

    @Test
    fun createApplicant() {
        // Arrange
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/onfido/CreateApplicantResponse.json"))
        )
        val firstName = "Theresa"
        val lastName = "May"
        val apiToken = "API_TOKEN"
        // Act
        val testObserver = subject.createApplicant(
            path = APPLICANTS,
            firstName = firstName,
            lastName = lastName,
            apiToken = apiToken
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val applicant = testObserver.values().first()
        applicant.firstName `should equal to` firstName
        applicant.lastName `should equal to` lastName
        applicant.id `should equal to` "6a29732d-4561-4760-a2e3-a244ad324ba2"
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$APPLICANTS"
        // Check Header
        request.headers.get("Authorization") `should equal` "Token token=$apiToken"
    }

    @Test
    fun createCheck() {
        // Arrange
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/onfido/CheckResponse.json"))
        )
        val applicantId = "12345"
        val apiToken = "API_TOKEN"
        // Act
        val testObserver = subject.createCheck(
            path = APPLICANTS,
            applicantId = applicantId,
            apiToken = apiToken
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val checkResponse = testObserver.values().first()
        checkResponse.id `should equal to` "8546921-123123-123123"
        checkResponse.reports.size `should equal to` 2
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$APPLICANTS$applicantId/$CHECKS"
        // Check Header
        request.headers.get("Authorization") `should equal` "Token token=$apiToken"
    }
}
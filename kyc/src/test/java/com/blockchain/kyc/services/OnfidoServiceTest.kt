package com.blockchain.kyc.services

import com.blockchain.kyc.api.APPLICANTS
import com.blockchain.kyc.api.CHECKS
import com.blockchain.kyc.models.CheckResultAdapter
import com.blockchain.kyc.models.CheckStatusAdapter
import com.squareup.moshi.Moshi
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.testutils.mockWebServerInit
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory

class OnfidoServiceTest {

    private lateinit var subject: OnfidoService
    private val moshi: Moshi = Moshi.Builder()
        .add(CheckResultAdapter())
        .add(CheckStatusAdapter())
        .build()
    private val moshiConverterFactory = MoshiConverterFactory.create(moshi)
    private val rxJava2CallAdapterFactory = RxJava2CallAdapterFactory.create()
    private val server: MockWebServer = MockWebServer()

    @get:Rule
    val initMockServer = mockWebServerInit(server)

    @Before
    fun setUp() {
        val okHttpClient = OkHttpClient.Builder()
            .build()
        val retrofit = Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl(server.url("/").toString())
            .addConverterFactory(moshiConverterFactory)
            .addCallAdapterFactory(rxJava2CallAdapterFactory)
            .build()

        subject = OnfidoService(retrofit)
    }

    @Test
    fun createApplicant() {
        // Arrange
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(CREATE_APPLICANT_RESPONSE)
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
                .setBody(CHECK_RESPONSE)
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

    companion object {

        private const val CREATE_APPLICANT_RESPONSE = "{\n" +
            "  \"id\": \"6a29732d-4561-4760-a2e3-a244ad324ba2\",\n" +
            "  \"created_at\": \"2018-07-25T13:30:55Z\",\n" +
            "  \"sandbox\": true,\n" +
            "  \"title\": null,\n" +
            "  \"first_name\": \"Theresa\",\n" +
            "  \"middle_name\": null,\n" +
            "  \"last_name\": \"May\",\n" +
            "  \"email\": null,\n" +
            "  \"gender\": null,\n" +
            "  \"dob\": null,\n" +
            "  \"telephone\": null,\n" +
            "  \"mobile\": null,\n" +
            "  \"nationality\": null,\n" +
            "  \"country\": \"gbr\",\n" +
            "  \"href\": \"/v2/applicants/6a29732d-4561-4760-a2e3-a244ad324ba2\",\n" +
            "  \"mothers_maiden_name\": null,\n" +
            "  \"country_of_birth\": null,\n" +
            "  \"town_of_birth\": null,\n" +
            "  \"previous_last_name\": null,\n" +
            "  \"id_numbers\": [],\n" +
            "  \"addresses\": []\n" +
            "}"

        private const val CHECK_RESPONSE = "{\n" +
            "  \"id\": \"8546921-123123-123123\",\n" +
            "  \"created_at\": \"2014-05-23T13:50:33Z\",\n" +
            "  \"href\": \"/v2/applicants/1030303-123123-123123/checks/8546921-123123-123123\",\n" +
            "  \"type\": \"standard\",\n" +
            "  \"status\": \"awaiting_applicant\",\n" +
            "  \"result\": null,\n" +
            "  \"results_uri\": \"https://onfido.com/dashboard/information_requests/1234\",\n" +
            "  \"redirect_uri\": null,\n" +
            "  \"reports\": [\n" +
            "    {\n" +
            "      \"id\": \"6951786-123123-422221\",\n" +
            "      \"name\": \"identity\",\n" +
            "      \"created_at\": \"2014-05-23T13:50:33Z\",\n" +
            "      \"status\": \"awaiting_data\",\n" +
            "      \"result\": null,\n" +
            "      \"href\": \"/v2/checks/8546921-123123-123123/reports/6951786-123123-422221\",\n" +
            "      \"breakdown\": {},\n" +
            "      \"properties\": {}\n" +
            "    },\n" +
            "    {\n" +
            "      \"id\": \"6951786-123123-316712\",\n" +
            "      \"name\": \"document\",\n" +
            "      \"created_at\": \"2014-05-23T13:50:33Z\",\n" +
            "      \"status\": \"awaiting_data\",\n" +
            "      \"result\": null,\n" +
            "      \"href\": \"/v2/checks/8546921-123123-123123/reports/6951786-123123-316712\",\n" +
            "      \"breakdown\": {},\n" +
            "      \"properties\": {}\n" +
            "    }\n" +
            "  ],\n" +
            "  \"tags\": [\n" +
            "    \"My tag\",\n" +
            "    \"Another tag\"\n" +
            "  ]\n" +
            "}"
    }
}
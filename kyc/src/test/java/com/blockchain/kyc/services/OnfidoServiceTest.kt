package com.blockchain.kyc.services

import com.blockchain.kyc.api.APPLICANTS
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
    private val moshi: Moshi = Moshi.Builder().build()
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
    }
}
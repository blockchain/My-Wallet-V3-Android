package com.blockchain.kyc.services.nabu

import com.blockchain.kyc.api.nabu.NABU_COUNTRIES
import com.blockchain.kyc.models.onfido.CheckResultAdapter
import com.blockchain.kyc.models.onfido.CheckStatusAdapter
import com.blockchain.testutils.MockedRetrofitTest
import com.blockchain.testutils.getStringFromResource
import com.blockchain.testutils.mockWebServerInit
import com.squareup.moshi.Moshi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

class NabuServiceTest {

    private lateinit var subject: NabuService
    private val moshi: Moshi = Moshi.Builder()
        .add(CheckResultAdapter())
        .add(CheckStatusAdapter())
        .build()
    private val server: MockWebServer = MockWebServer()
    private val environmentConfig: EnvironmentConfig = mock()

    @get:Rule
    val initMockServer = mockWebServerInit(server)

    @Before
    fun setUp() {
        subject = NabuService(
            environmentConfig,
            MockedRetrofitTest(moshi, server).retrofit
        )
    }

    @Test
    fun getEeaCountries() {
        // Arrange
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetEeaCountriesList.json"))
        )
        val userAgent = "USER_AGENT"
        // Act
        val testObserver = subject.getEeaCountries(
            path = NABU_COUNTRIES,
            userAgent = userAgent
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check Response
        val countryList = testObserver.values().first()
        countryList[0].code `should equal to` "AUT"
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$NABU_COUNTRIES?region=eea"
        // Check Header
        request.headers.get("User-Agent") `should equal` userAgent
    }
}
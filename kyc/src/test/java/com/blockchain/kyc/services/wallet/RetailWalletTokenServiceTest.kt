package com.blockchain.kyc.services.wallet

import com.blockchain.kyc.api.wallet.RETAIL_JWT_TOKEN
import com.blockchain.kyc.models.nabu.KycStateAdapter
import com.blockchain.kyc.models.nabu.UserStateAdapter
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

class RetailWalletTokenServiceTest {

    private lateinit var subject: RetailWalletTokenService
    private val moshi: Moshi = Moshi.Builder()
        .add(UserStateAdapter())
        .add(KycStateAdapter())
        .build()
    private val server: MockWebServer = MockWebServer()
    private val environmentConfig: EnvironmentConfig = mock()
    private val apiKey = "API_KEY"

    @get:Rule
    val initMockServer = mockWebServerInit(server)

    @Before
    fun setUp() {
        subject = RetailWalletTokenService(
            environmentConfig,
            apiKey,
            MockedRetrofitTest(moshi, server).retrofit
        )
    }

    @Test
    fun `createUser success`() {
        // Arrange
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/wallet/GetSignedTokenSuccess.json"))
        )
        // Act
        val testObserver = subject.createUser(
            path = RETAIL_JWT_TOKEN,
            guid = guid,
            sharedKey = sharedKey
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val jwtResponse = testObserver.values().first()
        jwtResponse.isSuccessful `should equal to` true
        jwtResponse.token `should equal` "TOKEN"
        jwtResponse.error `should equal` null
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$RETAIL_JWT_TOKEN?guid=$guid&sharedKey=$sharedKey&api_code=$apiKey"
    }

    @Test
    fun `createUser failure`() {
        // Arrange
        val guid = "GUID"
        val sharedKey = "SHARED_KEY"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/wallet/GetSignedTokenFailure.json"))
        )
        // Act
        val testObserver = subject.createUser(
            path = RETAIL_JWT_TOKEN,
            guid = guid,
            sharedKey = sharedKey
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val jwtResponse = testObserver.values().first()
        jwtResponse.isSuccessful `should equal to` false
        jwtResponse.error `should equal` "ERROR"
        jwtResponse.token `should equal` null
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$RETAIL_JWT_TOKEN?guid=$guid&sharedKey=$sharedKey&api_code=$apiKey"
    }
}
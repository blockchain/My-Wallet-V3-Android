package com.blockchain.nabu.service.nabu

import com.blockchain.nabu.api.nabu.NABU_COUNTRIES
import com.blockchain.nabu.api.nabu.NABU_INITIAL_AUTH
import com.blockchain.nabu.api.nabu.NABU_PUT_ADDRESS
import com.blockchain.nabu.api.nabu.NABU_RECORD_COUNTRY
import com.blockchain.nabu.api.nabu.NABU_RECOVER_USER
import com.blockchain.nabu.api.nabu.NABU_REGISTER_CAMPAIGN
import com.blockchain.nabu.api.nabu.NABU_SESSION_TOKEN
import com.blockchain.nabu.api.nabu.NABU_STATES
import com.blockchain.nabu.api.nabu.NABU_SUPPORTED_DOCUMENTS
import com.blockchain.nabu.api.nabu.NABU_UPDATE_WALLET_INFO
import com.blockchain.nabu.api.nabu.NABU_USERS_CURRENT
import com.blockchain.nabu.getEmptySessionToken
import com.blockchain.nabu.models.responses.nabu.AddAddressRequest
import com.blockchain.nabu.models.responses.nabu.KycState
import com.blockchain.nabu.models.responses.nabu.KycStateAdapter
import com.blockchain.nabu.models.responses.nabu.NabuBasicUser
import com.blockchain.nabu.models.responses.nabu.NabuJwt
import com.blockchain.nabu.models.responses.nabu.RecordCountryRequest
import com.blockchain.nabu.models.responses.nabu.RegisterCampaignRequest
import com.blockchain.nabu.models.responses.nabu.Scope
import com.blockchain.nabu.models.responses.nabu.SupportedDocuments
import com.blockchain.nabu.models.responses.nabu.UserState
import com.blockchain.nabu.models.responses.nabu.UserStateAdapter
import com.blockchain.nabu.models.responses.tokenresponse.NabuOfflineTokenResponse
import com.blockchain.nabu.service.NabuService
import com.blockchain.testutils.MockedRetrofitTest
import com.blockchain.testutils.getStringFromResource
import com.blockchain.testutils.mockWebServerInit
import com.squareup.moshi.Moshi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should contain`
import org.amshove.kluent.`should have key`
import org.amshove.kluent.`should not be`
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class NabuServiceTest {

    private lateinit var subject: NabuService
    private val moshi: Moshi = Moshi.Builder()
        .add(UserStateAdapter())
        .add(KycStateAdapter())
        .build()
    private val server: MockWebServer = MockWebServer()

    @get:Rule
    val initMockServer = mockWebServerInit(server)

    @Before
    fun setUp() {
        subject = NabuService(MockedRetrofitTest(moshi, server).retrofit)
    }

    @Test
    fun getAuthToken() {
        // Arrange
        val jwt = "JWT"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetNabuOfflineToken.json"))
        )
        // Act
        val testObserver = subject.getAuthToken(jwt).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val (userIdResponse, token) = testObserver.values().first()
        userIdResponse `should be equal to` "d753109e-34c2-42bd-82f1-cc90470234kf"
        token `should be equal to` "d753109e-23jd-42bd-82f1-cc904702asdfkjf"
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_INITIAL_AUTH"
    }

    @Test
    fun getSessionToken() {
        // Arrange
        val guid = "GUID"
        val email = "EMAIL"
        val userId = "USER_ID"
        val offlineToken = "OFFLINE_TOKEN"
        val appVersion = "6.14.0"
        val deviceId = "DEVICE_ID"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetNabuSessionToken.json"))
        )
        // Act
        val testObserver = subject.getSessionToken(
            userId,
            offlineToken,
            guid,
            email,
            appVersion,
            deviceId
        ).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val tokenResponse = testObserver.values().first()
        tokenResponse.id `should be equal to` "7af48b7c-af37-47da-b830-7cf5e6cbc52e"
        tokenResponse.token `should be equal to` "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJyZXRhaW" +
            "wtY29yZSIsImV4cCI6MTUzMzY5NzI3MywiaWF0IjoxNTMzNjU0MDczLCJ1c2VySUQiOiJhMmMwODA1My0xNDQ" +
            "4LTQ2NjEtYmNhZS0yYzA5NmFhNzdjOTgiLCJqdGkiOiI3YWY0OGI3Yy1hZjM3LTQ3ZGEtYjgzMC03Y2Y1ZTZ" +
            "jYmM1MmUifQ.UzawGRtKsYX96vGhm_Hv8yXWFDqrIpeZt4eH2p6Eelk"
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_SESSION_TOKEN?userId=$userId"
        // Check Header
        request.headers.get("authorization") `should be equal to` offlineToken
        request.headers.get("X-WALLET-GUID") `should be equal to` guid
        request.headers.get("X-WALLET-EMAIL") `should be equal to` email
        request.headers.get("X-APP-VERSION") `should be equal to` appVersion
        request.headers.get("X-CLIENT-TYPE") `should be equal to` NabuService.CLIENT_TYPE
        request.headers.get("X-DEVICE-ID") `should be equal to` deviceId
    }

    @Test
    fun createBasicUser() {
        // Arrange
        val firstName = "FIRST_NAME"
        val lastName = "LAST_NAME"
        val dateOfBirth = "12-12-1234"
        val sessionToken = getEmptySessionToken()
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource(""))
        )
        // Act
        val testObserver = subject.createBasicUser(
            firstName,
            lastName,
            dateOfBirth,
            sessionToken
        ).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_USERS_CURRENT"
        // Check Body
        val requestString = request.requestToString()
        val adapter = moshi.adapter(NabuBasicUser::class.java)
        val basicUserBody = adapter.fromJson(requestString)!!
        basicUserBody.firstName `should be equal to` firstName
        basicUserBody.lastName `should be equal to` lastName
        basicUserBody.dateOfBirth `should be equal to` dateOfBirth
        // Check Header
        request.headers["authorization"] `should be equal to` sessionToken.authHeader
    }

    @Test
    fun getUser() {
        // Arrange
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetUser.json"))
        )
        // Act
        val testObserver = subject.getUser(getEmptySessionToken()).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check Response
        testObserver.values().first()
            .apply {
                firstName `should be equal to` "satoshi"
                address?.city `should be equal to` "London"
                state `should be equal to` UserState.Created
                kycState `should be equal to` KycState.None
                email `should be equal to` "satoshi@btc.com"
                emailVerified `should be equal to` false
                mobile `should be equal to` "+447123123123"
                mobileVerified `should be equal to` true
                tags!! `should have key` "SUNRIVER"
            }
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_USERS_CURRENT"
        // Check Header
        request.headers["authorization"] `should be equal to` getEmptySessionToken().authHeader
    }

    @Test
    fun updateWalletInformation() {
        // Arrange
        val jwt = "JWT"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetUser.json"))
        )
        // Act
        val testObserver = subject.updateWalletInformation(getEmptySessionToken(), jwt).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check Response
        val nabuUser = testObserver.values().first()
        nabuUser.firstName `should be equal to` "satoshi"
        nabuUser.address?.city `should be equal to` "London"
        nabuUser.state `should be equal to` UserState.Created
        nabuUser.kycState `should be equal to` KycState.None
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_UPDATE_WALLET_INFO"
        // Check Header
        request.headers.get("authorization") `should be equal to` getEmptySessionToken().authHeader
    }

    @Test
    fun addAddress() {
        // Arrange
        val line1 = "LINE1"
        val line2 = "LINE2"
        val city = "CITY"
        val state = null
        val countryCode = "COUNTRY_CODE"
        val postCode = "POST_CODE"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )
        // Act
        val testObserver = subject.addAddress(
            getEmptySessionToken(),
            line1,
            line2,
            city,
            state,
            postCode,
            countryCode
        ).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_PUT_ADDRESS"
        // Check Body
        val requestString = request.requestToString()
        val adapter = moshi.adapter(AddAddressRequest::class.java)
        val addressRequest = adapter.fromJson(requestString)!!
        addressRequest.address.line1 `should be equal to` line1
        addressRequest.address.line2 `should be equal to` line2
        addressRequest.address.city `should be equal to` city
        addressRequest.address.state `should be equal to` state
        addressRequest.address.countryCode `should be equal to` countryCode
        addressRequest.address.postCode `should be equal to` postCode
        // Check Header
        request.headers.get("authorization") `should be equal to` getEmptySessionToken().authHeader
    }

    @Test
    fun recordCountrySelection() {
        // Arrange
        val jwt = "JWT"
        val countryCode = "US"
        val state = "US-AL"
        val notifyWhenAvailable = true
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )
        // Act
        val testObserver = subject.recordCountrySelection(
            getEmptySessionToken(),
            jwt,
            countryCode,
            state,
            notifyWhenAvailable
        ).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_RECORD_COUNTRY"
        // Check Body
        val requestString = request.requestToString()
        val adapter = moshi.adapter(RecordCountryRequest::class.java)
        val recordCountryRequest = adapter.fromJson(requestString)!!
        recordCountryRequest.jwt `should be equal to` jwt
        recordCountryRequest.countryCode `should be equal to` countryCode
        recordCountryRequest.state `should be equal to` state
        recordCountryRequest.notifyWhenAvailable `should be equal to` notifyWhenAvailable
        // Check Header
        request.headers.get("authorization") `should be equal to` getEmptySessionToken().authHeader
    }

    @Test
    fun `get kyc countries`() {
        // Arrange
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetEeaCountriesList.json"))
        )
        // Act
        val testObserver = subject.getCountriesList(Scope.Kyc).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check Response
        val countryList = testObserver.values().first()
        countryList[0].code `should be equal to` "AUT"
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_COUNTRIES?scope=kyc"
    }

    @Test
    fun `get all countries with no scope`() {
        // Arrange
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetEeaCountriesList.json"))
        )
        // Act
        val testObserver = subject.getCountriesList(Scope.None).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check Response
        val countryList = testObserver.values().first()
        countryList[0].code `should be equal to` "AUT"
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_COUNTRIES"
    }

    @Test
    fun getUserTiers() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetUser.json"))
        )
        subject.getUser(getEmptySessionToken())
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .values().single()
            .apply {
                tiers `should not be` null
                tiers!!.current `should be equal to` 0
                tiers!!.selected `should be equal to` 1
                tiers!!.next `should be equal to` 2
            }
    }

    @Test
    fun `getUser resubmission status - resubmission not needed`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetUser.json"))
        )
        subject.getUser(getEmptySessionToken())
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .values().single()
            .apply {
                resubmission `should be` null
            }
    }

    @Test
    fun `getUser resubmission status - resubmission needed`() {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetUserWithResubmissionFlag.json"))
        )
        subject.getUser(getEmptySessionToken())
            .test()
            .await()
            .assertComplete()
            .assertNoErrors()
            .values().single()
            .apply {
                resubmission `should not be` null
            }
    }

    @Test
    fun `get kyc states`() {
        // Arrange
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetKycStatesList.json"))
        )
        // Act
        val testObserver = subject.getStatesList("US", Scope.Kyc).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check Response
        val stateList = testObserver.values().first()
        stateList[0].code `should be equal to` "US-AL"
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_COUNTRIES/US/$NABU_STATES?scope=kyc"
    }

    @Test
    fun getSupportedDocuments() {
        // Arrange
        val countryCode = "US"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetSupportedDocuments.json"))
        )
        // Act
        val testObserver = subject.getSupportedDocuments(
            getEmptySessionToken(),
            countryCode
        ).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check Response
        val supportedDocuments = testObserver.values().first()
        supportedDocuments.size `should be equal to` 2
        supportedDocuments `should contain` SupportedDocuments.DRIVING_LICENCE
        supportedDocuments `should contain` SupportedDocuments.PASSPORT
        // Check URL
        val request = server.takeRequest()
        request.path!! `should be equal to` "/$NABU_SUPPORTED_DOCUMENTS/$countryCode"
        // Check Header
        request.headers.get("authorization") `should be equal to` getEmptySessionToken().authHeader
    }

    @Test
    fun `recover user`() {
        // Arrange
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetEeaCountriesList.json"))
        )
        val offlineToken = NabuOfflineTokenResponse("userId", "token")
        // Act
        val testObserver = subject.recoverUser(offlineToken, "jwt").test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check Body
        val request = server.takeRequest()
        val requestString = request.requestToString()
        val adapter = moshi.adapter(NabuJwt::class.java)
        val jwt = adapter.fromJson(requestString)!!
        jwt.jwt `should be equal to` "jwt"
        // Check URL
        request.path!! `should be equal to` "/$NABU_RECOVER_USER/${offlineToken.userId}"
        request.headers.get("authorization") `should be equal to` "Bearer ${offlineToken.token}"
    }

    @Test
    fun `register for campaign`() {
        // Arrange
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )
        val campaignRequest = RegisterCampaignRequest(
            mapOf("key" to "value"),
            true
        )
        // Act
        val testObserver = subject.registerCampaign(
            getEmptySessionToken(),
            campaignRequest,
            "campaign"
        ).test()
        // Assert
        testObserver.await()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check Body
        val request = server.takeRequest()
        val requestString = request.requestToString()
        val adapter = moshi.adapter(RegisterCampaignRequest::class.java)
        adapter.fromJson(requestString)!!.apply {
            data `should be equal to` mapOf("key" to "value")
            newUser `should be equal to` true
        }
        // Check URL
        request.path!! `should be equal to` "/$NABU_REGISTER_CAMPAIGN"
        request.headers.get("authorization") `should be equal to` getEmptySessionToken().authHeader
    }

    private fun RecordedRequest.requestToString(): String =
        body.inputStream().bufferedReader().use { it.readText() }
}
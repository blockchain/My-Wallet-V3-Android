package com.blockchain.kyc.services.nabu

import com.blockchain.kyc.api.nabu.NABU_COUNTRIES
import com.blockchain.kyc.api.nabu.NABU_CREATE_USER_ID
import com.blockchain.kyc.api.nabu.NABU_INITIAL_AUTH
import com.blockchain.kyc.api.nabu.NABU_PUT_ADDRESS
import com.blockchain.kyc.api.nabu.NABU_PUT_MOBILE
import com.blockchain.kyc.api.nabu.NABU_SESSION_TOKEN
import com.blockchain.kyc.api.nabu.NABU_USERS_CURRENT
import com.blockchain.kyc.api.nabu.NABU_VERIFICAITIONS
import com.blockchain.kyc.models.nabu.AddAddressRequest
import com.blockchain.kyc.models.nabu.AddMobileNumberRequest
import com.blockchain.kyc.models.nabu.KycState
import com.blockchain.kyc.models.nabu.KycStateAdapter
import com.blockchain.kyc.models.nabu.MobileVerificationRequest
import com.blockchain.kyc.models.nabu.NabuBasicUser
import com.blockchain.kyc.models.nabu.Scope
import com.blockchain.kyc.models.nabu.UserState
import com.blockchain.kyc.models.nabu.UserStateAdapter
import com.blockchain.testutils.MockedRetrofitTest
import com.blockchain.testutils.getStringFromResource
import com.blockchain.testutils.mockWebServerInit
import com.squareup.moshi.Moshi
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
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
        .add(UserStateAdapter())
        .add(KycStateAdapter())
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
    fun createUser() {
        // Arrange
        val guid = "GUID"
        val email = "EMAIL"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(CREATE_USER_RESPONSE)
        )
        // Act
        val testObserver = subject.createUserId(
            path = NABU_CREATE_USER_ID,
            email = email,
            guid = guid
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val userId = testObserver.values().first()
        userId.userId `should equal to` "uniqueUserId"
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$NABU_CREATE_USER_ID"
    }

    @Test
    fun getAuthToken() {
        // Arrange
        val guid = "GUID"
        val email = "EMAIL"
        val userId = "USER_ID"
        val appVersion = "6.14.0"
        val deviceId = "DEVICE_ID"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetNabuOfflineToken.json"))
        )
        // Act
        val testObserver = subject.getAuthToken(
            path = NABU_INITIAL_AUTH,
            email = email,
            guid = guid,
            userId = userId,
            appVersion = appVersion,
            deviceId = deviceId
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val (userIdResponse, token) = testObserver.values().first()
        userIdResponse `should equal to` "d753109e-34c2-42bd-82f1-cc90470234kf"
        token `should equal to` "d753109e-23jd-42bd-82f1-cc904702asdfkjf"
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$NABU_INITIAL_AUTH?userId=$userId"
        // Check Headers
        request.headers.get("X-WALLET-GUID") `should equal` guid
        request.headers.get("X-WALLET-EMAIL") `should equal` email
        request.headers.get("X-APP-VERSION") `should equal` appVersion
        request.headers.get("X-CLIENT-TYPE") `should equal` NabuService.CLIENT_TYPE
        request.headers.get("X-DEVICE-ID") `should equal` deviceId
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
            path = NABU_SESSION_TOKEN,
            email = email,
            guid = guid,
            userId = userId,
            offlineToken = offlineToken,
            appVersion = appVersion,
            deviceId = deviceId
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check response
        val tokenResponse = testObserver.values().first()
        tokenResponse.id `should equal to` "7af48b7c-af37-47da-b830-7cf5e6cbc52e"
        tokenResponse.token `should equal to` "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJpc3MiOiJyZXRhaW" +
            "wtY29yZSIsImV4cCI6MTUzMzY5NzI3MywiaWF0IjoxNTMzNjU0MDczLCJ1c2VySUQiOiJhMmMwODA1My0xNDQ" +
            "4LTQ2NjEtYmNhZS0yYzA5NmFhNzdjOTgiLCJqdGkiOiI3YWY0OGI3Yy1hZjM3LTQ3ZGEtYjgzMC03Y2Y1ZTZ" +
            "jYmM1MmUifQ.UzawGRtKsYX96vGhm_Hv8yXWFDqrIpeZt4eH2p6Eelk"
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$NABU_SESSION_TOKEN?userId=$userId"
        // Check Header
        request.headers.get("authorization") `should equal` offlineToken
        request.headers.get("X-WALLET-GUID") `should equal` guid
        request.headers.get("X-WALLET-EMAIL") `should equal` email
        request.headers.get("X-APP-VERSION") `should equal` appVersion
        request.headers.get("X-CLIENT-TYPE") `should equal` NabuService.CLIENT_TYPE
        request.headers.get("X-DEVICE-ID") `should equal` deviceId
    }

    @Test
    fun createBasicUser() {
        // Arrange
        val firstName = "FIRST_NAME"
        val lastName = "LAST_NAME"
        val dateOfBirth = "12-12-1234"
        val sessionToken = "SESSION_TOKEN"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource(""))
        )
        // Act
        val testObserver = subject.createBasicUser(
            path = NABU_USERS_CURRENT,
            firstName = firstName,
            lastName = lastName,
            dateOfBirth = dateOfBirth,
            sessionToken = sessionToken
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$NABU_USERS_CURRENT"
        // Check Body
        val requestString = request.requestToString()
        val adapter = moshi.adapter(NabuBasicUser::class.java)
        val basicUserBody = adapter.fromJson(requestString)!!
        basicUserBody.firstName `should equal to` firstName
        basicUserBody.lastName `should equal to` lastName
        basicUserBody.dateOfBirth `should equal to` dateOfBirth
        // Check Header
        request.headers.get("authorization") `should equal` sessionToken
    }

    @Test
    fun getUser() {
        // Arrange
        val sessionToken = "SESSION_TOKEN"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(getStringFromResource("com/blockchain/kyc/services/nabu/GetUser.json"))
        )
        // Act
        val testObserver = subject.getUser(
            path = NABU_USERS_CURRENT,
            sessionToken = sessionToken
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check Response
        val nabuUser = testObserver.values().first()
        nabuUser.firstName `should equal` "satoshi"
        nabuUser.address?.city `should equal` "London"
        nabuUser.state `should equal` UserState.Created
        nabuUser.kycState `should equal` KycState.None
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$NABU_USERS_CURRENT"
        // Check Header
        request.headers.get("authorization") `should equal` sessionToken
    }

    @Test
    fun addAddress() {
        // Arrange
        val sessionToken = "SESSION_TOKEN"
        val city = "CITY"
        val line1 = "LINE1"
        val line2 = "LINE2"
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
            path = NABU_PUT_ADDRESS,
            sessionToken = sessionToken,
            city = city,
            line1 = line1,
            line2 = line2,
            state = state,
            countryCode = countryCode,
            postCode = postCode
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$NABU_PUT_ADDRESS"
        // Check Body
        val requestString = request.requestToString()
        val adapter = moshi.adapter(AddAddressRequest::class.java)
        val addressRequest = adapter.fromJson(requestString)!!
        addressRequest.address.city `should equal` city
        addressRequest.address.line1 `should equal` line1
        addressRequest.address.line2 `should equal` line2
        addressRequest.address.state `should equal` state
        addressRequest.address.countryCode `should equal` countryCode
        addressRequest.address.postCode `should equal` postCode
        // Check Header
        request.headers.get("authorization") `should equal` sessionToken
    }

    @Test
    fun addMobileNumber() {
        // Arrange
        val sessionToken = "SESSION_TOKEN"
        val mobileNumber = "MOBILE_NUMBER"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )
        // Act
        val testObserver = subject.addMobileNumber(
            path = NABU_PUT_MOBILE,
            sessionToken = sessionToken,
            mobileNumber = mobileNumber
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$NABU_PUT_MOBILE"
        // Check Body
        val requestString = request.requestToString()
        val adapter = moshi.adapter(AddMobileNumberRequest::class.java)
        val addMobileNumberRequest = adapter.fromJson(requestString)!!
        addMobileNumberRequest.phoneNumber `should equal to` mobileNumber
        // Check Header
        request.headers.get("authorization") `should equal` sessionToken
    }

    @Test
    fun verifyMobileNumber() {
        // Arrange
        val sessionToken = "SESSION_TOKEN"
        val mobileNumber = "MOBILE_NUMBER"
        val verificationCode = "VERIFICATION_CODE"
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("")
        )
        // Act
        val testObserver = subject.verifyMobileNumber(
            path = NABU_VERIFICAITIONS,
            sessionToken = sessionToken,
            mobileNumber = mobileNumber,
            verificationCode = verificationCode
        ).test()
        // Assert
        testObserver.awaitTerminalEvent()
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        // Check URL
        val request = server.takeRequest()
        request.path `should equal to` "/$NABU_VERIFICAITIONS"
        // Check Body
        val requestString = request.requestToString()
        val adapter = moshi.adapter(MobileVerificationRequest::class.java)
        val mobileVerificationRequest = adapter.fromJson(requestString)!!
        mobileVerificationRequest.phoneNumber `should equal to` mobileNumber
        mobileVerificationRequest.verificationCode `should equal to` verificationCode
        mobileVerificationRequest.type `should equal to` "MOBILE"
        // Check Header
        request.headers.get("authorization") `should equal` sessionToken
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
        val testObserver = subject.getCountriesList(
            path = NABU_COUNTRIES,
            scope = Scope.Kyc
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
        request.path `should equal to` "/$NABU_COUNTRIES?scope=kyc"
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
        val testObserver = subject.getCountriesList(
            path = NABU_COUNTRIES,
            scope = Scope.None
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
        request.path `should equal to` "/$NABU_COUNTRIES"
    }

    private fun RecordedRequest.requestToString(): String =
        body.inputStream().bufferedReader().use { it.readText() }

    companion object {
        private const val CREATE_USER_RESPONSE = "{\n" +
            "    \"userId\": \"uniqueUserId\"\n" +
            "}"
    }
}
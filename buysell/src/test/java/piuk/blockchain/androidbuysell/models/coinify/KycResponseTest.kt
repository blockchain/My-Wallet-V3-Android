package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.amshove.kluent.`should be instance of`
import org.junit.Test

class KycResponseTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(ReviewStateAdapter())
        .build()
    private val type = Types.newParameterizedType(KycResponse::class.java)
    private val adapter: JsonAdapter<KycResponse> = moshi.adapter(type)

    @Test
    fun `KycResponse status pending`() {
        // Arrange

        // Act
        val result: KycResponse = adapter.fromJson(RESPONSE_PENDING)!!
        // Assert
        result `should be instance of` KycResponse::class.java
        result.state `should be instance of` ReviewState.Pending::class.java
    }

    @Test
    fun `KycResponse status rejected`() {
        // Arrange

        // Act
        val result: KycResponse = adapter.fromJson(RESPONSE_REJECTED)!!
        // Assert
        result `should be instance of` KycResponse::class.java
        result.state `should be instance of` ReviewState.Rejected::class.java
    }

    @Test
    fun `KycResponse status failed`() {
        // Arrange

        // Act
        val result: KycResponse = adapter.fromJson(RESPONSE_FAILED)!!
        // Assert
        result `should be instance of` KycResponse::class.java
        result.state `should be instance of` ReviewState.Failed::class.java
    }

    @Test
    fun `KycResponse status expired`() {
        // Arrange

        // Act
        val result: KycResponse = adapter.fromJson(RESPONSE_EXPIRED)!!
        // Assert
        result `should be instance of` KycResponse::class.java
        result.state `should be instance of` ReviewState.Expired::class.java
    }

    @Test
    fun `KycResponse status completed`() {
        // Arrange

        // Act
        val result: KycResponse = adapter.fromJson(RESPONSE_COMPLETED)!!
        // Assert
        result `should be instance of` KycResponse::class.java
        result.state `should be instance of` ReviewState.Completed::class.java
    }

    @Test
    fun `KycResponse status reviewing`() {
        // Arrange

        // Act
        val result: KycResponse = adapter.fromJson(RESPONSE_REVIEWING)!!
        // Assert
        result `should be instance of` KycResponse::class.java
        result.state `should be instance of` ReviewState.Reviewing::class.java
    }

    @Test
    fun `KycResponse status documents requested`() {
        // Arrange

        // Act
        val result: KycResponse = adapter.fromJson(RESPONSE_DOCUMENTS_REQUESTED)!!
        // Assert
        result `should be instance of` KycResponse::class.java
        result.state `should be instance of` ReviewState.DocumentsRequested::class.java
    }
}

private const val RESPONSE_PENDING = "{\n" +
    "  \"id\": 55555,\n" +
    "  \"state\": \"pending\",\n" +
    "  \"returnUrl\": \"https://mypage.com/kyc_complete\",\n" +
    "  \"redirectUrl\": \"https://example.com/url/to/perform/kyc/review\",\n" +
    "  \"externalId\": \"1234-abcd-5678-f33d\",\n" +
    "  \"updateTime\": \"2016-07-07T12:11:36Z\",\n" +
    "  \"createTime\": \"2016-07-07T12:10:19Z\"\n" +
    "}"

private const val RESPONSE_REJECTED = "{\n" +
    "  \"id\": 55555,\n" +
    "  \"state\": \"rejected\",\n" +
    "  \"returnUrl\": \"https://mypage.com/kyc_complete\",\n" +
    "  \"redirectUrl\": \"https://example.com/url/to/perform/kyc/review\",\n" +
    "  \"externalId\": \"1234-abcd-5678-f33d\",\n" +
    "  \"updateTime\": \"2016-07-07T12:11:36Z\",\n" +
    "  \"createTime\": \"2016-07-07T12:10:19Z\"\n" +
    "}"

private const val RESPONSE_FAILED = "{\n" +
    "  \"id\": 55555,\n" +
    "  \"state\": \"failed\",\n" +
    "  \"returnUrl\": \"https://mypage.com/kyc_complete\",\n" +
    "  \"redirectUrl\": \"https://example.com/url/to/perform/kyc/review\",\n" +
    "  \"externalId\": \"1234-abcd-5678-f33d\",\n" +
    "  \"updateTime\": \"2016-07-07T12:11:36Z\",\n" +
    "  \"createTime\": \"2016-07-07T12:10:19Z\"\n" +
    "}"

private const val RESPONSE_EXPIRED = "{\n" +
    "  \"id\": 55555,\n" +
    "  \"state\": \"expired\",\n" +
    "  \"returnUrl\": \"https://mypage.com/kyc_complete\",\n" +
    "  \"redirectUrl\": \"https://example.com/url/to/perform/kyc/review\",\n" +
    "  \"externalId\": \"1234-abcd-5678-f33d\",\n" +
    "  \"updateTime\": \"2016-07-07T12:11:36Z\",\n" +
    "  \"createTime\": \"2016-07-07T12:10:19Z\"\n" +
    "}"

private const val RESPONSE_COMPLETED = "{\n" +
    "  \"id\": 55555,\n" +
    "  \"state\": \"completed\",\n" +
    "  \"returnUrl\": \"https://mypage.com/kyc_complete\",\n" +
    "  \"redirectUrl\": \"https://example.com/url/to/perform/kyc/review\",\n" +
    "  \"externalId\": \"1234-abcd-5678-f33d\",\n" +
    "  \"updateTime\": \"2016-07-07T12:11:36Z\",\n" +
    "  \"createTime\": \"2016-07-07T12:10:19Z\"\n" +
    "}"

private const val RESPONSE_REVIEWING = "{\n" +
    "  \"id\": 55555,\n" +
    "  \"state\": \"reviewing\",\n" +
    "  \"returnUrl\": \"https://mypage.com/kyc_complete\",\n" +
    "  \"redirectUrl\": \"https://example.com/url/to/perform/kyc/review\",\n" +
    "  \"externalId\": \"1234-abcd-5678-f33d\",\n" +
    "  \"updateTime\": \"2016-07-07T12:11:36Z\",\n" +
    "  \"createTime\": \"2016-07-07T12:10:19Z\"\n" +
    "}"

private const val RESPONSE_DOCUMENTS_REQUESTED = "{\n" +
    "  \"id\": 55555,\n" +
    "  \"state\": \"documentsRequested\",\n" +
    "  \"returnUrl\": \"https://mypage.com/kyc_complete\",\n" +
    "  \"redirectUrl\": \"https://example.com/url/to/perform/kyc/review\",\n" +
    "  \"externalId\": \"1234-abcd-5678-f33d\",\n" +
    "  \"updateTime\": \"2016-07-07T12:11:36Z\",\n" +
    "  \"createTime\": \"2016-07-07T12:10:19Z\"\n" +
    "}"

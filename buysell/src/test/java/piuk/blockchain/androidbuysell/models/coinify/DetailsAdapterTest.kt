package piuk.blockchain.androidbuysell.models.coinify

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal to`
import org.junit.Test

class DetailsAdapterTest {

    private val moshi: Moshi = Moshi.Builder()
        .add(DetailsAdapter())
        .build()
    private val type = Types.newParameterizedType(List::class.java, Details::class.java)
    private val adapter: JsonAdapter<List<Details>> = moshi.adapter(type)

    @Test
    fun `fromJson bank`() {
        // Arrange

        // Act
        val result = adapter.fromJson(DETAILS_BANK)!!
        // Assert
        val details = result[0]
        details `should be instance of` BankDetails::class
        details as BankDetails
        details.account `should be instance of` Account::class
        details.account
        details.account.bic `should equal to` "BIC"
        details.bank.name!! `should equal to` "Example Bank"
    }

    @Test
    fun `fromJson card`() {
        // Arrange

        // Act
        val result = adapter.fromJson(DETAILS_CARD)!!
        // Assert
        val details = result[0]
        details `should be instance of` CardDetails::class
        details as CardDetails
        details.provider!! `should equal to` "isignthis"
    }

    @Test
    fun `fromJson blockchain`() {
        // Arrange

        // Act
        val result = adapter.fromJson(DETAILS_BLOCKCHAIN)!!
        // Assert
        val details = result[0]
        details `should be instance of` BlockchainDetails::class
        details as BlockchainDetails
        details.account `should be instance of` String::class.java
        details.account `should equal to` "16yaQgjFfViVyekj6XKNyTzX7Mu4bqmBMQ"
    }

    companion object {

        private const val DETAILS_BANK = "[" +
            "      {\n" +
            "        \"bank\": {\n" +
            "          \"name\": \"Example Bank\",\n" +
            "          \"address\": {\n" +
            "            \"city\": \"London\",\n" +
            "            \"street\": \"25 London Road\",\n" +
            "            \"country\": \"GB\",\n" +
            "            \"zipcode\": \"W1 1W\"\n" +
            "          }\n" +
            "        },\n" +
            "        \"holder\": {\n" +
            "          \"name\": \"Coinify\",\n" +
            "          \"address\": {\n" +
            "            \"city\": \"Herlev\",\n" +
            "            \"street\": \"Herlev\",\n" +
            "            \"country\": \"DK\",\n" +
            "            \"zipcode\": \"12345\"\n" +
            "          }\n" +
            "        },\n" +
            "        \"account\": {\n" +
            "          \"bic\": \"BIC\",\n" +
            "          \"type\": \"international\",\n" +
            "          \"number\": \"GB1234567890\",\n" +
            "          \"currency\": \"GBP\"\n" +
            "        },\n" +
            "        \"referenceText\": \"AB1234567\"\n" +
            "      }" +
            "]"

        private const val DETAILS_CARD = "[" +
            "      {\n" +
            "        \"provider\": \"isignthis\",\n" +
            "        \"paymentId\": \"a0895735-1234-1234-1234-47ecfb05ea7b\",\n" +
            "        \"redirectUrl\": \"https://coinify-verify.isignthis.com/landing/" +
            "a0895735-1234-1234-1234-47ecfb05ea7b\",\n" +
            "        \"cardPaymentId\": 352518\n" +
            "      }" +
            "]"

        private const val DETAILS_BLOCKCHAIN = "[" +
            "      {\n" +
            "        \"account\": \"16yaQgjFfViVyekj6XKNyTzX7Mu4bqmBMQ\"\n" +
            "      }" +
            "]"
    }
}
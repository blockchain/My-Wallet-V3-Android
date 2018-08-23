package com.blockchain.serialization

import com.squareup.moshi.Json
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal to`
import org.amshove.kluent.`should equal`
import org.json.JSONObject
import org.junit.Test

class SerialisationUtilsTest {

    @Test
    fun `class fromJsonString should return object`() {
        val testClass = TestClass::class.fromMoshiJson(TEST_CLASS_STRING)
        testClass `should be instance of` TestClass::class
        testClass.firstParam `should equal to` "parameter 1"
        testClass.secondParam `should equal to` 5
        testClass.thirdParam `should equal to` false
    }

    @Test
    fun `testClass to string should be correctly formed`() {
        val firstParamValue = "FIRST_PARAM_VALUE"
        val secondParamValue = 17
        val thirdParamValue = true

        val stringResult = TestClass(firstParamValue, secondParamValue, thirdParamValue)
            .toMoshiJson()

        val jsonObject = JSONObject(stringResult)
        jsonObject["firstParam"] `should equal` firstParamValue
        jsonObject["secondParam"] `should equal` secondParamValue
        jsonObject["third_param"] `should equal` thirdParamValue
    }

    companion object {
        private const val TEST_CLASS_STRING = "{\n" +
            "  \"firstParam\": \"parameter 1\",\n" +
            "  \"secondParam\": 5,\n" +
            "  \"third_param\": false\n" +
            "}"
    }

    private data class TestClass(
        val firstParam: String,
        val secondParam: Int,
        @field:Json(name = "third_param") val thirdParam: Boolean
    ) : JsonSerializable
}

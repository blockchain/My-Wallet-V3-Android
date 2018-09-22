package com.blockchain.serialization

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigDecimal

class BigDecimalJsonTests {

    private class ExampleDto(
        val value: BigDecimal
    )

    private val moshi = Moshi.Builder()
        .add(BigDecimalAdaptor())
        .build()

    private val jsonAdapter: JsonAdapter<ExampleDto> = moshi.adapter(
        ExampleDto::class.java
    )

    @Test
    fun `json to big decimal`() {
        jsonAdapter.fromJson(
            """
               {
                 "value": 0.3
                }
            """
        )!!.value `should equal` "0.3".toBigDecimal()
    }

    @Test
    fun `json to big decimal (string)`() {
        jsonAdapter.fromJson(
            """
               {
                 "value": "0.3"
                }
            """
        )!!.value `should equal` "0.3".toBigDecimal()
    }

    @Test
    fun `big decimal to json`() {
        jsonAdapter.toJson(
            ExampleDto("0.3".toBigDecimal())
        ) `should equal` """{"value":"0.3"}"""
    }

    @Test
    fun `18 digit big decimal to json`() {
        jsonAdapter.toJson(
            ExampleDto("0.123456789012345678".toBigDecimal())
        ) `should equal` """{"value":"0.123456789012345678"}"""
    }

    @Test
    fun `large btc example big decimal to json`() {
        jsonAdapter.toJson(
            ExampleDto(12345678.12345678.bitcoin().toBigDecimal())
        ) `should equal` """{"value":"12345678.12345678"}"""
    }

    @Test
    fun `large ether example big decimal to json`() {
        jsonAdapter.toJson(
            ExampleDto(12345678.12345678.ether().toBigDecimal())
        ) `should equal` """{"value":"12345678.123456780000000000"}"""
    }

    @Test
    fun `very large big decimal to json`() {
        jsonAdapter.toJson(
            ExampleDto("123456789012345678901".toBigDecimal())
        ) `should equal` """{"value":"123456789012345678901"}"""
    }
}

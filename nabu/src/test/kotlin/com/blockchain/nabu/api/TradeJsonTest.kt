package com.blockchain.nabu.api

import com.blockchain.serialization.BigDecimalAdaptor
import com.blockchain.serialization.JsonSerializable
import com.blockchain.testutils.`should be assignable from`
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import org.amshove.kluent.`should equal`
import org.junit.Test

class TradeJsonTest {

    @Test
    fun `should parse object correctly`() {
        val moshi: Moshi = Moshi.Builder()
            .add(TransactionStateAdapter())
            .add(BigDecimalAdaptor())
            .build()
        val type = Types.newParameterizedType(TradeJson::class.java)
        val adapter: JsonAdapter<TradeJson> = moshi.adapter(type)

        val response = """
{
    "id": "ede39566-1f0d-4e48-96fa-b558b70e46b7",
    "createdAt": "2018-07-30T13:45:67.890Z",
    "updatedAt": "2018-07-30T13:45:67.890Z",
    "pair": "BTC-ETH",
    "quantity": "0.1337",
    "currency": "ETH",
    "refundAddress": "1Refund6bAHb8ybZjqQMjJrcCrHGW9sb6uF",
    "price": "0.06",
    "depositAddress": "1Deposit6bAHb8ybZjqQMjJrcCrHGW9sb6uF",
    "depositQuantity": "0.008022",
    "withdrawalAddress": "0xwithdrawa7d398351b8be11c439e05c5b3259aec9b",
    "withdrawalQuantity": "0.1337",
    "depositTxHash": "e6a5cfee8063330577babb6fb92eabccf5c3c1aeea120c550b6779a6c657dfce",
    "withdrawalTxHash": "0xcc34f317a2fc8fb318777ea2529dfaf2ad9338907637137c3ec7d614abe7557f",
    "state": "FINISHED"
}
        """.trimIndent()

        adapter.fromJson(response)!!.apply {
            id `should equal` "ede39566-1f0d-4e48-96fa-b558b70e46b7"
            createdAt `should equal` "2018-07-30T13:45:67.890Z"
            updatedAt `should equal` "2018-07-30T13:45:67.890Z"
            pair `should equal` "BTC-ETH"
            quantity `should equal` "0.1337".toBigDecimal()
            currency `should equal` "ETH"
            refundAddress `should equal` "1Refund6bAHb8ybZjqQMjJrcCrHGW9sb6uF"
            price `should equal` "0.06".toBigDecimal()
            depositAddress `should equal` "1Deposit6bAHb8ybZjqQMjJrcCrHGW9sb6uF"
            depositQuantity `should equal` "0.008022".toBigDecimal()
            withdrawalAddress `should equal` "0xwithdrawa7d398351b8be11c439e05c5b3259aec9b"
            withdrawalQuantity `should equal` "0.1337".toBigDecimal()
            depositTxHash `should equal`
                "e6a5cfee8063330577babb6fb92eabccf5c3c1aeea120c550b6779a6c657dfce"
            withdrawalTxHash `should equal`
                "0xcc34f317a2fc8fb318777ea2529dfaf2ad9338907637137c3ec7d614abe7557f"
        }
    }

    @Test
    fun `ensure trade answer is JsonSerializable for proguard`() {
        JsonSerializable::class.`should be assignable from`(TradeJson::class)
    }
}
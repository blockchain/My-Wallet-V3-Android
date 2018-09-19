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
  "id": "039267ab-de16-4093-8cdf-a7ea1c732dbd",
  "state": "FINISHED",
  "createdAt": "2018-09-19T12:20:42.894Z",
  "updatedAt": "2018-09-19T12:24:18.943Z",
  "pair": "ETH-BTC",
  "refundAddress": "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
  "rate": "0.1",
  "depositAddress": "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
  "deposit": {
    "symbol": "ETH",
    "value": "100.0"
  },
  "withdrawalAddress": "3H4w1Sqk8UNNEfZoa9Z8FZJ6RYHrxLmzGU",
  "withdrawal": {
    "symbol": "BTC",
    "value": "10.0"
  },
  "withdrawalFee": {
    "symbol": "BTC",
    "value": "0.0000001"
  },
  "fiatValue": {
    "symbol": "GBP",
    "value": "10.0"
  },
  "depositTxHash": "e6a5cfee8063330577babb6fb92eabccf5c3c1aeea120c550b6779a6c657dfce",
  "withdrawalTxHash": "0xf902adc8862c6c6ad2cd06f12d952e95c50ad783bae50ef952e1f54b7762a50e"
}
        """.trimIndent()

        adapter.fromJson(response)!!.apply {
            id `should equal` "039267ab-de16-4093-8cdf-a7ea1c732dbd"
            createdAt `should equal` "2018-09-19T12:20:42.894Z"
            pair `should equal` "ETH-BTC"
            rate `should equal` 0.1.toBigDecimal()
            refundAddress `should equal` "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb"
            depositAddress `should equal` "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"
            deposit `should equal` Value("ETH", 100.0.toBigDecimal())
            withdrawalAddress `should equal` "3H4w1Sqk8UNNEfZoa9Z8FZJ6RYHrxLmzGU"
            withdrawal `should equal` Value("BTC", 10.0.toBigDecimal())
            state `should equal` TransactionState.Finished
            withdrawalFee `should equal` Value("BTC", 0.0000001.toBigDecimal().stripTrailingZeros())
            fiatValue `should equal` Value("GBP", 10.0.toBigDecimal())
        }
    }

    @Test
    fun `ensure trade answer is JsonSerializable for proguard`() {
        JsonSerializable::class.`should be assignable from`(TradeJson::class)
    }
}
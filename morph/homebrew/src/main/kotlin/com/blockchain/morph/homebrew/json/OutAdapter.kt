package com.blockchain.morph.homebrew.json

import com.blockchain.morph.homebrew.QuoteWebSocketParams
import com.squareup.moshi.FromJson
import com.squareup.moshi.ToJson

internal class OutAdapter {

    @ToJson
    @Suppress("unused")
    fun toJson(out: Out): OutSerialized =
        when (out) {
            is Out.Subscribe -> OutSerialized(
                channel = "conversion",
                operation = "subscribe",
                params = out.params
            )
            is Out.UnsubscribePair -> OutSerialized(
                channel = "conversion",
                operation = "unsubscribe",
                params = UnsubscribeParams(
                    type = "conversionPair",
                    pair = out.params.pair
                )
            )
        }

    @FromJson
    @Suppress("unused", "UNUSED_PARAMETER")
    fun fromJson(json: String): Out = throw UnsupportedOperationException()
}

internal class OutSerialized(
    val channel: String,
    val operation: String,
    val params: Any
)

internal sealed class Out {
    class Subscribe(val params: QuoteWebSocketParams) : Out()
    class UnsubscribePair(val params: QuoteWebSocketParams) : Out()
}

internal class UnsubscribeParams(
    val type: String,
    val pair: String
)

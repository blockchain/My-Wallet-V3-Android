package com.blockchain.network.websocket

import com.blockchain.serialization.JsonSerializable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.reactivex.Observable

inline fun <
    reified OUTGOING : JsonSerializable,
    reified INCOMING : JsonSerializable
    >
    WebSocket<String, String>.toJsonSocket(moshi: Moshi): WebSocket<OUTGOING, INCOMING> {
    return MoshiJsonWebSocketDecorator(this, moshi.adapter(OUTGOING::class.java), moshi.adapter(INCOMING::class.java))
}

class MoshiJsonWebSocketDecorator<OUTGOING : Any, INCOMING : Any>(
    private val inner: WebSocket<String, String>,
    private val outgoingAdapter: JsonAdapter<OUTGOING>,
    private val incomingAdapter: JsonAdapter<INCOMING>
) : WebSocket<OUTGOING, INCOMING>, WebSocketConnection by inner {

    override fun send(message: OUTGOING) {
        inner.send(outgoingAdapter.toJson(message))
    }

    override val responses: Observable<INCOMING>
        get() = inner.responses.map { incomingAdapter.fromJson(it)!! }
}

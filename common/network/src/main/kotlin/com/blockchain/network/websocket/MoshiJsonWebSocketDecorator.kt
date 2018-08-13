package com.blockchain.network.websocket

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.reactivex.Observable

inline fun <reified INCOMING : Any, reified OUTGOING : Any> WebSocket<String, String>.toJsonSocket(
    moshi: Moshi
): WebSocket<INCOMING, OUTGOING> {
    return MoshiJsonWebSocketDecorator(this, moshi.adapter(INCOMING::class.java), moshi.adapter(OUTGOING::class.java))
}

class MoshiJsonWebSocketDecorator<INCOMING : Any, OUTGOING : Any>(
    private val inner: WebSocket<String, String>,
    private val incomingAdapter: JsonAdapter<INCOMING>,
    private val outgoingAdapter: JsonAdapter<OUTGOING>
) : WebSocket<INCOMING, OUTGOING>, WebSocketOpenClose by inner {

    override fun send(message: OUTGOING) {
        inner.send(outgoingAdapter.toJson(message))
    }

    override val responses: Observable<INCOMING>
        get() = inner.responses.map { incomingAdapter.fromJson(it)!! }
}

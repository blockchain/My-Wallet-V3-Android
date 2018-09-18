package com.blockchain.network.websocket

import com.blockchain.serialization.JsonSerializable
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject

interface ChannelAwareWebSocket {
    fun openChannel(name: String, params: JsonSerializable? = null): WebSocketChannel<String>
}

interface WebSocketChannel<INCOMING> : WebSocketReceive<INCOMING> {
    fun close(params: JsonSerializable? = null)
}

fun StringWebSocket.channelAware(): ChannelAwareWebSocket = WebSocketChannelAdapter(this)

private class WebSocketChannelAdapter(private val underlingSocket: StringWebSocket) : ChannelAwareWebSocket {
    private val outAdapter = Moshi.Builder()
        .build().adapter(SubscribeUnsubscribeJson::class.java)

    override fun openChannel(name: String, params: JsonSerializable?): WebSocketChannel<String> {
        underlingSocket.send(outAdapter.toJson(SubscribeUnsubscribeJson(name, "subscribe", params)))
        return underlingSocket.asChannel(name, outAdapter)
    }
}

class ErrorFromServer(val fullJson: String) : Exception("Server returned error")

private fun StringWebSocket.asChannel(
    name: String,
    outAdapter: JsonAdapter<SubscribeUnsubscribeJson>
): WebSocketChannel<String> {

    return object : WebSocketChannel<String> {

        private val closed = PublishSubject.create<Any>()

        override fun close(params: JsonSerializable?) {
            this@asChannel.send(outAdapter.toJson(SubscribeUnsubscribeJson(name, "unsubscribe", params)))
            closed.onNext(Any())
        }

        override val responses: Observable<String>
            get() = this@asChannel.responses.filter { json ->
                incomingAdapter.fromJson(json)
                    ?.let {
                        it.channel == name &&
                            it.type != "subscribed" &&
                            it.type != "unsubscribed" &&
                            throwIfError(it, json)
                    } ?: false
            }
                .takeUntil(closed)

        private fun throwIfError(message: IncomingMessage, json: String): Boolean {
            if (message.type == "error")
                throw ErrorFromServer(json)
            return true
        }

        private val incomingAdapter = Moshi.Builder().build().adapter(IncomingMessage::class.java)
    }
}

private class IncomingMessage(
    val channel: String,
    val type: String
) : JsonSerializable

private class SubscribeUnsubscribeJson(
    @Suppress("unused") val channel: String,
    @Suppress("unused") val operation: String,
    val params: Any?
) : JsonSerializable

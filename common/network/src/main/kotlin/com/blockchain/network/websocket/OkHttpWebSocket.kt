package com.blockchain.network.websocket

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocketListener

class Options(
    val url: String,
    val origin: String,
    /**
     * Used in close reason message text
     */
    val name: String = "Unnamed"
)

/**
 * Creates a new disconnected socket instance from an [OkHttpClient] with the supplied [options].
 *
 * Unlike [OkHttpClient.newWebSocket], you must call [WebSocket.open] to actually connect.
 *
 * It is reusable, you can [WebSocket.close] and [WebSocket.open], you will get a new underlying [OkHttpWebSocket].
 */
fun OkHttpClient.newBlockchainWebSocket(
    options: Options,
    listener: WebSocket.Listener<String>? = null
): WebSocket<String, String> = OkHttpWebSocket(this, options, listener)

/**
 * Websocket status code as defined by [Section
 * 7.4 of RFC 6455](http://tools.ietf.org/html/rfc6455#section-7.4)
 */
private const val STATUS_CODE_NORMAL_CLOSURE = 1000

private class OkHttpWebSocket(
    private val client: OkHttpClient,
    private val options: Options,
    private val listener: WebSocket.Listener<String>?
) : WebSocket<String, String> {

    override fun open() {
        socket = client.newWebSocket(
            options.toRequest(),
            OkHttpWebSocketListener()
        )
    }

    override val connectionEvents: Observable<ConnectionEvent>
        get() = connectionEventsSubject

    override fun close() {
        socket?.close(STATUS_CODE_NORMAL_CLOSURE, "${options.name} WebSocket deliberately stopped")
    }

    private val subject = PublishSubject.create<String>()
    private val connectionEventsSubject = PublishSubject.create<ConnectionEvent>()

    override val responses: Observable<String>
        get() = subject

    override fun send(message: String) {
        socket?.send(message)
    }

    @Volatile
    private var socket: okhttp3.WebSocket? = null

    private inner class OkHttpWebSocketListener : WebSocketListener() {

        override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            connectionEventsSubject.onNext(ConnectionEvent.Connected)
            listener?.onOpen()
        }

        override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            connectionEventsSubject.onNext(ConnectionEvent.ClientDisconnect)
            listener?.onClose()
        }

        override fun onMessage(webSocket: okhttp3.WebSocket, message: String) {
            super.onMessage(webSocket, message)
            subject.onNext(message)
            listener?.onMessage(message)
        }

        override fun onFailure(webSocket: okhttp3.WebSocket, t: Throwable, response: Response?) {
            super.onFailure(webSocket, t, response)
            connectionEventsSubject.onNext(ConnectionEvent.Failure(t))
        }
    }
}

private fun Options.toRequest() =
    Request.Builder()
        .url(url)
        .addHeader("Origin", origin)
        .build()

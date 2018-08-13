package com.blockchain.network.websocket

import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocketListener

class Options(
    val url: String,
    val origin: String
)

/**
 * Websocket status code as defined by [Section
 * 7.4 of RFC 6455](http://tools.ietf.org/html/rfc6455#section-7.4)
 */
private const val STATUS_CODE_NORMAL_CLOSURE = 1000

class OkHttpWebSocket(
    private val client: OkHttpClient,
    private val options: Options,
    private val listener: WebSocket.Listener<String> = WebSocket.NullListener
) : WebSocket<String, String> {

    override fun open() {
        socket = client.newWebSocket(
            options.toRequest(),
            OkHttpWebSocketListener()
        )
    }

    override fun close() {
        socket?.close(STATUS_CODE_NORMAL_CLOSURE, "Normal")
    }

    private val subject = PublishSubject.create<String>()

    override val responses: Observable<String>
        get() = subject

    override fun send(message: String) {
        socket?.send(message)
    }

    private var socket: okhttp3.WebSocket? = null

    private inner class OkHttpWebSocketListener : WebSocketListener() {

        override fun onOpen(webSocket: okhttp3.WebSocket, response: Response) {
            super.onOpen(webSocket, response)
            listener.onOpen()
        }

        override fun onClosed(webSocket: okhttp3.WebSocket, code: Int, reason: String) {
            super.onClosed(webSocket, code, reason)
            listener.onClose()
        }

        override fun onMessage(webSocket: okhttp3.WebSocket, message: String) {
            subject.onNext(message)
            listener.onMessage(message)
        }
    }
}

private fun Options.toRequest() =
    Request.Builder()
        .url(url)
        .addHeader("Origin", origin)
        .build()

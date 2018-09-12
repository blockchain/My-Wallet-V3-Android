package com.blockchain.morph.ui.homebrew.exchange

import com.blockchain.morph.ui.BuildConfig
import com.blockchain.network.websocket.ConnectionEvent
import com.blockchain.network.websocket.WebSocket
import io.reactivex.Observable
import timber.log.Timber

fun <OUTGOING, INCOMING> WebSocket<OUTGOING, INCOMING>.timber(label: String): WebSocket<OUTGOING, INCOMING> =
    if (BuildConfig.DEBUG) TimberWebSocket(label, this) else this

private class TimberWebSocket<OUTGOING, INCOMING>(
    private val label: String,
    private val inner: WebSocket<OUTGOING, INCOMING>
) : WebSocket<OUTGOING, INCOMING> {

    override fun open() {
        Timber.d("WebSocket $label Open called")
        inner.open()
    }

    override fun close() {
        Timber.d("WebSocket $label Close called")
        inner.close()
    }

    override val connectionEvents: Observable<ConnectionEvent>
        get() = inner.connectionEvents
            .doOnNext {
                when (it) {
                    is ConnectionEvent.Connected -> Timber.d("WebSocket $label Connected")
                    is ConnectionEvent.Failure -> Timber.e("WebSocket $label Failed ${it.throwable}")
                    is ConnectionEvent.ClientDisconnect -> Timber.e("WebSocket $label Client Disconnected")
                }
            }

    override fun send(message: OUTGOING) {
        Timber.v("WebSocket $label send $message")
        inner.send(message)
    }

    override val responses: Observable<INCOMING>
        get() = inner.responses
            .doOnNext {
                Timber.v("WebSocket $label receive $it")
            }
}

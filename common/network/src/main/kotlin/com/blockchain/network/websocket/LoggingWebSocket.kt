package com.blockchain.network.websocket

import com.blockchain.logging.Logger
import com.blockchain.logging.NullLogger
import io.reactivex.Observable
import org.koin.KoinContext
import org.koin.dsl.context.emptyParameters
import org.koin.standalone.StandAloneContext

fun <OUTGOING, INCOMING> WebSocket<OUTGOING, INCOMING>.debugLog(label: String): WebSocket<OUTGOING, INCOMING> =
    getLoggerFromKoin().let { logger ->
        if (logger == NullLogger) this else DebugLogWebSocket(label, this, logger)
    }

private fun getLoggerFromKoin(): Logger =
    (StandAloneContext.koinContext as KoinContext).get("", emptyParameters())

private class DebugLogWebSocket<OUTGOING, INCOMING>(
    private val label: String,
    private val inner: WebSocket<OUTGOING, INCOMING>,
    private val logger: Logger
) : WebSocket<OUTGOING, INCOMING> {

    override fun open() {
        logger.d("WebSocket $label Open called")
        inner.open()
    }

    override fun close() {
        logger.d("WebSocket $label Close called")
        inner.close()
    }

    override val connectionEvents: Observable<ConnectionEvent>
        get() = inner.connectionEvents
            .doOnNext {
                when (it) {
                    is ConnectionEvent.Connected -> logger.d("WebSocket $label Connected")
                    is ConnectionEvent.Failure -> logger.e("WebSocket $label Failed ${it.throwable}")
                    is ConnectionEvent.ClientDisconnect -> logger.e("WebSocket $label Client Disconnected")
                }
            }

    override fun send(message: OUTGOING) {
        logger.v("WebSocket $label send $message")
        inner.send(message)
    }

    override val responses: Observable<INCOMING>
        get() = inner.responses
            .doOnNext {
                logger.v("WebSocket $label receive $it")
            }
}

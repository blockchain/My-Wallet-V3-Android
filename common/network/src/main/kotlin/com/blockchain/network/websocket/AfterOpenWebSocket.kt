package com.blockchain.network.websocket

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign

fun <OUTGOING, INCOMING> WebSocket<OUTGOING, INCOMING>.afterOpen(
    afterOpenAction: (WebSocketSend<OUTGOING>) -> Disposable
): WebSocket<OUTGOING, INCOMING> = AfterOpenWebSocket(this, afterOpenAction)

private class AfterOpenWebSocket<OUTGOING, INCOMING>(
    private val inner: WebSocket<OUTGOING, INCOMING>,
    private val afterOpenAction: (WebSocketSend<OUTGOING>) -> Disposable
) : WebSocket<OUTGOING, INCOMING> by inner {

    private val connections = CompositeDisposable()

    override fun open() {
        connections.clear()
        connections += watchEvents()
        inner.open()
    }

    private fun watchEvents(): Disposable =
        connectionEvents
            .subscribe {
                if (it === ConnectionEvent.Connected) {
                    connections += afterOpenAction(this)
                }
            }

    override fun close() {
        connections.clear()
        inner.close()
    }
}

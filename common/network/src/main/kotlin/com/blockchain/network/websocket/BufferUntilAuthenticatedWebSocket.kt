package com.blockchain.network.websocket

import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.plusAssign
import java.util.LinkedList
import java.util.Queue
import java.util.concurrent.atomic.AtomicReference

fun <OUTGOING, INCOMING> WebSocket<OUTGOING, INCOMING>.bufferUntilAuthenticated(
    limit: Int = 10
): WebSocket<OUTGOING, INCOMING> =
    BufferUntilAuthenticated(this, limit)

private class BufferUntilAuthenticated<OUTGOING, INCOMING>(
    private val inner: WebSocket<OUTGOING, INCOMING>,
    private val limit: Int
) : WebSocket<OUTGOING, INCOMING> by inner {

    private fun newQueue(): Queue<OUTGOING> = LinkedList<OUTGOING>()

    private val buffer = AtomicReference<Queue<OUTGOING>?>(newQueue())

    private val connections = CompositeDisposable()

    override fun open() {
        connections.clear()
        connections += watchEvents()
        inner.open()
    }

    private fun watchEvents(): Disposable =
        connectionEvents
            .subscribe {
                when (it) {
                    ConnectionEvent.Authenticated -> {
                        buffer.getAndSet(null)?.let {
                            it.forEach { message ->
                                inner.send(message)
                            }
                        }
                    }
                    is ConnectionEvent.Failure, ConnectionEvent.ClientDisconnect -> {
                        startQueuing()
                    }
                }
            }

    override fun close() {
        startQueuing()
        connections.clear()
        inner.close()
    }

    private fun startQueuing() {
        buffer.compareAndSet(null, newQueue())
    }

    override fun send(message: OUTGOING) {
        val localBuffer = buffer.get()
        if (localBuffer == null) {
            inner.send(message)
        } else {
            localBuffer.add(message)
            while (localBuffer.size > limit) localBuffer.poll()
        }
    }
}

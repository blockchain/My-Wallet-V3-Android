package com.blockchain.network.websocket

import io.reactivex.Observable

interface WebSocketOpenClose {
    fun open()
    fun close()
}

interface WebSocket<INCOMING, in OUTGOING> : WebSocketOpenClose {
    fun send(message: OUTGOING)

    val responses: Observable<INCOMING>

    interface Listener<in INCOMING> {
        fun onOpen()
        fun onMessage(message: INCOMING)
        fun onClose()
    }

    companion object {
        val NullListener = object : Listener<Any> {
            override fun onOpen() {}

            override fun onMessage(message: Any) {}

            override fun onClose() {}
        }
    }
}

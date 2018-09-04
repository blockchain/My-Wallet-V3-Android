package com.blockchain.network.websocket

import org.junit.Assert.assertTrue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class MessageWaiter(numberOfMessages: Int) : WebSocket.Listener<Any> by WebSocket.NullListener {

    private val messageLatch = CountDownLatch(numberOfMessages)

    override fun onMessage(message: Any) {
        messageLatch.countDown()
    }

    fun waitForAllMessages() {
        assertTrue("Never got all the messages", messageLatch.await(1, TimeUnit.SECONDS))
    }
}

class ConnectWaiter(numberOfConnects: Int = 1) : WebSocket.Listener<Any> by WebSocket.NullListener {

    private val messageLatch = CountDownLatch(numberOfConnects)

    override fun onOpen() {
        messageLatch.countDown()
    }

    fun waitForAllConnects() {
        assertTrue("Didn't connect enough times", messageLatch.await(1, TimeUnit.SECONDS))
    }
}

class CloseWaiter(numberOfCloses: Int = 1) : WebSocket.Listener<Any> by WebSocket.NullListener {

    private val messageLatch = CountDownLatch(numberOfCloses)

    override fun onClose() {
        messageLatch.countDown()
    }

    fun waitForAllCloses() {
        assertTrue("Didn't close enough times", messageLatch.await(1, TimeUnit.SECONDS))
    }
}

operator fun <INCOMING> WebSocket.Listener<INCOMING>.plus(secondListener: WebSocket.Listener<INCOMING>) =
    object : WebSocket.Listener<INCOMING> {
        override fun onOpen() {
            this@plus.onOpen()
            secondListener.onOpen()
        }

        override fun onMessage(message: INCOMING) {
            this@plus.onMessage(message)
            secondListener.onMessage(message)
        }

        override fun onClose() {
            this@plus.onClose()
            secondListener.onClose()
        }
    }

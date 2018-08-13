package com.blockchain.network.websocket

import com.blockchain.testutils.after
import com.blockchain.testutils.before
import io.fabric8.mockwebserver.DefaultMockServer
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

fun DefaultMockServer.initRule() = mockWebServerInit(this)

fun mockWebServerInit(server: DefaultMockServer) =
    before {
        server.start()
    } after {
        server.shutdown()
    }

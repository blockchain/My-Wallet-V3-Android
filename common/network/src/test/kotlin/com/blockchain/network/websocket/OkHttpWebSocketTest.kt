package com.blockchain.network.websocket

import com.blockchain.network.initRule
import io.fabric8.mockwebserver.DefaultMockServer
import okhttp3.OkHttpClient
import org.amshove.kluent.`should equal`
import org.junit.Rule
import org.junit.Test

class OkHttpWebSocketTest {

    private val server = DefaultMockServer()

    @get:Rule
    private val initMockServer = server.initRule()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    @Test
    fun `can send and receive one message`() {
        server.expect().get().withPath("/service")
            .andUpgradeToWebSocket()
            .open()
            .expect("subscribe").andEmit("SUBSCRIBED").once()
            .done()
            .once()

        val waiter = MessageWaiter(1)
        okHttpClient.newBlockchainWebSocket(
            getOptions("/service"),
            waiter
        ).apply {
            val test = responses.test()
            open()
            send("subscribe")
            waiter.waitForAllMessages()
            test.values() `should equal` listOf("SUBSCRIBED")
        }
    }

    @Test
    fun `can send and receive two messages`() {
        server.expect().get().withPath("/service2")
            .andUpgradeToWebSocket()
            .open()
            .expect("subscribeA").andEmit("SUBSCRIBED_A").once()
            .expect("subscribeB").andEmit("SUBSCRIBED_B").once()
            .done()
            .once()

        val waiter = MessageWaiter(2)
        okHttpClient.newBlockchainWebSocket(
            getOptions("/service2"),
            waiter
        ).apply {
            val test = responses.test()
            open()
            send("subscribeA")
            send("subscribeB")
            waiter.waitForAllMessages()
            test.values() `should equal` listOf("SUBSCRIBED_A", "SUBSCRIBED_B")
        }
    }

    private fun getOptions(path: String): Options {
        return Options(url = server.url(path), origin = "https://blockchain.info")
    }
}

package com.blockchain.network.websocket

import com.blockchain.network.initRule
import io.fabric8.mockwebserver.DefaultMockServer
import okhttp3.OkHttpClient
import org.amshove.kluent.`should equal`
import org.junit.Rule
import org.junit.Test

class OkHttpWebSocketConnectionEventTests {

    private val server = DefaultMockServer()

    @get:Rule
    private val initMockServer = server.initRule()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    private fun getOptions(path: String): Options {
        return Options(url = server.url(path), origin = "https://blockchain.info")
    }

    @Test
    fun `connect event`() {
        server.expect().get().withPath("/service")
            .andUpgradeToWebSocket()
            .open()
            .expect("subscribe").andEmit("SUBSCRIBED").once()
            .done()
            .once()

        val waiter = ConnectWaiter(1)
        okHttpClient.newBlockchainWebSocket(
            getOptions("/service"),
            waiter
        ).apply {
            val test = connectionEvents.test()
            open()
            waiter.waitForAllConnects()
            test.values().first() `should equal` ConnectionEvent.Connected
        }
    }

    @Test
    fun `close event`() {
        server.expect().get().withPath("/service")
            .andUpgradeToWebSocket()
            .open()
            .expect("subscribe").andEmit("SUBSCRIBED").once()
            .done()
            .once()

        val waiter = CloseWaiter(1)
        okHttpClient.newBlockchainWebSocket(
            getOptions("/service"),
            waiter
        ).apply {
            val test = connectionEvents.test()
            open()
            close()
            waiter.waitForAllCloses()
            test.values().last() `should equal` ConnectionEvent.ClientDisconnect
        }
    }

    @Test
    fun `multiple open and close events`() {
        server.expect().get().withPath("/service")
            .andUpgradeToWebSocket()
            .open()
            .done()
            .times(2)

        val firstWaiter = CloseWaiter(1)
        val secondWaiter = CloseWaiter(2)
        okHttpClient.newBlockchainWebSocket(
            getOptions("/service"),
            firstWaiter + secondWaiter
        ).apply {
            val test = connectionEvents.test()
            open()
            close()
            firstWaiter.waitForAllCloses()
            open()
            close()
            secondWaiter.waitForAllCloses()
            test.values() `should equal` listOf(
                ConnectionEvent.Connected,
                ConnectionEvent.ClientDisconnect,
                ConnectionEvent.Connected,
                ConnectionEvent.ClientDisconnect
            )
        }
    }
}

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

        val openWaiter = ConnectWaiter(1)
        val closeWaiter = CloseWaiter(1)
        okHttpClient.newBlockchainWebSocket(
            getOptions("/service"),
            openWaiter + closeWaiter
        ).apply {
            val test = connectionEvents.test()
            open()
            openWaiter.waitForAllConnects()
            Thread.sleep(50)
            close()
            closeWaiter.waitForAllCloses()
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
        val firstOpenWaiter = ConnectWaiter(1)
        val firstCloseWaiter = CloseWaiter(1)
        val secondOpenWaiter = ConnectWaiter(2)
        val secondCloseWaiter = CloseWaiter(2)
        okHttpClient.newBlockchainWebSocket(
            getOptions("/service"),
            firstOpenWaiter + firstCloseWaiter + secondOpenWaiter + secondCloseWaiter
        ).autoRetry().apply {
            val test = connectionEvents.test()
            open()
            firstOpenWaiter.waitForAllConnects()
            Thread.sleep(50)
            close()
            firstCloseWaiter.waitForAllCloses()
            open()
            secondOpenWaiter.waitForAllConnects()
            Thread.sleep(50)
            close()
            secondCloseWaiter.waitForAllCloses()
            test.values() `should equal` listOf(
                ConnectionEvent.Connected,
                ConnectionEvent.ClientDisconnect,
                ConnectionEvent.Connected,
                ConnectionEvent.ClientDisconnect
            )
        }
    }
}

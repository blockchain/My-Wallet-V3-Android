package com.blockchain.network.websocket

import com.blockchain.network.initRule
import com.squareup.moshi.Moshi
import io.fabric8.mockwebserver.DefaultMockServer
import okhttp3.OkHttpClient
import org.amshove.kluent.`should equal`
import org.junit.Rule
import org.junit.Test

class OkHttpWebSocketJsonIntegrationTest {

    @Suppress("unused")
    class ClientMessage(val data1: String, val data2: Int)

    data class ServerMessage(val data3: String, val data4: Int)

    private val server = DefaultMockServer()

    @get:Rule
    private val initMockServer = server.initRule()

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder().build()

    private val moshi = Moshi.Builder().build()

    @Test
    fun `can send and receive one message`() {
        server.expect().get().withPath("/service")
            .andUpgradeToWebSocket()
            .open()
            .expect("{\"data1\":\"Subscribe\",\"data2\":1}").andEmit("{\"data3\":\"OK\",\"data4\":2}").once()
            .done()
            .once()

        val waiter = MessageWaiter(1)
        okHttpClient.newBlockchainWebSocket(
            getOptions("/service"),
            waiter
        ).toJsonSocket<ClientMessage, ServerMessage>(moshi)
            .apply {
                val test = responses.test()
                open()
                send(ClientMessage(data1 = "Subscribe", data2 = 1))
                waiter.waitForAllMessages()
                test.values() `should equal` listOf(ServerMessage(data3 = "OK", data4 = 2))
            }
    }

    private fun getOptions(path: String): Options {
        return Options(url = server.url(path), origin = "https://blockchain.info")
    }
}

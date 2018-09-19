package com.blockchain.network.websocket

import org.amshove.kluent.`should equal`
import org.junit.Test

class WebSocketOptionsTest {

    @Test
    fun `the default origin is blockchain dot info`() {
        Options("wss://anyUrl").origin `should equal` "https://blockchain.info"
    }
}

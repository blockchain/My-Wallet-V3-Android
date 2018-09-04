package com.blockchain.network

import com.blockchain.testutils.after
import com.blockchain.testutils.before
import io.fabric8.mockwebserver.DefaultMockServer

fun DefaultMockServer.initRule() = mockWebServerInit(this)

fun mockWebServerInit(server: DefaultMockServer) =
    before {
        server.start()
    } after {
        server.shutdown()
    }

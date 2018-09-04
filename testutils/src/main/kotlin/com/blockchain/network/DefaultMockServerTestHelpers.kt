package com.blockchain.network

import com.blockchain.testutils.after
import com.blockchain.testutils.before
import io.fabric8.mockwebserver.DefaultMockServer

fun DefaultMockServer.initRule() =
    before {
        this.start()
    } after {
        this.shutdown()
    }

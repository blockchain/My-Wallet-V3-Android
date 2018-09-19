package com.blockchain.koin.modules

import com.blockchain.network.websocket.Options
import org.koin.dsl.module.applicationContext

val nabuUrlModule = applicationContext {

    bean("nabu") {
        Options(
            name = "Nabu",
            url = getProperty(WebSocketUrlKeys.Nabu)
        )
    }
}

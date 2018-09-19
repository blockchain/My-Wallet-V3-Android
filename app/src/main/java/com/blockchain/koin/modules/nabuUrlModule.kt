package com.blockchain.koin.modules

import com.blockchain.network.websocket.Options
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.BuildConfig

val nabuUrlModule = applicationContext {

    bean("nabu") {
        Options(
            name = "Nabu",
            url = BuildConfig.NABU_WEBSOCKET_URL
        )
    }
}

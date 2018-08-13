package com.blockchain.koin.modules

import com.blockchain.network.modules.MoshiBuilderInterceptor
import org.koin.dsl.module.applicationContext

val moshiModule = applicationContext {

    bean {
        listOf<MoshiBuilderInterceptor>(
            get("buySell"),
            get("kyc")
        )
    }
}

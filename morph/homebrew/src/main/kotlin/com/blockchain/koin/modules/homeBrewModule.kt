package com.blockchain.koin.modules

import com.blockchain.koin.moshiInterceptor
import com.blockchain.morph.homebrew.json.OutAdapter
import org.koin.dsl.module.applicationContext

val homeBrewModule = applicationContext {

    moshiInterceptor("homeBrew") { builder ->
        builder.add(OutAdapter())
    }
}

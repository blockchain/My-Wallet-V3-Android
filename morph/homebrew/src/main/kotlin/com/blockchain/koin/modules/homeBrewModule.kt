package com.blockchain.koin.modules

import com.blockchain.morph.homebrew.json.OutAdapter
import com.blockchain.network.modules.MoshiBuilderInterceptor
import com.squareup.moshi.Moshi
import org.koin.dsl.module.applicationContext

val homeBrewModule = applicationContext {

    bean("homeBrew") {
        object : MoshiBuilderInterceptor {
            override fun intercept(builder: Moshi.Builder) {
                builder
                    .add(OutAdapter())
            }
        } as MoshiBuilderInterceptor
    }
}

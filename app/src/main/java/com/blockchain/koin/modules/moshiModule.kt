package com.blockchain.koin.modules

import com.blockchain.network.modules.MoshiBuilderInterceptor
import com.squareup.moshi.Moshi
import org.koin.dsl.module.applicationContext
import retrofit2.converter.moshi.MoshiConverterFactory

val moshiModule = applicationContext {

    bean {
        listOf<MoshiBuilderInterceptor>(
            get("buySell")
        )
    }

    bean {
        MoshiConverterFactory.create(
            Moshi.Builder()
                .also {
                    get<List<MoshiBuilderInterceptor>>()
                        .forEach { interceptor -> interceptor.intercept(it) }
                }
                .build()
        )
    }
}

package com.blockchain.koin

import com.blockchain.network.modules.MoshiBuilderInterceptor
import com.squareup.moshi.Moshi
import org.koin.dsl.context.Context

fun Context.moshiInterceptor(name: String, function: (builder: Moshi.Builder) -> Unit) =
    bean(name) {
        object : MoshiBuilderInterceptor {
            override fun intercept(builder: Moshi.Builder) {
                function(builder)
            }
        } as MoshiBuilderInterceptor
    }

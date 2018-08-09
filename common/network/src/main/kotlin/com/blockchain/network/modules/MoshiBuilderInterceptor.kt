package com.blockchain.network.modules

import com.squareup.moshi.Moshi

interface MoshiBuilderInterceptor {
    fun intercept(builder: Moshi.Builder)
}
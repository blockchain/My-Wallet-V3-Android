package com.blockchain.network.modules

import com.squareup.moshi.Moshi

class MoshiBuilderInterceptorList(list: List<MoshiBuilderInterceptor>) : List<MoshiBuilderInterceptor> by list

interface MoshiBuilderInterceptor {
    fun intercept(builder: Moshi.Builder)
}
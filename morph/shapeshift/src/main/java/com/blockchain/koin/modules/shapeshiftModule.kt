package com.blockchain.koin.modules

import info.blockchain.wallet.shapeshift.ShapeShiftEndpoints
import info.blockchain.wallet.shapeshift.ShapeShiftUrls
import org.koin.dsl.module.applicationContext
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.jackson.JacksonConverterFactory

val shapeShiftModule = applicationContext {

    bean("shapeshift") {
        Retrofit.Builder()
            .baseUrl(ShapeShiftUrls.SHAPESHIFT_URL)
            .client(get())
            .addConverterFactory(get<JacksonConverterFactory>())
            .addCallAdapterFactory(get<RxJava2CallAdapterFactory>())
            .build()
    }

    bean {
        get<Retrofit>("shapeshift").create(ShapeShiftEndpoints::class.java)
    }
}
package com.blockchain.koin.modules

import com.blockchain.morph.regulation.UsStatesDataManager
import com.blockchain.morph.trade.MorphTradeDataManager
import info.blockchain.wallet.shapeshift.ShapeShiftApi
import info.blockchain.wallet.shapeshift.ShapeShiftEndpoints
import info.blockchain.wallet.shapeshift.ShapeShiftUrls
import info.blockchain.wallet.shapeshift.regulation.ShapeShiftUsStatesDataManager
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManagerAdapter
import piuk.blockchain.androidcore.data.shapeshift.datastore.ShapeShiftDataStore
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

    factory { ShapeShiftApi(get()) }

    context("Payload") {

        bean { ShapeShiftDataStore() }

        factory { ShapeShiftDataManager(get(), get(), get(), get()) }

        factory("shapeshift") { ShapeShiftDataManagerAdapter(get()) as MorphTradeDataManager }

        factory {
            ShapeShiftUsStatesDataManager(get(), get()) as UsStatesDataManager
        }
    }
}

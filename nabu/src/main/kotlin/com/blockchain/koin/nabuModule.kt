package com.blockchain.koin

import com.blockchain.nabu.api.NabuMarkets
import com.blockchain.nabu.service.NabuMarketsService
import org.koin.dsl.module.applicationContext
import retrofit2.Retrofit

val nabuModule = applicationContext {

    bean { get<Retrofit>("nabu").create(NabuMarkets::class.java) }

    bean { NabuMarketsService(get(), get()) }
}

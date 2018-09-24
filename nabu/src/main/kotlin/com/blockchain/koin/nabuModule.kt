package com.blockchain.koin

import com.blockchain.morph.exchange.service.TradeLimitService
import com.blockchain.morph.trade.MorphTradeDataManager
import com.blockchain.nabu.api.NabuMarkets
import com.blockchain.nabu.api.TransactionStateAdapter
import com.blockchain.nabu.datamanagers.NabuDataManagerAdapter
import com.blockchain.nabu.service.NabuMarketsService
import org.koin.dsl.module.applicationContext
import retrofit2.Retrofit

val nabuModule = applicationContext {

    bean { get<Retrofit>("nabu").create(NabuMarkets::class.java) }

    context("Payload") {

        factory { NabuMarketsService(get(), get()) }
            .bind(TradeLimitService::class)

        factory("nabu") { NabuDataManagerAdapter(get()) as MorphTradeDataManager }
    }

    moshiInterceptor("nabu") { builder ->
        builder.add(TransactionStateAdapter())
    }
}

package com.blockchain.koin.modules

import com.blockchain.morph.QuoteWebSocketServiceFactory
import com.blockchain.morph.exchange.service.QuoteServiceFactory
import com.blockchain.morph.exchange.service.TradeExecutionService
import com.blockchain.morph.homebrew.HomeBrewTradeExecutionService
import org.koin.dsl.module.applicationContext

val homeBrewModule = applicationContext {

    context("Payload") {

        factory {
            QuoteWebSocketServiceFactory(get("nabu"), get(), get(), get()) as QuoteServiceFactory
        }

        factory {
            HomeBrewTradeExecutionService(get()) as TradeExecutionService
        }
    }
}

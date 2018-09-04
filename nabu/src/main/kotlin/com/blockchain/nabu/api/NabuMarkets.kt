package com.blockchain.nabu.api

import io.reactivex.Single
import retrofit2.http.GET
import retrofit2.http.Path

interface NabuMarkets {

    @GET("nabu-app/markets/quotes/{trading_pair}/config")
    fun getTradingConfig(
        @Path("trading_pair") tradingPair: String
    ): Single<TradingConfig>
}

package com.blockchain.nabu.api

import io.reactivex.Single
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

internal interface NabuMarkets {

    @GET("markets/quotes/{trading_pair}/config")
    fun getTradingConfig(
        @Path("trading_pair") tradingPair: String,
        @Header("authorization") authorization: String
    ): Single<TradingConfig>

    @GET("trades/limits")
    fun getTradesLimits(
        @Header("authorization") authorization: String
    ): Single<TradesLimits>

    @POST("trades")
    fun executeTrade(
        @Body tradeRequest: TradeRequest,
        @Header("authorization") authorization: String
    ): Single<TradeJson>

    @GET("trades")
    fun getTrades(
        @Header("authorization") authorization: String
    ): Single<List<TradeJson>>
}

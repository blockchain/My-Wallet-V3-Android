package com.blockchain.morph.trade

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

interface MorphTradeDataManager : MorphTradeDataHistoryList {

    fun findTrade(depositAddress: String): Single<MorphTrade>

    fun updateTrade(
        orderId: String,
        newStatus: MorphTrade.Status,
        newHashOut: String?
    ): Completable

    fun getTradeStatus(depositAddress: String): Observable<MorphTradeStatus>
}

interface MorphTradeDataHistoryList {

    fun getTrades(): Single<List<MorphTrade>>
}

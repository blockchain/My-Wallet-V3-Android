package com.blockchain.nabu.datamanagers

import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeDataManager
import com.blockchain.morph.trade.MorphTradeStatus
import com.blockchain.nabu.dataadapters.NabuTradeAdapter
import com.blockchain.nabu.dataadapters.NabuTradeStatusResponseAdapter
import com.blockchain.nabu.service.NabuMarketsService
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single

internal class NabuDataManagerAdapter(
    private val nabuMarketsService: NabuMarketsService
) : MorphTradeDataManager {

    override fun findTrade(depositAddress: String): Single<MorphTrade> =
        nabuMarketsService
            .getTrades()
            .flattenAsObservable { it }
            .filter { it.depositAddress == depositAddress }
            .map<MorphTrade> { NabuTradeAdapter(it) }
            .singleOrError()

    override fun getTrades(): Single<List<MorphTrade>> =
        nabuMarketsService
            .getTrades()
            .flattenAsObservable { it }
            .map<MorphTrade> { NabuTradeAdapter(it) }
            .toList()

    override fun getTradeStatus(depositAddress: String): Observable<MorphTradeStatus> =
        nabuMarketsService
            .getTrades()
            .flattenAsObservable { it }
            .filter { it.depositAddress == depositAddress }
            .map<MorphTradeStatus> {
                NabuTradeStatusResponseAdapter(
                    it
                )
            }

    override fun updateTrade(
        orderId: String,
        newStatus: MorphTrade.Status,
        newHashOut: String?
    ): Completable = Completable.complete()
}
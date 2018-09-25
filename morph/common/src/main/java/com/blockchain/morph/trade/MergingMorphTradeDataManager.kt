package com.blockchain.morph.trade

import io.reactivex.Single
import io.reactivex.functions.BiFunction

class MergingMorphTradeDataManager(
    private val firstTradeManager: MorphTradeDataHistoryList,
    private val secondTradeDataManager: MorphTradeDataHistoryList
) : MorphTradeDataHistoryList {

    override fun getTrades(): Single<List<MorphTrade>> = Single.zip(
        firstTradeManager.getTrades()
            .returnEmptyIfFailed(),
        secondTradeDataManager.getTrades()
            .returnEmptyIfFailed(),
        BiFunction { nabuTrades: List<MorphTrade>, shapeShiftTrades: List<MorphTrade> ->
            mutableListOf<MorphTrade>().apply {
                addAll(nabuTrades)
                addAll(shapeShiftTrades)
            }.toList()
                .sortedByDescending { it.timestamp }
                .toList()
        }
    )

    private fun Single<List<MorphTrade>>.returnEmptyIfFailed(): Single<List<MorphTrade>> =
        this.doOnError { it.printStackTrace() }
            .onErrorReturn { emptyList() }
}
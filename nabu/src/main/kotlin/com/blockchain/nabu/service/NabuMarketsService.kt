package com.blockchain.nabu.service

import com.blockchain.morph.CoinPair
import com.blockchain.nabu.api.NabuMarkets
import info.blockchain.balance.CryptoValue
import io.reactivex.Single

class NabuMarketsService internal constructor(
    private val nabuMarkets: NabuMarkets
) {

    fun getTradingConfig(tradingPair: CoinPair): Single<TradingConfig> {
        return nabuMarkets.getTradingConfig(
            tradingPair.pairCodeUpper
        ).map {
            TradingConfig(minOrderSize = CryptoValue.fromMajor(tradingPair.from, it.minOrderSize.toBigDecimal()))
        }
    }
}

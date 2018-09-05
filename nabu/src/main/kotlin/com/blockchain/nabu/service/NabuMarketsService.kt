package com.blockchain.nabu.service

import com.blockchain.morph.CoinPair
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.api.NabuMarkets
import info.blockchain.balance.CryptoValue
import io.reactivex.Single

class NabuMarketsService internal constructor(
    private val nabuMarkets: NabuMarkets,
    private val authenticator: Authenticator
) {

    fun getTradingConfig(tradingPair: CoinPair): Single<TradingConfig> {
        return authenticator.authenticate {
            nabuMarkets.getTradingConfig(
                tradingPair.pairCodeUpper,
                it.authHeader
            ).map {
                TradingConfig(minOrderSize = CryptoValue.fromMajor(tradingPair.from, it.minOrderSize))
            }
        }
    }
}

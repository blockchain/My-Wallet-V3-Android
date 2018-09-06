package com.blockchain.nabu.service

import com.blockchain.morph.CoinPair
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.api.NabuMarkets
import com.blockchain.nabu.api.PeriodicLimit
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
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

    fun getTradesLimits(): Single<FiatTradesLimits> {
        return authenticator.authenticate {
            nabuMarkets.getTradesLimits(
                it.authHeader
            ).map {
                FiatTradesLimits(
                    minOrder = FiatValue.fromMajor(it.currency, it.minOrder),
                    maxOrder = FiatValue.fromMajor(it.currency, it.maxOrder),
                    maxPossibleOrder = FiatValue.fromMajor(it.currency, it.maxPossibleOrder),
                    daily = it.daily.toFiat(it.currency),
                    weekly = it.weekly.toFiat(it.currency),
                    annual = it.annual.toFiat(it.currency)
                )
            }
        }
    }
}

private fun PeriodicLimit.toFiat(currencyCode: String) =
    FiatPeriodicLimit(
        limit = FiatValue.fromMajor(currencyCode, limit),
        available = FiatValue.fromMajor(currencyCode, available),
        used = FiatValue.fromMajor(currencyCode, used)
    )

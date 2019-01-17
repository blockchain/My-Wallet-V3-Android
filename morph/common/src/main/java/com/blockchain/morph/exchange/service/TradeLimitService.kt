package com.blockchain.morph.exchange.service

import info.blockchain.balance.FiatValue
import info.blockchain.balance.compareTo
import io.reactivex.Single

interface TradeLimitService {

    fun getTradesLimits(fiatCurrency: String): Single<FiatTradesLimits>
}

class FiatTradesLimits(
    val minOrder: FiatValue,
    val maxOrder: FiatValue,
    val maxPossibleOrder: FiatValue,
    val daily: FiatPeriodicLimit,
    val weekly: FiatPeriodicLimit,
    val annual: FiatPeriodicLimit
) {
    fun minAvailable(): FiatValue {
        var min = daily.available

        if (weekly.available != null && (min == null || weekly.available < min)) {
            min = weekly.available
        }

        if (annual.available != null && (min == null || annual.available < min)) {
            min = annual.available
        }

        return min ?: minOrder.toZero()
    }
}

class FiatPeriodicLimit(
    val limit: FiatValue?,
    val available: FiatValue?,
    val used: FiatValue?
)

package com.blockchain.nabu.service

import info.blockchain.balance.FiatValue

class FiatTradesLimits(
    val minOrder: FiatValue,
    val maxOrder: FiatValue,
    val maxPossibleOrder: FiatValue,
    val daily: FiatPeriodicLimit,
    val weekly: FiatPeriodicLimit,
    val annual: FiatPeriodicLimit
)

class FiatPeriodicLimit(
    val limit: FiatValue,
    val available: FiatValue,
    val used: FiatValue
)

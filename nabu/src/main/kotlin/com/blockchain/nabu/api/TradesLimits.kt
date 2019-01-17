package com.blockchain.nabu.api

import com.blockchain.serialization.JsonSerializable
import java.math.BigDecimal

internal class TradesLimits(
    val currency: String,
    val minOrder: BigDecimal,
    val maxOrder: BigDecimal,
    val maxPossibleOrder: BigDecimal,
    val daily: PeriodicLimit?,
    val weekly: PeriodicLimit?,
    val annual: PeriodicLimit?
) : JsonSerializable

internal class PeriodicLimit(
    val limit: BigDecimal?,
    val available: BigDecimal?,
    val used: BigDecimal?
) : JsonSerializable

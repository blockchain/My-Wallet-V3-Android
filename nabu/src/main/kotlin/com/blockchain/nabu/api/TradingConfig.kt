package com.blockchain.nabu.api

import com.blockchain.serialization.JsonSerializable
import java.math.BigDecimal

internal class TradingConfig(
    val minOrderSize: BigDecimal
) : JsonSerializable

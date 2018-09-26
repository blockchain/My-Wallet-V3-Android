package com.blockchain.morph.homebrew

import com.blockchain.serialization.JsonSerializable
import java.math.BigDecimal

internal data class ExchangeRateJson(
    val rates: List<Rate>?
) : JsonSerializable

internal data class Rate(
    val pair: String,
    val price: BigDecimal
) : JsonSerializable

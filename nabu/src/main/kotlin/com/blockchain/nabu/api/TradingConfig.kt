package com.blockchain.nabu.api

import com.blockchain.serialization.JsonSerializable

internal class TradingConfig(
    val minOrderSize: String
) : JsonSerializable

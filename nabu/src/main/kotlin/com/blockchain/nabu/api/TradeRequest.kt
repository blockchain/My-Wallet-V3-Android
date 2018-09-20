package com.blockchain.nabu.api

import com.blockchain.serialization.JsonSerializable

data class TradeRequest(
    val destinationAddress: String,
    val refundAddress: String,
    val quote: QuoteJson
) : JsonSerializable

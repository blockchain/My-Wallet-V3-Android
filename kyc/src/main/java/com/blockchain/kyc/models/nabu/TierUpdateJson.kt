package com.blockchain.kyc.models.nabu

import com.blockchain.serialization.JsonSerializable

internal data class TierUpdateJson(
    val selectedTier: Int
) : JsonSerializable

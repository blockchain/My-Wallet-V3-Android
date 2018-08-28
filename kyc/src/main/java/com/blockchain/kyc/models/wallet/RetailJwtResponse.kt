package com.blockchain.kyc.models.wallet

import com.squareup.moshi.Json

internal data class RetailJwtResponse(
    @field:Json(name = "success") val isSuccessful: Boolean,
    val token: String?,
    val error: String?
)
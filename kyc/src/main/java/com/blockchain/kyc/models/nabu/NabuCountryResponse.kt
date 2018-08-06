package com.blockchain.kyc.models.nabu

data class NabuCountryResponse(
    val code: String,
    val name: String,
    val regions: List<String>
)
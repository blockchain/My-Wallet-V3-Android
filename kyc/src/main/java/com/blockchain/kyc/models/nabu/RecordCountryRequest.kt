package com.blockchain.kyc.models.nabu

internal data class RecordCountryRequest(
    val jwt: String,
    val countryCode: String,
    val notifyWhenAvailable: Boolean
)
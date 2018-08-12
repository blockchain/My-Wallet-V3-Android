package com.blockchain.kyc.models.nabu
import com.squareup.moshi.Json

data class NabuBasicUser(
    val id: String,
    @Json(name = "firstname") val firstName: String,
    @Json(name = "lastname") val lastName: String,
    val email: String,
    val dateOfBirth: String
)
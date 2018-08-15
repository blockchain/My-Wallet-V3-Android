package com.blockchain.kyc.models.nabu

import com.squareup.moshi.Json

data class NabuBasicUser(
    val id: String,
    val firstName: String,
    val lastName: String,
    val email: String,
    @field:Json(name = "dob") val dateOfBirth: String
)
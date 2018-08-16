package com.blockchain.kyc.models.nabu

import com.squareup.moshi.Json

data class NabuBasicUser(
    val firstName: String,
    val lastName: String,
    @field:Json(name = "dob") val dateOfBirth: String
)
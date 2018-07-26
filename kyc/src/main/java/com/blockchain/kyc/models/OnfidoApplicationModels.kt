package com.blockchain.kyc.models

import com.squareup.moshi.Json

data class ApplicantRequest(
    @field:Json(name = "first_name") val firstName: String,
    @field:Json(name = "last_name") val lastName: String
)

data class ApplicantResponse(
    val id: String,
    @field:Json(name = "created_at") val createdAt: String,
    val sandbox: Boolean,
    @field:Json(name = "first_name") val firstName: String,
    @field:Json(name = "last_name") val lastName: String,
    val country: String
)
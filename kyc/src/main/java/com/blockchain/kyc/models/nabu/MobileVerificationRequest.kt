package com.blockchain.kyc.models.nabu

import com.squareup.moshi.Json

data class MobileVerificationRequest(
    @field:Json(name = "value") val phoneNumber: String,
    @field:Json(name = "code") val verificationCode: String,
    val type: String = "MOBILE"
)
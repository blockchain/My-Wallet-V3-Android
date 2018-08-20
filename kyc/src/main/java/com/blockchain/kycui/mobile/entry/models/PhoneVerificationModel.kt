package com.blockchain.kycui.mobile.entry.models

data class PhoneVerificationModel(
    val sanitizedPhoneNumber: String,
    val verificationCode: String
)
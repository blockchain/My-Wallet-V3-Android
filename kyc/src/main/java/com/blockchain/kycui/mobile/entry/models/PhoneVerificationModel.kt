package com.blockchain.kycui.mobile.entry.models

import com.blockchain.kycui.mobile.validation.models.VerificationCode

data class PhoneVerificationModel(
    val sanitizedPhoneNumber: String,
    val verificationCode: VerificationCode
)
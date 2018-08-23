package com.blockchain.kycui.mobile.validation.models

class VerificationCode(verificationCode: String) {
    val code = verificationCode.toUpperCase()
    val isValid = verificationCode.length >= 5
}
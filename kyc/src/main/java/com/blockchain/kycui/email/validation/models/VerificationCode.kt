package com.blockchain.kycui.email.validation.models

class VerificationCode(verificationCode: String) {
    val code = verificationCode.toUpperCase()
    val isValid = verificationCode.length >= 5
}
package com.blockchain.kycui.mobile.validation.models

class VerificationCode(val verificationCode: String) {
    val isValid = verificationCode.length >= 5
}
package com.blockchain.kycui.mobile.entry.models

class PhoneNumber(val raw: String) {
    val sanitized = "+${raw.replace("[^\\d.]".toRegex(), "")}"
    val isValid = sanitized.length >= 9
}
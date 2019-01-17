package piuk.blockchain.androidcore.data.settings

class PhoneNumber(val raw: String) {
    val sanitized = "+${raw.replace("[^\\d.]".toRegex(), "")}"
    val isValid = sanitized.length >= 9
}
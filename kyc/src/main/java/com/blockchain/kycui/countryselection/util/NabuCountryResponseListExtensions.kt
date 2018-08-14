package com.blockchain.kycui.countryselection.util

import com.blockchain.kyc.models.nabu.NabuCountryResponse
import java.util.Locale

private const val asciiOffset = 0x41
private const val flagOffset = 0x1F1E6

fun List<NabuCountryResponse>.toDisplayList(locale: Locale): List<CountryDisplayModel> = this.map {
    CountryDisplayModel(
        Locale(locale.displayLanguage, it.code).displayCountry,
        it.code,
        getFlagEmojiFromCountryCode(it.code)
    )
}.sortedWith(compareBy { it.name })

private fun getFlagEmojiFromCountryCode(countryCode: String): String {
    val firstChar = Character.codePointAt(countryCode, 0) - asciiOffset + flagOffset
    val secondChar = Character.codePointAt(countryCode, 1) - asciiOffset + flagOffset
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}

data class CountryDisplayModel(
    val name: String,
    val countryCode: String,
    val flag: String
)
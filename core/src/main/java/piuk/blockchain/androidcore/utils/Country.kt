package piuk.blockchain.androidcore.utils

import java.util.Locale

class CountryHelper(val locale: Locale) {

    val countryList: List<Country> = Locale.getISOCountries().map {
        Country(
            Locale(locale.displayLanguage, it).displayCountry,
            it,
            getFlagEmojiFromCountryCode(it)
        )
    }.sortedWith(compareBy { it.name })

    private fun getFlagEmojiFromCountryCode(countryCode: String): String {
        val firstChar = Character.codePointAt(countryCode, 0) - asciiOffset + flagOffset
        val secondChar = Character.codePointAt(countryCode, 1) - asciiOffset + flagOffset
        return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
    }

    companion object {
        private const val asciiOffset = 0x41
        private const val flagOffset = 0x1F1E6
    }
}

data class Country(
    val name: String,
    val countryCode: String,
    val flag: String
)
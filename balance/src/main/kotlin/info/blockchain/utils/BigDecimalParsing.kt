package info.blockchain.utils

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale

/**
 * Tries to parse a string as a [BigDecimal].
 * Ignores all non-digit and non-separator characters.
 * If it fails to parse, it will return null.
 */
fun String.tryParseBigDecimal(locale: Locale = Locale.getDefault()) =
    try {
        parseBigDecimal(locale)
    } catch (_: ParseException) {
        null
    }

/**
 * Parses a string as a [BigDecimal].
 * Ignores all non-digit and non-separator characters.
 */
fun String.parseBigDecimal(locale: Locale): BigDecimal {
    val format = NumberFormat.getNumberInstance(locale)
    if (format is DecimalFormat) {
        format.isParseBigDecimal = true
    }
    return format.parse(this.replace(findInvalidCharacters, "")) as BigDecimal
}

private val findInvalidCharacters = "[^\\d.,]".toRegex()

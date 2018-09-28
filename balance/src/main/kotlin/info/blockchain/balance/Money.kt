package info.blockchain.balance

import java.math.BigDecimal
import java.util.Locale

interface Money {

    val isZero: Boolean

    val isPositive: Boolean

    val maxDecimalPlaces: Int

    /**
     * Where a Money type can store more decimal places than is necessary,
     * this property can be used to limit it for user input and display.
     */
    val userDecimalPlaces: Int
        get() = maxDecimalPlaces

    fun toBigDecimal(): BigDecimal

    fun toZero(): Money

    /**
     * User displayable symbol
     */
    fun symbol(locale: Locale = Locale.getDefault()): String

    /**
     * String formatted in the specified locale, or the systems default locale.
     * Includes symbol, which may appear on either side of the number.
     */
    fun toStringWithSymbol(locale: Locale = Locale.getDefault()): String

    /**
     * String formatted in the specified locale, or the systems default locale.
     * Without symbol.
     */
    fun toStringWithoutSymbol(locale: Locale = Locale.getDefault()): String

    /**
     * The formatted string in parts in the specified locale, or the systems default locale.
     */
    fun toStringParts(locale: Locale = Locale.getDefault()) = toStringWithoutSymbol(locale)
        .let {
            val index = it.lastIndexOf(LocaleDecimalFormat[locale].decimalFormatSymbols.decimalSeparator)
            if (index != -1) {
                Parts(
                    symbol = symbol(locale),
                    major = it.substring(0, index),
                    minor = it.substring(index + 1),
                    majorAndMinor = it
                )
            } else {
                Parts(
                    symbol = symbol(locale),
                    major = it,
                    minor = "",
                    majorAndMinor = it
                )
            }
        }

    class Parts(
        val symbol: String,
        val major: String,
        val minor: String,
        val majorAndMinor: String
    )
}
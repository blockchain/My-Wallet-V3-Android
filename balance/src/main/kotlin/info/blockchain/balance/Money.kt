package info.blockchain.balance

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

    fun symbol(locale: Locale = Locale.getDefault()): String

    fun toStringWithSymbol(locale: Locale = Locale.getDefault()): String

    fun toStringWithoutSymbol(locale: Locale = Locale.getDefault()): String

    fun toParts(locale: Locale) = toStringWithoutSymbol(locale)
        .let {
            val index = it.lastIndexOf(LocaleDecimalFormat[locale].decimalFormatSymbols.decimalSeparator)
            if (index != -1) {
                Parts(
                    symbol(locale),
                    it.substring(0, index),
                    it.substring(index + 1)
                )
            } else {
                Parts(
                    symbol(locale),
                    it,
                    ""
                )
            }
        }
}

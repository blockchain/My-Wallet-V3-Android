package info.blockchain.balance

import java.util.Locale

interface Money {

    val isZero: Boolean

    val isPositive: Boolean

    fun symbol(locale: Locale = Locale.getDefault()): String

    fun toStringWithSymbol(locale: Locale = Locale.getDefault()): String

    fun toStringWithoutSymbol(locale: Locale = Locale.getDefault()): String
}

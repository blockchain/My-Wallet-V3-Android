package info.blockchain.balance

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private data class Key(val locale: Locale, val currencyCode: String, val includeSymbol: Boolean)

private object FiatFormat {

    private val cache: MutableMap<Key, NumberFormat> = ConcurrentHashMap()

    operator fun get(key: Key) = cache.getOrPut(key) {
        val currencyInstance = Currency.getInstance(key.currencyCode)
        (NumberFormat.getCurrencyInstance(key.locale) as DecimalFormat)
            .apply {
                decimalFormatSymbols =
                    decimalFormatSymbols.apply {
                        currency = currencyInstance
                        if (!key.includeSymbol) {
                            currencySymbol = ""
                        }
                    }
                minimumFractionDigits = currencyInstance.defaultFractionDigits
                maximumFractionDigits = currencyInstance.defaultFractionDigits
            }
    }
}

data class FiatValue(
    val currencyCode: String,
    val value: BigDecimal
) {
    val isZero: Boolean = value.signum() == 0

    fun toStringWithSymbol(locale: Locale): String =
        FiatFormat[Key(locale, currencyCode, includeSymbol = true)]
            .format(value)

    fun toStringWithoutSymbol(locale: Locale): String =
        FiatFormat[Key(locale, currencyCode, includeSymbol = false)]
            .format(value)
            .trim()

    operator fun plus(other: FiatValue): FiatValue {
        if (currencyCode != other.currencyCode)
            throw MismatchedCurrencyCodeException("Mismatched currency codes during add")
        return FiatValue(currencyCode, value + other.value)
    }
}

class MismatchedCurrencyCodeException(message: String) : Exception(message)

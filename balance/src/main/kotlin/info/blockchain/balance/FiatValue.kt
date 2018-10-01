package info.blockchain.balance

import java.math.BigDecimal
import java.math.RoundingMode
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

// TODO: AND-1363 Remove suppress, possibly by implementing equals manually as copy is not needed
@Suppress("DataClassPrivateConstructor")
data class FiatValue private constructor(
    override val currencyCode: String,
    internal val value: BigDecimal
) : Money {

    override val maxDecimalPlaces: Int get() = maxDecimalPlaces(currencyCode)

    override val isZero: Boolean get() = value.signum() == 0

    override val isPositive: Boolean get() = value.signum() == 1

    override fun toBigDecimal(): BigDecimal = value

    val valueMinor: Long = value.movePointRight(maxDecimalPlaces).toLong()

    override fun toStringWithSymbol(locale: Locale): String =
        FiatFormat[Key(locale, currencyCode, includeSymbol = true)]
            .format(value)

    override fun toStringWithoutSymbol(locale: Locale): String =
        FiatFormat[Key(locale, currencyCode, includeSymbol = false)]
            .format(value)
            .trim()

    operator fun plus(other: FiatValue): FiatValue {
        if (currencyCode != other.currencyCode)
            throw MismatchedCurrencyCodeException("Mismatched currency codes during add")
        return FiatValue(currencyCode, value + other.value)
    }

    override fun symbol(locale: Locale): String = Currency.getInstance(currencyCode).getSymbol(locale)

    override fun toZero(): FiatValue = fromMajor(currencyCode, BigDecimal.ZERO)

    companion object {

        fun fromMinor(currencyCode: String, minor: Long) =
            fromMajor(
                currencyCode,
                BigDecimal.valueOf(minor).movePointLeft(maxDecimalPlaces(currencyCode))
            )

        @JvmStatic
        fun fromMajor(currencyCode: String, major: BigDecimal) =
            FiatValue(
                currencyCode,
                major.setScale(
                    maxDecimalPlaces(currencyCode),
                    RoundingMode.HALF_UP
                )
            )

        private fun maxDecimalPlaces(currencyCode: String) = Currency.getInstance(currencyCode).defaultFractionDigits
    }
}

class MismatchedCurrencyCodeException(message: String) : Exception(message)

private fun ensureComparable(a: String, b: String) {
    if (a != b) throw ComparisonException(a, b)
}

operator fun FiatValue.compareTo(b: FiatValue): Int {
    ensureComparable(currencyCode, b.currencyCode)
    return valueMinor.compareTo(b.valueMinor)
}

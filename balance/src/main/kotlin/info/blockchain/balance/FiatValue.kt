package info.blockchain.balance

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private object LocaleDecimalFormat {

    private val cache: MutableMap<Locale, DecimalFormat> = ConcurrentHashMap()

    operator fun get(locale: Locale) = cache.getOrPut(locale) {
        NumberFormat.getCurrencyInstance(locale) as DecimalFormat
    }
}

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
    val currencyCode: String,
    val value: BigDecimal
) : Money {

    override val isZero: Boolean get() = value.signum() == 0

    override val isPositive: Boolean get() = value.signum() == 1

    val valueMinor: Long =
        value.movePointRight(Currency.getInstance(currencyCode).defaultFractionDigits).toLong()

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

    override fun symbol(locale: Locale): String = Currency.getInstance(currencyCode).getSymbol(locale)

    companion object {

        fun fromMinor(currencyCode: String, minor: Long) =
            fromMajor(
                currencyCode,
                BigDecimal.valueOf(minor).movePointLeft(Currency.getInstance(currencyCode).defaultFractionDigits)
            )

        @JvmStatic
        fun fromMajor(currencyCode: String, major: BigDecimal) =
            FiatValue(
                currencyCode,
                major.setScale(
                    Currency.getInstance(currencyCode).defaultFractionDigits,
                    RoundingMode.HALF_UP
                )
            )
    }
}

class Parts(
    val symbol: String,
    val major: String,
    val minor: String
)

class MismatchedCurrencyCodeException(message: String) : Exception(message)

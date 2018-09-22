package info.blockchain.balance

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal object LocaleDecimalFormat {

    private val cache: MutableMap<Locale, DecimalFormat> = ConcurrentHashMap()

    operator fun get(locale: Locale) = cache.getOrPut(locale) {
        NumberFormat.getCurrencyInstance(locale) as DecimalFormat
    }
}

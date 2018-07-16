package piuk.blockchain.androidcore.data.currency

import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

internal object LocaleCurrencyNumberFormat {
    private val cache: MutableMap<Locale, NumberFormat> = ConcurrentHashMap()

    operator fun get(locale: Locale) = cache.getOrPut(locale) {
        NumberFormat.getCurrencyInstance(locale)
    }
}
package piuk.blockchain.androidcore.data.currency

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.utils.PrefsUtil
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

internal class CurrencyPreference(
    private val prefs: PrefsUtil,
    private val preferenceKey: String,
    private val defaultCurrency: CryptoCurrency
) : ReadWriteProperty<CurrencyState, CryptoCurrency> {

    private var cachedCurrency: CryptoCurrency? = null

    override fun getValue(thisRef: CurrencyState, property: KProperty<*>): CryptoCurrency {
        var localCopy = cachedCurrency
        if (localCopy == null) {
            localCopy = readCryptoCurrency()
            cachedCurrency = localCopy
        }
        return localCopy
    }

    override fun setValue(thisRef: CurrencyState, property: KProperty<*>, value: CryptoCurrency) {
        prefs.setValue(preferenceKey, value.name)
        cachedCurrency = value
    }

    private fun readCryptoCurrency(): CryptoCurrency =
        try {
            CryptoCurrency.valueOf(prefs.getValue(preferenceKey, defaultCurrency.name))
        } catch (e: IllegalArgumentException) {
            prefs.removeValue(preferenceKey)
            defaultCurrency
        }
}

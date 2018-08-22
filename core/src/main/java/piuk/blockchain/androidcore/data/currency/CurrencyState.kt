package piuk.blockchain.androidcore.data.currency

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.utils.PrefsUtil

/**
 * Singleton class to store user's preferred crypto currency state.
 * (ie is Wallet currently showing FIAT, ETH, BTC ot BCH)
 */
class CurrencyState(private val prefs: PrefsUtil) {

    enum class DisplayMode {
        Crypto,
        Fiat;

        fun toggle() =
            when (this) {
                Crypto -> Fiat
                Fiat -> Crypto
            }
    }

    var displayMode = DisplayMode.Crypto

    @Deprecated("Use displayMode")
    var isDisplayingCryptoCurrency
        get() = displayMode == DisplayMode.Crypto
        set(value) {
            displayMode = if (value) DisplayMode.Crypto else DisplayMode.Fiat
        }

    val fiatUnit: String
        get() = prefs.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    var cryptoCurrency: CryptoCurrency by CurrencyPreference(
        prefs,
        PrefsUtil.KEY_CURRENCY_CRYPTO_STATE,
        defaultCurrency = CryptoCurrency.BTC
    )
}

package piuk.blockchain.androidcore.data.currency

import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.utils.PrefsUtil

/**
 * Singleton class to store user's preferred crypto currency state.
 * (ie is Wallet currently showing FIAT, ETH, BTC ot BCH)
 */
class CurrencyState(private val prefs: PrefsUtil) {

    var isDisplayingCryptoCurrency = true

    val fiatUnit: String
        get() = prefs.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    var cryptoCurrency: CryptoCurrency by CurrencyPreference(
        prefs,
        PrefsUtil.KEY_CURRENCY_CRYPTO_STATE,
        defaultCurrency = CryptoCurrency.BTC
    )
}

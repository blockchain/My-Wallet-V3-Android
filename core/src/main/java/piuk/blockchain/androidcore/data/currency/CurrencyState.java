package piuk.blockchain.androidcore.data.currency;

import java.util.Currency;
import java.util.Locale;

import piuk.blockchain.androidcore.utils.PrefsUtil;
import piuk.blockchain.androidcore.utils.annotations.Mockable;

/**
 * Singleton class to store user's preferred crypto currency state.
 * (ie is Wallet currently showing FIAT, ETH, BTC ot BCH)
 */
@Mockable
public class CurrencyState {

    private static CurrencyState INSTANCE;

    private PrefsUtil prefs;
    private CryptoCurrency cryptoCurrency;
    private boolean isDisplayingCryptoCurrency;

    private CurrencyState() {
        isDisplayingCryptoCurrency = false;
    }

    public static CurrencyState getInstance() {
        if (INSTANCE == null)
            INSTANCE = new CurrencyState();
        return INSTANCE;
    }

    public void init(PrefsUtil prefs) {
        this.prefs = prefs;
        String value = prefs.getValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, CryptoCurrency.BTC.name());
        try {
            cryptoCurrency = CryptoCurrency.valueOf(value);
        } catch (IllegalArgumentException e) {
            // It's possible that the wrong string is stored here - clear stored value
            prefs.removeValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE);
            setCryptoCurrency(CryptoCurrency.BTC);
        }
        isDisplayingCryptoCurrency = true;
    }

    public CryptoCurrency getCryptoCurrency() {
        return cryptoCurrency;
    }

    public void setCryptoCurrency(CryptoCurrency cryptoCurrency) {
        prefs.setValue(PrefsUtil.KEY_CURRENCY_CRYPTO_STATE, cryptoCurrency.name());
        this.cryptoCurrency = cryptoCurrency;
    }

    public void toggleCryptoCurrency() {
        if (cryptoCurrency == CryptoCurrency.BTC) {
            cryptoCurrency = CryptoCurrency.ETHER;
        } else {
            cryptoCurrency = CryptoCurrency.BTC;
        }

        setCryptoCurrency(cryptoCurrency);

    }

    public boolean isDisplayingCryptoCurrency() {
        return isDisplayingCryptoCurrency;
    }

    public void setDisplayingCryptoCurrency(boolean displayingCryptoCurrency) {
        isDisplayingCryptoCurrency = displayingCryptoCurrency;
    }

    public String getFiatUnit() {
        return prefs.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
    }

    /**
     * Returns the symbol for the chosen currency, based on the passed currency code and the chosen
     * device [Locale].
     *
     * @param currencyCode The 3-letter currency code, eg. "GBP"
     * @param locale The current device [Locale]
     * @return The correct currency symbol (eg. "$")
     */
    public String getCurrencySymbol(String currencyCode, Locale locale) {
        return Currency.getInstance(currencyCode).getSymbol(locale);
    }
}

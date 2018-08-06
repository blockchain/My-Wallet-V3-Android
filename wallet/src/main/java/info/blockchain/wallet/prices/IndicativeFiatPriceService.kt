package info.blockchain.wallet.prices

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import io.reactivex.Observable

/**
 * Access to streams of indicative rates.
 * These are suitable for on the fly converting for information purposes.
 */
interface IndicativeFiatPriceService {

    /**
     * A stream of indicative rates from [CryptoCurrency] to Fiat.
     * These are suitable for converting Crypto to Fiat for display purposes.
     */
    fun indicativeRateStream(from: CryptoCurrency, toFiat: String): Observable<ExchangeRate.CryptoToFiat>

    /**
     * A stream of indicative rates from Fiat to [CryptoCurrency].
     * These are suitable for converting Fiat to Crypto for display purposes.
     */
    fun indicativeRateStream(fromFiat: String, to: CryptoCurrency): Observable<ExchangeRate.FiatToCrypto> =
        indicativeRateStream(to, fromFiat).map { it.inverse() }
}

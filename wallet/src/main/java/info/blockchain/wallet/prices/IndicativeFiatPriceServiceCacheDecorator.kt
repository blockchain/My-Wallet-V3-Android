package info.blockchain.wallet.prices

import com.jakewharton.rx.replayingShare
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import io.reactivex.Observable
import java.util.concurrent.ConcurrentHashMap

/**
 * Yields a caching indicative price service
 */
fun CurrentPriceApi.toCachedIndicativeFiatPriceService(): IndicativeFiatPriceService =
    toIndicativeFiatPriceService().cache()

/**
 * Cache requests to save hits on the api
 */
private fun IndicativeFiatPriceService.cache(): IndicativeFiatPriceService =
    IndicativeFiatPriceServiceCacheDecorator(this)

/**
 * Caches requests
 */
private class IndicativeFiatPriceServiceCacheDecorator(private val inner: IndicativeFiatPriceService) :
    IndicativeFiatPriceService {

    private val c2fMap: MutableMap<Pair<CryptoCurrency, String>, Observable<ExchangeRate.CryptoToFiat>> =
        ConcurrentHashMap()

    override fun indicativeRateStream(from: CryptoCurrency, toFiat: String) =
        c2fMap.getOrPut(from to toFiat) {
            inner.indicativeRateStream(from, toFiat)
                .replayingShare()
        }
}

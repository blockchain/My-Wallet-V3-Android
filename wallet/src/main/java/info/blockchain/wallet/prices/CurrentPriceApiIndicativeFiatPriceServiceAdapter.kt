package info.blockchain.wallet.prices

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import io.reactivex.Observable
import io.reactivex.Single
import java.util.concurrent.TimeUnit

internal fun CurrentPriceApi.toIndicativeFiatPriceService(): IndicativeFiatPriceService {
    return CurrentPriceApiIndicativeFiatPriceServiceAdapter(this)
}

private class CurrentPriceApiIndicativeFiatPriceServiceAdapter(
    private val currentPriceApi: CurrentPriceApi
) : IndicativeFiatPriceService {

    override fun indicativeRateStream(from: CryptoCurrency, toFiat: String): Observable<ExchangeRate.CryptoToFiat> =
        repeat {
            currentPriceApi.currentPrice(from, toFiat)
        }.map {
            ExchangeRate.CryptoToFiat(
                from,
                toFiat,
                it
            )
        }
}

private fun <T> repeat(function: () -> Single<T>): Observable<T> =
    Observable.defer { function().toObservable() }
        .repeatWhen { o -> o.concatMap { _ -> Observable.timer(1, TimeUnit.SECONDS) } }

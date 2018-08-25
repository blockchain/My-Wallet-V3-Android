package com.blockchain.morph.dev

import com.blockchain.morph.exchange.mvi.ExchangeIntent
import com.blockchain.morph.exchange.mvi.toIntent
import com.blockchain.morph.ui.homebrew.exchange.RateStream
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.wallet.prices.IndicativeFiatPriceService
import io.reactivex.Observable
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Gives real fiat prices, but fake, oscillating, crypto to crypto prices. Updates every 5 seconds
 */
internal class FakeRatesStream(
    private val priceService: IndicativeFiatPriceService
) : RateStream {

    override fun rateStream(
        from: CryptoCurrency,
        to: CryptoCurrency,
        fiat: String
    ): Observable<ExchangeIntent> = fakeRates(from, to, fiat)

    private fun fakeRates(
        from: CryptoCurrency,
        to: CryptoCurrency,
        fiat: String
    ): Observable<ExchangeIntent> {
        val fromToCrypto = Observable.interval(5000, TimeUnit.MILLISECONDS)
            .map {
                ExchangeRate.CryptoToCrypto(
                    from = from, to = to,
                    rate = (Math.sin(2 * Math.PI * it.toDouble() / 3.9) / 20.0 + 1.0 + 17).toBigDecimal()
                ).toIntent()
            }

        val toFromCrypto = Observable.interval(5000, TimeUnit.MILLISECONDS)
            .map {
                ExchangeRate.CryptoToCrypto(
                    from = to, to = from,
                    rate = (1.0 / ((Math.sin(2 * Math.PI * it.toDouble() / 3.9) / 20.0) + 1.0 + 17)).toBigDecimal()
                ).toIntent()
            }

        val fromCryptoToFiat = priceService.indicativeRateStream(from, fiat)
            .doOnNext {
                Timber.d("Price ${it.from}->${it.to} = ${it.rate}")
            }
            .map { it.toIntent() }

        val toCryptoToFiat = priceService.indicativeRateStream(to, fiat)
            .doOnNext {
                Timber.d("Price ${it.from}->${it.to} = ${it.rate}")
            }
            .map { it.toIntent() }

        return Observable.merge(fromToCrypto, toFromCrypto, fromCryptoToFiat, toCryptoToFiat)
    }
}

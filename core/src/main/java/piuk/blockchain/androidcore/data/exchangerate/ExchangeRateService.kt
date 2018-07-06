package piuk.blockchain.androidcore.data.exchangerate

import info.blockchain.wallet.prices.PriceApi
import io.reactivex.Observable
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies
import piuk.blockchain.androidcore.utils.annotations.WebRequest
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import javax.inject.Inject

class ExchangeRateService @Inject constructor(private val priceApi: PriceApi) {

    @WebRequest
    fun getBtcExchangeRateObservable() =
        priceApi.getPriceIndexes(CryptoCurrencies.BTC.symbol)
            .applySchedulers()

    @WebRequest
    fun getEthExchangeRateObservable() =
        priceApi.getPriceIndexes(CryptoCurrencies.ETHER.symbol)
            .applySchedulers()

    @WebRequest
    fun getBchExchangeRateObservable() =
        priceApi.getPriceIndexes(CryptoCurrencies.BCH.symbol)
            .applySchedulers()

    @WebRequest
    fun getBtcHistoricPrice(
        currency: String,
        timeInSeconds: Long
    ): Observable<Double> =
        priceApi.getHistoricPrice(CryptoCurrencies.BTC.symbol, currency, timeInSeconds)
            .applySchedulers()

    @WebRequest
    fun getEthHistoricPrice(
        currency: String,
        timeInSeconds: Long
    ): Observable<Double> =
        priceApi.getHistoricPrice(CryptoCurrencies.ETHER.symbol, currency, timeInSeconds)
            .applySchedulers()

    @WebRequest
    fun getBchHistoricPrice(
        currency: String,
        timeInSeconds: Long
    ): Observable<Double> =
        priceApi.getHistoricPrice(CryptoCurrencies.BCH.symbol, currency, timeInSeconds)
            .applySchedulers()
}
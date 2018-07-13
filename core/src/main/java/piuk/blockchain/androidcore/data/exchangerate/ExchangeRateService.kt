package piuk.blockchain.androidcore.data.exchangerate

import info.blockchain.wallet.prices.PriceApi
import io.reactivex.Observable
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.utils.annotations.WebRequest
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import javax.inject.Inject

class ExchangeRateService @Inject constructor(private val priceApi: PriceApi) {

    @WebRequest
    fun getBtcExchangeRateObservable() =
        priceApi.getPriceIndexes(CryptoCurrency.BTC.symbol)
            .applySchedulers()

    @WebRequest
    fun getEthExchangeRateObservable() =
        priceApi.getPriceIndexes(CryptoCurrency.ETHER.symbol)
            .applySchedulers()

    @WebRequest
    fun getBchExchangeRateObservable() =
        priceApi.getPriceIndexes(CryptoCurrency.BCH.symbol)
            .applySchedulers()

    @WebRequest
    fun getBtcHistoricPrice(
        currency: String,
        timeInSeconds: Long
    ): Observable<Double> =
        priceApi.getHistoricPrice(CryptoCurrency.BTC.symbol, currency, timeInSeconds)
            .applySchedulers()

    @WebRequest
    fun getEthHistoricPrice(
        currency: String,
        timeInSeconds: Long
    ): Observable<Double> =
        priceApi.getHistoricPrice(CryptoCurrency.ETHER.symbol, currency, timeInSeconds)
            .applySchedulers()

    @WebRequest
    fun getBchHistoricPrice(
        currency: String,
        timeInSeconds: Long
    ): Observable<Double> =
        priceApi.getHistoricPrice(CryptoCurrency.BCH.symbol, currency, timeInSeconds)
            .applySchedulers()
}
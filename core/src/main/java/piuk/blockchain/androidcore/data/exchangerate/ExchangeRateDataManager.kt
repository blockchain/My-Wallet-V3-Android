package piuk.blockchain.androidcore.data.exchangerate

import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.rxjava.RxPinning
import piuk.blockchain.androidcore.injection.PresenterScope
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import javax.inject.Inject

/**
 * This data manager is responsible for storing and updating the latest exchange rates information
 * for all crypto currencies.
 * Historic prices for all crypto currencies can be queried from here.
 */
@PresenterScope
class ExchangeRateDataManager @Inject constructor(
    private val exchangeRateDataStore: ExchangeRateDataStore,
    rxBus: RxBus
) {

    private val rxPinning = RxPinning(rxBus)

    fun updateTickers() =
        rxPinning.call { exchangeRateDataStore.updateExchangeRates() }
            .applySchedulers()

    fun getLastPrice(cryptoCurrency: CryptoCurrency, currencyName: String) =
        exchangeRateDataStore.getLastPrice(cryptoCurrency, currencyName)

    @Deprecated(
        "Use getLastPrice",
        replaceWith = ReplaceWith("getLastPrice(CryptoCurrency.BTC, currencyName)")
    )
    fun getLastBtcPrice(currencyName: String) =
        exchangeRateDataStore.getLastBtcPrice(currencyName)

    @Deprecated(
        "Use getLastPrice",
        replaceWith = ReplaceWith("getLastPrice(CryptoCurrency.BCH, currencyName)")
    )
    fun getLastBchPrice(currencyName: String) =
        exchangeRateDataStore.getLastBchPrice(currencyName)

    @Deprecated(
        "Use getLastPrice",
        replaceWith = ReplaceWith("getLastPrice(CryptoCurrency.ETHER, currencyName)")
    )
    fun getLastEthPrice(currencyName: String) =
        exchangeRateDataStore.getLastEthPrice(currencyName)

    fun getCurrencyLabels() = exchangeRateDataStore.getCurrencyLabels()

    /**
     * Returns the historic value of a number of Satoshi at a given time in a given currency.
     *
     * @param satoshis The amount of Satoshi to be converted
     * @param currency The currency to be converted to as a 3 letter acronym, eg USD, GBP
     * @param timeInSeconds The time at which to get the price, in seconds since epoch
     * @return A double value, which <b>is not</b> rounded to any significant figures
     */
    fun getBtcHistoricPrice(
        satoshis: Long,
        currency: String,
        timeInSeconds: Long
    ): Observable<BigDecimal> = rxPinning.call<BigDecimal> {
        exchangeRateDataStore.getBtcHistoricPrice(currency, timeInSeconds)
            .map {
                val exchangeRate = BigDecimal.valueOf(it)
                val satoshiDecimal = BigDecimal.valueOf(satoshis)
                return@map exchangeRate.multiply(
                    satoshiDecimal.divide(
                        SATOSHIS_PER_BITCOIN,
                        8,
                        RoundingMode.HALF_UP
                    )
                )
            }
    }

    /**
     * Returns the historic value of a number of Satoshi at a given time in a given currency.
     *
     * @param satoshis The amount of Satoshi to be converted
     * @param currency The currency to be converted to as a 3 letter acronym, eg USD, GBP
     * @param timeInSeconds The time at which to get the price, in seconds since epoch
     * @return A double value, which <b>is not</b> rounded to any significant figures
     */
    fun getBchHistoricPrice(
        satoshis: Long,
        currency: String,
        timeInSeconds: Long
    ): Observable<BigDecimal> = rxPinning.call<BigDecimal> {
        exchangeRateDataStore.getBchHistoricPrice(currency, timeInSeconds)
            .map {
                val exchangeRate = BigDecimal.valueOf(it)
                val satoshiDecimal = BigDecimal.valueOf(satoshis)
                return@map exchangeRate.multiply(
                    satoshiDecimal.divide(
                        SATOSHIS_PER_BITCOIN,
                        8,
                        RoundingMode.HALF_UP
                    )
                )
            }
    }

    /**
     * Returns the historic value of a number of Wei at a given time in a given currency.
     *
     * @param wei The amount of Ether to be converted in Wei, ie ETH * 1e18
     * @param currency The currency to be converted to as a 3 letter acronym, eg USD, GBP
     * @param timeInSeconds The time at which to get the price, in seconds since epoch
     * @return A double value, which <b>is not</b> rounded to any significant figures
     */
    fun getEthHistoricPrice(
        wei: BigInteger,
        currency: String,
        timeInSeconds: Long
    ): Observable<BigDecimal> = rxPinning.call<BigDecimal> {
        exchangeRateDataStore.getEthHistoricPrice(currency, timeInSeconds)
            .map {
                val exchangeRate = BigDecimal.valueOf(it)
                val ethDecimal = BigDecimal(wei)
                return@map exchangeRate.multiply(
                    ethDecimal.divide(
                        WEI_PER_ETHER,
                        8,
                        RoundingMode.HALF_UP
                    )
                )
            }
    }

    fun getBtcFromFiat(fiatAmount: BigDecimal, fiatUnit: String): BigDecimal {
        return fiatAmount.divide(getLastBtcPrice(fiatUnit).toBigDecimal(), 8, RoundingMode.HALF_UP)
    }

    fun getBchFromFiat(fiatAmount: BigDecimal, fiatUnit: String): BigDecimal {
        return fiatAmount.divide(getLastBchPrice(fiatUnit).toBigDecimal(), 8, RoundingMode.HALF_UP)
    }

    fun getEthFromFiat(fiatAmount: BigDecimal, fiatUnit: String): BigDecimal {
        return fiatAmount.divide(getLastEthPrice(fiatUnit).toBigDecimal(), 8, RoundingMode.HALF_UP)
    }

    fun getFiatFromBtc(btc: BigDecimal, fiatUnit: String): BigDecimal {
        return getLastBtcPrice(fiatUnit).toBigDecimal().multiply(btc)
    }

    fun getFiatFromBch(bch: BigDecimal, fiatUnit: String): BigDecimal {
        return getLastBchPrice(fiatUnit).toBigDecimal().multiply(bch)
    }

    fun getFiatFromEth(eth: BigDecimal, fiatUnit: String): BigDecimal {
        return getLastEthPrice(fiatUnit).toBigDecimal().multiply(eth)
    }

    companion object {
        internal val SATOSHIS_PER_BITCOIN = BigDecimal.valueOf(100_000_000L)
        internal val WEI_PER_ETHER = BigDecimal.valueOf(1e18)
    }
}
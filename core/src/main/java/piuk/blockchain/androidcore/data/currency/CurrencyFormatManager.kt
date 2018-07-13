package piuk.blockchain.androidcore.data.currency

import android.support.annotation.VisibleForTesting
import info.blockchain.balance.CryptoCurrency
import org.web3j.utils.Convert
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.injection.PresenterScope
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.annotations.Mockable
import piuk.blockchain.androidcore.utils.helperfunctions.InvalidatableLazy
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.text.ParseException
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

@Mockable
@PresenterScope
class CurrencyFormatManager @Inject constructor(
    private val currencyState: CurrencyState,
    private val exchangeRateDataManager: ExchangeRateDataManager,
    private val prefsUtil: PrefsUtil,
    private val currencyFormatUtil: CurrencyFormatUtil,
    private val locale: Locale
) {

    private val invalidatable = InvalidatableLazy {
        prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)
    }

    /**
     * Returns the currency's country code
     *
     * @return The currency abbreviation (USD, GBP etc)
     * @see ExchangeRateDataManager.getCurrencyLabels
     */
    val fiatCountryCode: String by invalidatable

    /**
     * Notifies the class that the fiat code has been reset. This allows [fiatCountryCode] to be
     * lazily loaded again in case of update.
     */
    fun invalidateFiatCode() = invalidatable.invalidate()

    // region Selected Coin methods based on CurrencyState.currencyState
    /**
     * Returns the maximum decimals allowed for current crypto currency state. Useful to apply max decimal length
     * on text fields.
     *
     * @return decimal length.
     */
    fun getSelectedCoinMaxFractionDigits() =
        when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> currencyFormatUtil.getBtcMaxFractionDigits()
            CryptoCurrency.ETHER -> currencyFormatUtil.getEthMaxFractionDigits()
            CryptoCurrency.BCH -> currencyFormatUtil.getBchMaxFractionDigits()
            else -> throw IllegalArgumentException(currencyState.cryptoCurrency.toString() + " not supported.")
        }

    /**
     * Crypto unit based on current crypto currency state.
     *
     * @return BTC, BCH or ETH.
     */
    fun getSelectedCoinUnit() =
        when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> currencyFormatUtil.getBtcUnit()
            CryptoCurrency.ETHER -> currencyFormatUtil.getEthUnit()
            CryptoCurrency.BCH -> currencyFormatUtil.getBchUnit()
            else -> throw IllegalArgumentException(currencyState.cryptoCurrency.toString() + " not supported.")
        }

    @VisibleForTesting
    fun getConvertedCoinValue(
        coinValue: BigDecimal,
        convertEthDenomination: ETHDenomination? = null,
        convertBtcDenomination: BTCDenomination? = BTCDenomination.SATOSHI
    ): BigDecimal {
        return if (convertEthDenomination != null) {
            when (convertEthDenomination) {
                ETHDenomination.ETH -> coinValue
                else -> coinValue.divide(ETH_DEC.toBigDecimal(), 18, RoundingMode.HALF_UP)
            }
        } else {
            when (convertBtcDenomination) {
                BTCDenomination.BTC -> coinValue
                else -> coinValue.divide(BTC_DEC.toBigDecimal(), 8, RoundingMode.HALF_UP)
            }
        }
    }

    /**
     * Accepts a [BigDecimal] value in Satoshis/Wei and returns the display amount as a [String]
     * based on the chosen denomination type.
     *
     * eg. 10_000 Satoshi -> "0.0001" when unit == UNIT_BTC
     * eg. 1_000_000_000_000_000_000 Wei -> "1.0" when unit == UNIT_ETH
     *
     * TODO: Note: a default denomination of SATOSHI was set since this method is used in so many places.
     * Not technically correct and should be improved.
     *
     * @param value The amount to be formatted in Satoshis
     * @return An amount formatted as a [String]
     */
    fun getFormattedSelectedCoinValue(
        coinValue: BigDecimal,
        convertEthDenomination: ETHDenomination? = null,
        convertBtcDenomination: BTCDenomination? = BTCDenomination.SATOSHI
    ): String {
        val convertedCoinValue =
            getConvertedCoinValue(coinValue, convertEthDenomination, convertBtcDenomination)

        return when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> currencyFormatUtil.formatBtc(convertedCoinValue)
            CryptoCurrency.ETHER -> currencyFormatUtil.formatEth(convertedCoinValue)
            CryptoCurrency.BCH -> currencyFormatUtil.formatBch(convertedCoinValue)
            else -> throw IllegalArgumentException(currencyState.cryptoCurrency.toString() + " not supported.")
        }
    }

    fun getFormattedSelectedCoinValueWithUnit(
        coinValue: BigDecimal,
        convertEthDenomination: ETHDenomination? = null,
        convertBtcDenomination: BTCDenomination? = BTCDenomination.SATOSHI
    ): String {
        val convertedCoinValue =
            getConvertedCoinValue(coinValue, convertEthDenomination, convertBtcDenomination)

        return when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> currencyFormatUtil.formatBtcWithUnit(convertedCoinValue)
            CryptoCurrency.ETHER -> currencyFormatUtil.formatEthWithUnit(convertedCoinValue)
            CryptoCurrency.BCH -> currencyFormatUtil.formatBchWithUnit(convertedCoinValue)
            else -> throw IllegalArgumentException(currencyState.cryptoCurrency.toString() + " not supported.")
        }
    }

    /**
     * @return Formatted String of crypto amount from fiat currency amount.
     */
    fun getFormattedSelectedCoinValueFromFiatString(fiatText: String): String {
        val fiatAmount = fiatText.toSafeDouble(locale).toBigDecimal()

        return when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> currencyFormatUtil.formatBtc(
                exchangeRateDataManager.getBtcFromFiat(fiatAmount, fiatCountryCode)
            )
            CryptoCurrency.ETHER -> currencyFormatUtil.formatEth(
                exchangeRateDataManager.getEthFromFiat(fiatAmount, fiatCountryCode)
            )
            CryptoCurrency.BCH -> currencyFormatUtil.formatBch(
                exchangeRateDataManager.getBchFromFiat(fiatAmount, fiatCountryCode)
            )
            else -> throw IllegalArgumentException(currencyState.cryptoCurrency.toString() + " not supported.")
        }
    }
    // endregion

    // region Fiat methods
    /**
     * Returns the symbol for the current chosen currency, based on the passed currency code and the chosen
     * device [Locale].
     *
     * @return The correct currency symbol (eg. "$")
     */
    fun getFiatSymbol(): String =
        Currency.getInstance(fiatCountryCode).getSymbol(locale)

    /**
     * Returns the symbol for the chosen currency, based on the passed currency code and the chosen
     * device [Locale].
     *
     * @param currencyCode The 3-letter currency code, eg. "GBP"
     * @param locale The current device [Locale]
     * @return The correct currency symbol (eg. "$")
     */
    fun getFiatSymbol(currencyCode: String, locale: Locale): String =
        currencyFormatUtil.getFiatSymbol(currencyCode, locale)

    // TODO This should be private but there are a few places that still use this
    fun getFiatFormat(currencyCode: String) = currencyFormatUtil.getFiatFormat(currencyCode)

    private fun getFiatValueFromSelectedCoin(
        coinValue: BigDecimal,
        convertEthDenomination: ETHDenomination? = null,
        convertBtcDenomination: BTCDenomination? = null
    ): BigDecimal {
        return if (convertEthDenomination != null) {
            when (currencyState.cryptoCurrency) {
                CryptoCurrency.ETHER -> getFiatValueFromEth(coinValue, convertEthDenomination)
                else -> throw IllegalArgumentException("${currencyState.cryptoCurrency} denomination not supported.")
            }
        } else {
            when (currencyState.cryptoCurrency) {
                CryptoCurrency.BTC -> getFiatValueFromBtc(coinValue, convertBtcDenomination)
                CryptoCurrency.BCH -> getFiatValueFromBch(coinValue, convertBtcDenomination)
                else -> throw IllegalArgumentException("${currencyState.cryptoCurrency} denomination not supported.")
            }
        }
    }

    private fun getFiatValueFromBtc(
        coinValue: BigDecimal,
        convertBtcDenomination: BTCDenomination? = BTCDenomination.SATOSHI
    ): BigDecimal {
        val fiatUnit = fiatCountryCode

        val sanitizedDenomination = when (convertBtcDenomination) {
            BTCDenomination.BTC -> coinValue
            else -> coinValue.divide(BTC_DEC.toBigDecimal(), 8, RoundingMode.HALF_UP)
        }
        return exchangeRateDataManager.getFiatFromBtc(sanitizedDenomination, fiatUnit)
    }

    private fun getFiatValueFromBch(
        coinValue: BigDecimal,
        convertBtcDenomination: BTCDenomination? = BTCDenomination.SATOSHI
    ): BigDecimal {
        val fiatUnit = fiatCountryCode

        val sanitizedDenomination = when (convertBtcDenomination) {
            BTCDenomination.BTC -> coinValue
            else -> coinValue.divide(BTC_DEC.toBigDecimal(), 8, RoundingMode.HALF_UP)
        }
        return exchangeRateDataManager.getFiatFromBch(sanitizedDenomination, fiatUnit)
    }

    private fun getFiatValueFromEth(
        coinValue: BigDecimal,
        convertEthDenomination: ETHDenomination?
    ): BigDecimal {
        val fiatUnit = fiatCountryCode

        val sanitizedDenomination = when (convertEthDenomination) {
            ETHDenomination.ETH -> coinValue
            else -> coinValue.divide(ETH_DEC.toBigDecimal(), 18, RoundingMode.HALF_UP)
        }
        return exchangeRateDataManager.getFiatFromEth(sanitizedDenomination, fiatUnit)
    }

    fun getFormattedFiatValueFromSelectedCoinValue(
        coinValue: BigDecimal,
        convertEthDenomination: ETHDenomination? = null,
        convertBtcDenomination: BTCDenomination? = null
    ): String {
        val fiatUnit = fiatCountryCode
        val fiatBalance = getFiatValueFromSelectedCoin(
            coinValue,
            convertEthDenomination,
            convertBtcDenomination
        )
        return currencyFormatUtil.formatFiat(fiatBalance, fiatUnit)
    }

    fun getFormattedFiatValueFromSelectedCoinValueWithSymbol(
        coinValue: BigDecimal,
        convertEthDenomination: ETHDenomination? = null,
        convertBtcDenomination: BTCDenomination? = null
    ): String {
        val fiatUnit = fiatCountryCode
        val fiatBalance = getFiatValueFromSelectedCoin(
            coinValue,
            convertEthDenomination,
            convertBtcDenomination
        )
        return currencyFormatUtil.formatFiatWithSymbol(fiatBalance.toDouble(), fiatUnit, locale)
    }

    fun getFormattedFiatValueFromBchValueWithSymbol(
        coinValue: BigDecimal,
        convertBtcDenomination: BTCDenomination? = null
    ): String {
        val fiatUnit = fiatCountryCode
        val fiatBalance = getFiatValueFromBch(coinValue, convertBtcDenomination)
        return currencyFormatUtil.formatFiatWithSymbol(fiatBalance.toDouble(), fiatUnit, locale)
    }

    fun getFormattedFiatValueFromBtcValueWithSymbol(
        coinValue: BigDecimal,
        convertBtcDenomination: BTCDenomination? = null
    ): String {
        val fiatUnit = fiatCountryCode
        val fiatBalance = getFiatValueFromBtc(coinValue, convertBtcDenomination)
        return currencyFormatUtil.formatFiatWithSymbol(fiatBalance.toDouble(), fiatUnit, locale)
    }

    fun getFormattedFiatValueFromEthValueWithSymbol(
        coinValue: BigDecimal,
        convertEthDenomination: ETHDenomination? = null
    ): String {
        val fiatUnit = fiatCountryCode
        val fiatBalance = getFiatValueFromEth(coinValue, convertEthDenomination)
        return currencyFormatUtil.formatFiatWithSymbol(fiatBalance.toDouble(), fiatUnit, locale)
    }

    /**
     * Returns a formatted fiat string based on the input text and last known exchange rate.
     * If the input text can't be cast to a double this will return 0.0
     *
     * @return Formatted String of fiat amount from coin amount.
     */
    fun getFormattedFiatValueFromCoinValueInputText(
        coinInputText: String,
        convertEthDenomination: ETHDenomination? = null,
        convertBtcDenomination: BTCDenomination? = null
    ): String {
        val cryptoAmount = coinInputText.toSafeDouble(locale).toBigDecimal()
        return getFormattedFiatValueFromSelectedCoinValue(
            cryptoAmount,
            convertEthDenomination,
            convertBtcDenomination
        )
    }

    /**
     * Accepts a [Double] value in fiat currency and returns a [String] formatted to the region
     * with the correct currency symbol. For example, 1.2345 with country code "USD" and locale
     * [Locale.UK] would return "US$1.23".
     *
     * @param amount The amount of fiat currency to be formatted as a [Double]
     * @return The formatted currency [String]
     */
    fun getFormattedFiatValueWithSymbol(amount: Double): String =
        currencyFormatUtil.formatFiatWithSymbol(amount, fiatCountryCode, locale)

    /**
     * Accepts a [Double] value in fiat currency and returns a [String] formatted to the region
     * with the correct currency symbol. For example, 1.2345 with country code "USD" and locale
     * [Locale.UK] would return "US$1.23".
     *
     * @param amount The amount of fiat currency to be formatted as a [Double]
     * @param currencyCode The 3-letter currency code, eg. "GBP"
     * @param locale The current device [Locale]
     * @return The formatted currency [String]
     */
    fun getFormattedFiatValueWithSymbol(
        amount: Double,
        currencyCode: String,
        locale: Locale
    ): String = currencyFormatUtil.formatFiatWithSymbol(amount, currencyCode, locale)
    // endregion

    // region Coin specific methods
    /**
     * Returns formatted string of supplied coin value.
     * (ie 1,000.00 BTC, 0.0001 BTC)
     *
     * @param coinValue Value of the coin
     * @param coinDenomination Denomination of the coinValue supplied
     * @return BTC decimal formatted amount with appended BTC unit
     */
    fun getFormattedBtcValueWithUnit(
        coinValue: BigDecimal,
        coinDenomination: BTCDenomination
    ): String {
        val value = when (coinDenomination) {
            BTCDenomination.BTC -> coinValue
            else -> coinValue.divide(BTC_DEC.toBigDecimal(), 8, RoundingMode.HALF_UP)
        }

        return currencyFormatUtil.formatBtcWithUnit(value)
    }

    /**
     * Returns formatted string of supplied coin value.
     * (ie 1,000.00, 0.0001)
     *
     * @param coinValue Value of the coin
     * @param coinDenomination Denomination of the coinValue supplied
     * @return BTC decimal formatted amount
     */
    fun getFormattedBtcValue(coinValue: BigDecimal, coinDenomination: BTCDenomination): String {
        val value = when (coinDenomination) {
            BTCDenomination.BTC -> coinValue
            else -> coinValue.divide(BTC_DEC.toBigDecimal(), 8, RoundingMode.HALF_UP)
        }

        return currencyFormatUtil.formatBtc(value)
    }

    /**
     * Returns formatted string of supplied coin value.
     * (ie 1,000.00 BCH, 0.0001 BCH)
     *
     * @param coinValue Value of the coin
     * @param coinDenomination Denomination of the coinValue supplied
     * @return BTC decimal formatted amount with appended BTC unit
     */
    fun getFormattedBchValueWithUnit(
        coinValue: BigDecimal,
        coinDenomination: BTCDenomination
    ): String {
        val value = when (coinDenomination) {
            BTCDenomination.BTC -> coinValue
            else -> coinValue.divide(BTC_DEC.toBigDecimal(), 8, RoundingMode.HALF_UP)
        }

        return currencyFormatUtil.formatBchWithUnit(value)
    }

    /**
     * Returns formatted string of supplied coin value.
     * (ie 1,000.00, 0.0001)
     *
     * @param coinValue Value of the coin
     * @param coinDenomination Denomination of the coinValue supplied
     * @return BCH decimal formatted amount
     */
    fun getFormattedBchValue(coinValue: BigDecimal, coinDenomination: BTCDenomination): String {
        val value = when (coinDenomination) {
            BTCDenomination.BTC -> coinValue
            else -> coinValue.divide(BTC_DEC.toBigDecimal(), 8, RoundingMode.HALF_UP)
        }

        return currencyFormatUtil.formatBch(value)
    }

    /**
     * Returns formatted string of supplied coin value.
     * (ie 1,000.00 ETH, 0.0001 ETH)
     *
     * @param coinValue Value of the coin
     * @param coinDenomination Denomination of the coinValue supplied
     * @return ETH decimal formatted amount with appended ETH unit
     */
    fun getFormattedEthValueWithUnit(
        coinValue: BigDecimal,
        coinDenomination: ETHDenomination
    ): String {
        val value = when (coinDenomination) {
            ETHDenomination.ETH -> coinValue
            else -> coinValue.divide(ETH_DEC.toBigDecimal(), 18, RoundingMode.HALF_UP)
        }

        return currencyFormatUtil.formatEthWithUnit(value)
    }

    /**
     * Returns formatted string of supplied coin value.
     * (ie 1,000.00 ETH, 0.0001 ETH)
     *
     * @param coinValue Value of the coin
     * @param coinDenomination Denomination of the coinValue supplied
     * @return ETH decimal formatted amount with appended ETH unit
     */
    fun getFormattedEthShortValueWithUnit(
        coinValue: BigDecimal,
        coinDenomination: ETHDenomination
    ): String {
        val value = when (coinDenomination) {
            ETHDenomination.ETH -> coinValue
            else -> coinValue.divide(ETH_DEC.toBigDecimal(), 18, RoundingMode.HALF_UP)
        }

        return currencyFormatUtil.formatEthShortWithUnit(value)
    }

    /**
     * Returns formatted string of supplied coin value.
     * (ie 1,000.00, 0.0001)
     *
     * @param coinValue Value of the coin
     * @param coinDenomination Denomination of the coinValue supplied
     * @return ETH decimal formatted amount
     */
    fun getFormattedEthValue(coinValue: BigDecimal, coinDenomination: ETHDenomination): String {
        val value = when (coinDenomination) {
            ETHDenomination.ETH -> coinValue
            else -> coinValue.divide(ETH_DEC.toBigDecimal(), 18, RoundingMode.HALF_UP)
        }

        return currencyFormatUtil.formatEth(value)
    }

    /**
     * Returns formatted string of supplied coin value.
     * (ie 1,000.00, 0.0001)
     *
     * @param coinValue Value of the coin
     * @param coinDenomination Denomination of the coinValue supplied
     * @return ETH decimal formatted amount
     */
    fun getFormattedEthShortValue(
        coinValue: BigDecimal,
        coinDenomination: ETHDenomination
    ): String {
        val value = when (coinDenomination) {
            ETHDenomination.ETH -> coinValue
            else -> coinValue.divide(ETH_DEC.toBigDecimal(), 18, RoundingMode.HALF_UP)
        }

        return currencyFormatUtil.formatEthShort(value)
    }
    // endregion

    // region Convert methods
    /**
     * Returns btc amount from satoshis.
     *
     * @return btc, mbtc or bits relative to what is set in monetaryUtil
     */
    fun getTextFromSatoshis(satoshis: Long, decimalSeparator: String): String {
        var displayAmount = getFormattedSelectedCoinValue(satoshis.toBigDecimal())
        displayAmount = displayAmount.replace(".", decimalSeparator)
        return displayAmount
    }

    /**
     * Returns amount of satoshis from btc amount. This could be btc, mbtc or bits.
     *
     * @return satoshis
     */
    fun getSatoshisFromText(text: String?, decimalSeparator: String): BigInteger {
        if (text == null || text.isEmpty()) return BigInteger.ZERO

        val amountToSend = stripSeparator(text, decimalSeparator)

        val amount = try {
            java.lang.Double.parseDouble(amountToSend)
        } catch (e: NumberFormatException) {
            0.0
        }

        return BigDecimal.valueOf(amount)
            .multiply(BigDecimal.valueOf(100000000))
            .toBigInteger()
    }

    /**
     * Returns amount of wei from ether amount.
     *
     * @return satoshis
     */
    fun getWeiFromText(text: String?, decimalSeparator: String): BigInteger {
        if (text == null || text.isEmpty()) return BigInteger.ZERO

        val amountToSend = stripSeparator(text, decimalSeparator)
        return Convert.toWei(amountToSend, Convert.Unit.ETHER).toBigInteger()
    }

    fun stripSeparator(text: String, decimalSeparator: String): String = text.trim { it <= ' ' }
        .replace(" ", "")
        .replace(decimalSeparator, ".")
    // endregion

    companion object {
        private const val BTC_DEC = 1e8
        private const val ETH_DEC = 1e18
    }
}

fun String.toSafeDouble(locale: Locale): Double = try {
    var amount = this
    if (amount.isEmpty()) amount = "0"
    NumberFormat.getInstance(locale).parse(amount).toDouble()
} catch (e: ParseException) {
    0.0
}

fun String.toSafeLong(locale: Locale): Long = try {
    var amount = this
    if (amount.isEmpty()) amount = "0"
    Math.round(NumberFormat.getInstance(locale).parse(amount).toDouble() * 1e8)
} catch (e: ParseException) {
    0L
}
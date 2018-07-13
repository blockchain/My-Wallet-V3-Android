package piuk.blockchain.androidcore.data.currency

import info.blockchain.balance.CryptoCurrency
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * This class allows us to format decimal values for clean UI display.
 */
class CurrencyFormatUtil @Inject constructor() {

    private lateinit var btcFormat: DecimalFormat
    private lateinit var ethFormat: DecimalFormat
    private lateinit var fiatFormat: DecimalFormat
    private lateinit var ethShortFormat: DecimalFormat

    private val btcUnit = CryptoCurrency.BTC.symbol
    private val bchUnit = CryptoCurrency.BCH.symbol
    private val ethUnit = CryptoCurrency.ETHER.symbol

    private val maxEthShortDecimalLength = 8
    private val maxBtcDecimalLength = 8
    private val maxEthDecimalLength = 18

    init {
        val defaultLocale = Locale.getDefault()

        fiatFormat = (NumberFormat.getInstance(defaultLocale) as DecimalFormat).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }

        btcFormat = (NumberFormat.getInstance(defaultLocale) as DecimalFormat).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = maxBtcDecimalLength
        }

        ethFormat = (NumberFormat.getInstance(defaultLocale) as DecimalFormat).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = maxEthDecimalLength
        }

        ethShortFormat = (NumberFormat.getInstance(defaultLocale) as DecimalFormat).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = maxEthShortDecimalLength
        }
    }

    fun getBtcUnit() = btcUnit

    fun getBchUnit() = bchUnit

    fun getEthUnit() = ethUnit

    fun getBtcMaxFractionDigits() = maxBtcDecimalLength

    fun getBchMaxFractionDigits() = maxBtcDecimalLength

    fun getEthMaxFractionDigits() = maxEthDecimalLength

    fun formatFiat(fiatBalance: BigDecimal, fiatUnit: String): String =
        getFiatFormat(fiatUnit).format(fiatBalance)

    /**
     * TODO: This is seriously slow and causes noticeable UI lag. We should move fetching the
     * number format to a factory which can be called from a Presenter, with the result cached
     * there and passed to this method. This avoids two problems:
     *
     * 1) Expensive multiple fetches of [NumberFormat] saved within method scope for no real reason.
     *
     * 2) Ties the currently selected [Locale] to the UI and it's associated lifecycle. If we moved
     * [NumberFormat] to a property in this class, it would have to be invalidated when the [Locale]
     * is changed. By fetching [NumberFormat] from a factory in the Presenter, we avoid having to
     * invalidate the [Locale] on changing, as the Presenter will be released and GC'd anyway.
     *
     * NumberFormatFactory.getDefault(locale: Locale) -> Presenter
     *
     * Not doing now because I need to get this release out.
     */
    fun formatFiatWithSymbol(fiatValue: Double, currencyCode: String, locale: Locale): String {
        val numberFormat = NumberFormat.getCurrencyInstance(locale)
        val decimalFormatSymbols = (numberFormat as DecimalFormat).decimalFormatSymbols
        numberFormat.decimalFormatSymbols = decimalFormatSymbols.apply {
            this.currencySymbol = Currency.getInstance(currencyCode).getSymbol(locale)
        }
        return numberFormat.format(fiatValue)
    }

    fun getFiatSymbol(currencyCode: String, locale: Locale): String =
        Currency.getInstance(currencyCode).getSymbol(locale)

    fun formatBtc(btc: BigDecimal): String = btcFormat.format(btc.toNaturalNumber()).toWebZero()

    fun formatSatoshi(satoshi: Long): String =
        btcFormat.format(satoshi.div(BTC_DEC).toNaturalNumber()).toWebZero()

    fun formatBch(bch: BigDecimal): String = formatBtc(bch)

    fun formatEth(eth: BigDecimal): String = ethFormat.format(eth.toNaturalNumber()).toWebZero()

    fun formatEthShort(eth: BigDecimal): String =
        ethShortFormat.format(eth.toNaturalNumber()).toWebZero()

    fun formatWei(wei: Long): String =
        ethFormat.format(wei.div(ETH_DEC).toNaturalNumber()).toWebZero()

    fun formatBtcWithUnit(btc: BigDecimal): String {
        val amountFormatted = btcFormat.format(btc.toNaturalNumber()).toWebZero()
        return "$amountFormatted $btcUnit"
    }

    fun formatBchWithUnit(bch: BigDecimal): String {
        val amountFormatted = btcFormat.format(bch.toNaturalNumber()).toWebZero()
        return "$amountFormatted $bchUnit"
    }

    fun formatEthWithUnit(eth: BigDecimal): String {
        val amountFormatted = ethFormat.format(eth.toNaturalNumber()).toWebZero()
        return "$amountFormatted $ethUnit"
    }

    fun formatEthShortWithUnit(eth: BigDecimal): String {
        val amountFormatted = ethShortFormat.format(eth.toNaturalNumber()).toWebZero()
        return "$amountFormatted $ethUnit"
    }

    fun formatWeiWithUnit(wei: Long): String {
        val amountFormatted = ethFormat.format(wei.div(ETH_DEC).toNaturalNumber()).toWebZero()
        return "$amountFormatted $ethUnit"
    }

    /**
     * Returns the Fiat format as a [NumberFormat] object for a given currency code.
     *
     * @param fiat The currency code (ie USD) for the format you wish to return
     * @return A [NumberFormat] object with the correct decimal fractions for the chosen Fiat format
     * @see ExchangeRateFactory.getCurrencyLabels
     */
    // TODO This should be private but is exposed for CurrencyFormatManager for now until usage removed
    fun getFiatFormat(currencyCode: String) =
        fiatFormat.apply { currency = Currency.getInstance(currencyCode) }

    companion object {
        private const val BTC_DEC = 1e8
        private const val ETH_DEC = 1e18
    }
}

private fun BigDecimal.toNaturalNumber() = Math.max(this.toDouble(), 0.0)

private fun Double.toNaturalNumber() = Math.max(this, 0.0)

// Replace 0.0 with 0 to match web
private fun String.toWebZero() = if (this == "0.0" || this == "0.00") "0" else this
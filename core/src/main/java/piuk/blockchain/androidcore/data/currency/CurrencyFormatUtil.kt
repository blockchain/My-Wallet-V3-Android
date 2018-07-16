package piuk.blockchain.androidcore.data.currency

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

private const val MaxEthShortDecimalLength = 8

/**
 * This class allows us to format decimal values for clean UI display.
 */
class CurrencyFormatUtil @Inject constructor() {
    private val btcFormat = createDecimalFormat(1, CryptoCurrency.BTC.dp)
    private val bchFormat = createDecimalFormat(1, CryptoCurrency.BCH.dp)
    private val ethFormat = createDecimalFormat(1, CryptoCurrency.ETHER.dp)
    private val fiatFormat = createDecimalFormat(2, 2)

    private val ethShortFormat = createDecimalFormat(1, MaxEthShortDecimalLength)

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

    @Deprecated("Use format")
    fun formatBtc(btc: BigDecimal): String = btcFormat.format(btc.toPositiveDouble()).toWebZero()

    fun formatSatoshi(satoshi: Long): String =
        btcFormat.format(satoshi.div(BTC_DEC).toPositiveDouble()).toWebZero()

    fun formatBch(bch: BigDecimal): String = formatBtc(bch)

    fun formatEth(eth: BigDecimal): String = ethFormat.format(eth.toPositiveDouble()).toWebZero()

    fun formatWei(wei: Long): String =
        ethFormat.format(wei.div(ETH_DEC).toPositiveDouble()).toWebZero()

    fun format(cryptoValue: CryptoValue): String =
        cryptoValue.currency.decimalFormat().formatWithoutUnit(cryptoValue.toMajorUnit())

    fun formatWithUnit(cryptoValue: CryptoValue) =
        cryptoValue.currency.decimalFormat().formatWithUnit(cryptoValue.toMajorUnit(), cryptoValue.currency.symbol)

    @Deprecated("Use formatWithUnit")
    fun formatBtcWithUnit(btc: BigDecimal) = btcFormat.formatWithUnit(btc, CryptoCurrency.BTC.symbol)

    @Deprecated("Use formatWithUnit")
    fun formatBchWithUnit(bch: BigDecimal) = bchFormat.formatWithUnit(bch, CryptoCurrency.BCH.symbol)

    fun formatEthWithUnit(eth: BigDecimal) = ethFormat.formatWithUnit(eth, CryptoCurrency.ETHER.symbol)

    @Deprecated("Use formatWithUnit")
    fun formatEthShortWithUnit(eth: BigDecimal): String {
        return ethShortFormat.formatWithUnit(eth, CryptoCurrency.ETHER.symbol)
    }

    fun formatWeiWithUnit(wei: Long): String {
        val amountFormatted = ethFormat.format(wei.div(ETH_DEC).toPositiveDouble()).toWebZero()
        return "$amountFormatted ${CryptoCurrency.ETHER.symbol}"
    }

    private fun DecimalFormat.formatWithUnit(value: BigDecimal, symbol: String) =
        "${formatWithoutUnit(value)} $symbol"

    private fun DecimalFormat.formatWithoutUnit(value: BigDecimal) =
        format(value.toPositiveDouble()).toWebZero()

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

    private fun CryptoCurrency.decimalFormat() = when (this) {
        CryptoCurrency.BTC -> btcFormat
        CryptoCurrency.BCH -> bchFormat
        CryptoCurrency.ETHER -> ethShortFormat
    }
}

private fun BigDecimal.toPositiveDouble() = this.toDouble().toPositiveDouble()

private fun Double.toPositiveDouble() = Math.max(this, 0.0)

// Replace 0.0 with 0 to match web
private fun String.toWebZero() = if (this == "0.0" || this == "0.00") "0" else this

private fun createDecimalFormat(minDigits: Int, maxDigits: Int) =
    (NumberFormat.getInstance(Locale.getDefault()) as DecimalFormat).apply {
        minimumFractionDigits = minDigits
        maximumFractionDigits = maxDigits
    }

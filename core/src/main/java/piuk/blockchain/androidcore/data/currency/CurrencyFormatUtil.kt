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

    fun formatFiatWithSymbol(fiatValue: Double, currencyCode: String, locale: Locale): String {
        val numberFormat = LocaleCurrencyNumberFormat[locale]
        val decimalFormatSymbols = (numberFormat as DecimalFormat).decimalFormatSymbols
        numberFormat.decimalFormatSymbols = decimalFormatSymbols.apply {
            this.currencySymbol = Currency.getInstance(currencyCode).getSymbol(locale)
        }
        return numberFormat.format(fiatValue)
    }

    fun getFiatSymbol(currencyCode: String, locale: Locale): String =
        Currency.getInstance(currencyCode).getSymbol(locale)

    @Deprecated("Use format", replaceWith = ReplaceWith("format(CryptoValue.bitcoinFromMajor(btc))"))
    fun formatBtc(btc: BigDecimal): String = format(CryptoValue.bitcoinFromMajor(btc))

    @Deprecated("Use format", replaceWith = ReplaceWith("format(CryptoValue.bitcoinFromSatoshis(satoshi))"))
    fun formatSatoshi(satoshi: Long): String = format(CryptoValue.bitcoinFromSatoshis(satoshi))

    @Deprecated("Use format", replaceWith = ReplaceWith("format(CryptoValue.bitcoinCashFromMajor(bch))"))
    fun formatBch(bch: BigDecimal): String = format(CryptoValue.bitcoinCashFromMajor(bch))

    @Deprecated("Use format", replaceWith = ReplaceWith("format(CryptoValue.etherFromMajor(eth), Precision.Full)"))
    fun formatEth(eth: BigDecimal): String = format(CryptoValue.etherFromMajor(eth), Precision.Full)

    @Deprecated("Use format", replaceWith = ReplaceWith("format(CryptoValue.etherFromWei(wei), Precision.Full)"))
    fun formatWei(wei: Long): String = format(CryptoValue.etherFromWei(wei), Precision.Full)

    fun format(cryptoValue: CryptoValue, displayMode: Precision = Precision.Short): String =
        cryptoValue.currency.decimalFormat(displayMode).formatWithoutUnit(cryptoValue.toMajorUnit())

    fun formatWithUnit(cryptoValue: CryptoValue, displayMode: Precision = Precision.Short) =
        cryptoValue.currency.decimalFormat(displayMode).formatWithUnit(
            cryptoValue.toMajorUnit(),
            cryptoValue.currency.symbol
        )

    enum class Precision {
        /**
         * Some currencies will be displayed at a shorter length
         */
        Short,
        /**
         * Full decimal place precision is used for the display string
         */
        Full
    }

    @Deprecated("Use formatWithUnit", replaceWith = ReplaceWith("formatWithUnit(CryptoValue.bitcoinFromMajor(btc))"))
    fun formatBtcWithUnit(btc: BigDecimal) = formatWithUnit(CryptoValue.bitcoinFromMajor(btc))

    @Deprecated(
        "Use formatWithUnit",
        replaceWith = ReplaceWith("formatWithUnit(CryptoValue.bitcoinCashFromMajor(bch))")
    )
    fun formatBchWithUnit(bch: BigDecimal) = formatWithUnit(CryptoValue.bitcoinCashFromMajor(bch))

    @Deprecated(
        "Use formatWithUnit",
        replaceWith = ReplaceWith("formatWithUnit(CryptoValue.etherFromMajor(eth), Precision.Full)")
    )
    fun formatEthWithUnit(eth: BigDecimal) = formatWithUnit(CryptoValue.etherFromMajor(eth), Precision.Full)

    @Deprecated(
        "Use formatWithUnit",
        replaceWith = ReplaceWith("formatWithUnit(CryptoValue.etherFromMajor(eth), Precision.Short)")
    )
    fun formatEthShortWithUnit(eth: BigDecimal) = formatWithUnit(CryptoValue.etherFromMajor(eth), Precision.Short)

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
    internal fun getFiatFormat(currencyCode: String) =
        fiatFormat.apply { currency = Currency.getInstance(currencyCode) }

    companion object {

        private const val ETH_DEC = 1e18
    }

    private fun CryptoCurrency.decimalFormat(displayMode: Precision) = when (this) {
        CryptoCurrency.BTC -> btcFormat
        CryptoCurrency.BCH -> bchFormat
        CryptoCurrency.ETHER -> when (displayMode) {
            Precision.Short -> ethShortFormat
            Precision.Full -> ethFormat
        }
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

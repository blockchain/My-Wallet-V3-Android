package piuk.blockchain.androidcore.data.currency

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.FormatPrecision
import info.blockchain.balance.format
import info.blockchain.balance.formatWithUnit
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/**
 * This class allows us to format decimal values for clean UI display.
 */
@Deprecated("Use the CryptoValue.format and formatWithUnit extension methods.")
class CurrencyFormatUtil @Inject constructor() {
    fun formatFiat(fiatValue: FiatValue): String =
        fiatValue.toStringWithoutSymbol(Locale.getDefault())

    fun formatFiatWithSymbol(fiatValue: FiatValue, locale: Locale) =
        fiatValue.toStringWithSymbol(locale)

    @Deprecated(
        "", replaceWith =
        ReplaceWith("formatFiatWithSymbol(FiatValue(currencyCode, fiatValue.toBigDecimal()), locale)")
    )
    fun formatFiatWithSymbol(fiatValue: Double, currencyCode: String, locale: Locale) =
        formatFiatWithSymbol(FiatValue(currencyCode, fiatValue.toBigDecimal()), locale)

    fun getFiatSymbol(currencyCode: String, locale: Locale): String =
        Currency.getInstance(currencyCode).getSymbol(locale)

    @Deprecated("Use format", replaceWith = ReplaceWith("CryptoValue.bitcoinFromMajor(btc).format()"))
    fun formatBtc(btc: BigDecimal): String = format(CryptoValue.bitcoinFromMajor(btc))

    @Deprecated("Use format", replaceWith = ReplaceWith("CryptoValue.bitcoinFromSatoshis(satoshi).format()"))
    fun formatSatoshi(satoshi: Long): String = format(CryptoValue.bitcoinFromSatoshis(satoshi))

    @Deprecated("Use format", replaceWith = ReplaceWith("CryptoValue.bitcoinCashFromMajor(bch).format()"))
    fun formatBch(bch: BigDecimal): String = format(CryptoValue.bitcoinCashFromMajor(bch))

    @Deprecated("Use format", replaceWith = ReplaceWith("CryptoValue.etherFromMajor(eth).format(FormatPrecision.Full)"))
    fun formatEth(eth: BigDecimal): String = format(CryptoValue.etherFromMajor(eth), FormatPrecision.Full)

    @Deprecated("Use format", replaceWith = ReplaceWith("CryptoValue.etherFromWei(wei).format(FormatPrecision.Full)"))
    fun formatWei(wei: Long): String = format(CryptoValue.etherFromWei(wei), FormatPrecision.Full)

    @Deprecated("Use format", replaceWith = ReplaceWith("cryptoValue.format(displayMode)"))
    fun format(cryptoValue: CryptoValue, displayMode: FormatPrecision = FormatPrecision.Short): String =
        cryptoValue.format(precision = displayMode)

    @Deprecated("Use format", replaceWith = ReplaceWith("cryptoValue.formatWithUnit(displayMode)"))
    fun formatWithUnit(cryptoValue: CryptoValue, displayMode: FormatPrecision = FormatPrecision.Short) =
        cryptoValue.formatWithUnit(precision = displayMode)

    @Deprecated("Use formatWithUnit", replaceWith = ReplaceWith("formatWithUnit(CryptoValue.bitcoinFromMajor(btc))"))
    fun formatBtcWithUnit(btc: BigDecimal) = formatWithUnit(CryptoValue.bitcoinFromMajor(btc))

    @Deprecated(
        "Use formatWithUnit",
        replaceWith = ReplaceWith("formatWithUnit(CryptoValue.bitcoinCashFromMajor(bch))")
    )
    fun formatBchWithUnit(bch: BigDecimal) = formatWithUnit(CryptoValue.bitcoinCashFromMajor(bch))

    @Deprecated(
        "Use formatWithUnit",
        replaceWith = ReplaceWith("formatWithUnit(CryptoValue.etherFromMajor(eth), FormatPrecision.Full)")
    )
    fun formatEthWithUnit(eth: BigDecimal) = formatWithUnit(CryptoValue.etherFromMajor(eth), FormatPrecision.Full)

    @Deprecated(
        "Use formatWithUnit",
        replaceWith = ReplaceWith("formatWithUnit(CryptoValue.etherFromMajor(eth), FormatPrecision.Short)")
    )
    fun formatEthShortWithUnit(eth: BigDecimal) = formatWithUnit(CryptoValue.etherFromMajor(eth), FormatPrecision.Short)
}

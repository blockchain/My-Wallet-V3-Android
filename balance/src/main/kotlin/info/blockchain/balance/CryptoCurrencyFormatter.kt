package info.blockchain.balance

import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Locale

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

fun CryptoValue.format(displayMode: Precision = Precision.Short): String =
    CryptoCurrencyFormatter.format(this, displayMode)

fun CryptoValue.formatWithUnit(displayMode: Precision = Precision.Short) =
    CryptoCurrencyFormatter.formatWithUnit(this, displayMode)

private const val MaxEthShortDecimalLength = 8

private object CryptoCurrencyFormatter {
    private val btcFormat = createCryptoDecimalFormat(CryptoCurrency.BTC.dp)
    private val bchFormat = createCryptoDecimalFormat(CryptoCurrency.BCH.dp)
    private val ethFormat = createCryptoDecimalFormat(CryptoCurrency.ETHER.dp)
    private val ethShortFormat =
        createCryptoDecimalFormat(MaxEthShortDecimalLength)

    fun format(
        cryptoValue: CryptoValue,
        displayMode: Precision = Precision.Short
    ): String =
        cryptoValue.currency.decimalFormat(displayMode).formatWithoutUnit(cryptoValue.toMajorUnit())

    fun formatWithUnit(
        cryptoValue: CryptoValue,
        displayMode: Precision = Precision.Short
    ) =
        cryptoValue.currency.decimalFormat(displayMode).formatWithUnit(
            cryptoValue.toMajorUnit(),
            cryptoValue.currency.symbol
        )

    private fun CryptoCurrency.decimalFormat(displayMode: Precision) = when (this) {
        CryptoCurrency.BTC -> btcFormat
        CryptoCurrency.BCH -> bchFormat
        CryptoCurrency.ETHER -> when (displayMode) {
            Precision.Short -> ethShortFormat
            Precision.Full -> ethFormat
        }
    }

    private fun DecimalFormat.formatWithUnit(value: BigDecimal, symbol: String) =
        "${formatWithoutUnit(value)} $symbol"

    private fun DecimalFormat.formatWithoutUnit(value: BigDecimal) =
        format(value.toPositiveDouble()).toWebZero()
}

private fun BigDecimal.toPositiveDouble() = this.toDouble().toPositiveDouble()

private fun Double.toPositiveDouble() = Math.max(this, 0.0)

// Replace 0.0 with 0 to match web
private fun String.toWebZero() = if (this == "0.0" || this == "0.00") "0" else this

private fun createCryptoDecimalFormat(maxDigits: Int) =
    (NumberFormat.getInstance(Locale.getDefault()) as DecimalFormat).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = maxDigits
    }

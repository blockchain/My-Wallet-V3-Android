package info.blockchain.balance

import java.math.BigDecimal
import java.math.BigInteger
import java.util.Locale

data class CryptoValue(
    val currency: CryptoCurrency,

    /**
     * Amount in the smallest unit of the currency, Satoshi/Wei for example.
     */
    val amount: BigInteger
) : Money {

    override val maxDecimalPlaces: Int = currency.dp

    override val userDecimalPlaces: Int = currency.userDp

    override fun symbol(locale: Locale) = currency.symbol

    override fun toStringWithSymbol(locale: Locale) = formatWithUnit(locale)

    override fun toStringWithoutSymbol(locale: Locale) = format(locale)

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    override fun toBigDecimal(): BigDecimal = amount.toBigDecimal().movePointLeft(currency.dp)

    override val isPositive: Boolean get() = amount.signum() == 1

    override val isZero: Boolean get() = amount.signum() == 0

    companion object {
        val ZeroBtc = bitcoinFromSatoshis(0L)
        val ZeroBch = bitcoinCashFromSatoshis(0L)
        val ZeroEth = CryptoValue(CryptoCurrency.ETHER, BigInteger.ZERO)

        fun zero(cryptoCurrency: CryptoCurrency) = when (cryptoCurrency) {
            CryptoCurrency.BTC -> ZeroBtc
            CryptoCurrency.BCH -> ZeroBch
            CryptoCurrency.ETHER -> ZeroEth
        }

        fun bitcoinFromSatoshis(satoshi: Long) = CryptoValue(CryptoCurrency.BTC, satoshi.toBigInteger())
        fun bitcoinFromSatoshis(satoshi: BigInteger) = CryptoValue(CryptoCurrency.BTC, satoshi)
        fun bitcoinCashFromSatoshis(satoshi: Long) = CryptoValue(CryptoCurrency.BCH, satoshi.toBigInteger())
        fun bitcoinCashFromSatoshis(satoshi: BigInteger) = CryptoValue(CryptoCurrency.BCH, satoshi)
        @Deprecated(
            message = "Long overflows when the value goes above 9.22+ Ether.",
            replaceWith = ReplaceWith("CryptoValue.etherFromWei(wei.toBigInteger())")
        )
        fun etherFromWei(wei: Long) = CryptoValue(CryptoCurrency.ETHER, wei.toBigInteger())

        fun etherFromWei(wei: BigInteger) = CryptoValue(CryptoCurrency.ETHER, wei)

        fun bitcoinFromMajor(bitcoin: Int) = bitcoinFromMajor(bitcoin.toBigDecimal())
        fun bitcoinFromMajor(bitcoin: BigDecimal) = fromMajor(CryptoCurrency.BTC, bitcoin)

        fun bitcoinCashFromMajor(bitcoinCash: Int) = bitcoinCashFromMajor(bitcoinCash.toBigDecimal())
        fun bitcoinCashFromMajor(bitcoinCash: BigDecimal) = fromMajor(CryptoCurrency.BCH, bitcoinCash)

        fun etherFromMajor(ether: Long) = etherFromMajor(ether.toBigDecimal())
        fun etherFromMajor(ether: BigDecimal) = fromMajor(CryptoCurrency.ETHER, ether)

        fun fromMajor(
            currency: CryptoCurrency,
            major: BigDecimal
        ) = CryptoValue(currency, major.movePointRight(currency.dp).toBigInteger())

        fun min(a: CryptoValue, b: CryptoValue) = if (a <= b) a else b

        fun max(a: CryptoValue, b: CryptoValue) = if (a >= b) a else b
    }

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    fun toMajorUnitDouble() = toBigDecimal().toDouble()

    override fun toZero(): CryptoValue = zero(currency)
}

operator fun CryptoValue.compareTo(other: CryptoValue): Int {
    ensureComparable(currency, other.currency)
    return amount.compareTo(other.amount)
}

private fun ensureComparable(a: CryptoCurrency, b: CryptoCurrency) {
    if (a != b) throw Exception("Can't compare ${a.symbol} and ${b.symbol}")
}

fun CryptoCurrency.withMajorValue(majorValue: BigDecimal) = CryptoValue.fromMajor(this, majorValue)

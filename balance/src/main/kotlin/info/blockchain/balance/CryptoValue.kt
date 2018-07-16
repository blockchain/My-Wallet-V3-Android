package info.blockchain.balance

import java.math.BigDecimal
import java.math.BigInteger

data class CryptoValue(
    val currency: CryptoCurrency,

    /**
     * Amount in the smallest unit of the currency, Satoshi/Wei for example.
     */
    val amount: BigInteger
) {
    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    fun toMajorUnit(): BigDecimal {
        return currency.smallestUnitValueToBigDecimal(amount)
    }

    fun isPositive(): Boolean {
        return amount.signum() == 1
    }

    companion object {
        val ZeroBtc = bitcoinFromSatoshis(0L)
        val ZeroBch = bitcoinCashFromSatoshis(0L)
        val ZeroEth = CryptoValue(CryptoCurrency.ETHER, BigInteger.ZERO)

        fun bitcoinFromSatoshis(satoshi: Long) = CryptoValue(CryptoCurrency.BTC, satoshi.toBigInteger())
        fun bitcoinCashFromSatoshis(satoshi: Long) = CryptoValue(CryptoCurrency.BCH, satoshi.toBigInteger())
        fun etherFromWei(wei: Long) = CryptoValue(CryptoCurrency.ETHER, wei.toBigInteger())

        fun bitcoinFromMajor(bitcoin: Int) = fromMajor(CryptoCurrency.BTC, bitcoin.toBigDecimal())

        fun bitcoinCashFromMajor(bitcoinCash: Int) = fromMajor(CryptoCurrency.BCH, bitcoinCash.toBigDecimal())

        fun etherFromMajor(ether: Long) = fromMajor(CryptoCurrency.ETHER, ether.toBigDecimal())

        private fun fromMajor(
            currency: CryptoCurrency,
            bitcoin: BigDecimal
        ) = CryptoValue(currency, bitcoin.movePointRight(currency.dp).toBigInteger())
    }

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    fun toMajorUnitDouble() = toMajorUnit().toDouble()
}
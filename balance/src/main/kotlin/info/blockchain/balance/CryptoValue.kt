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
        return currency.smallestUnitLongToBigDecimal(amount)
    }

    fun isPositive(): Boolean {
        return amount.signum() == 1
    }

    companion object {
        val ZeroBtc = bitcoin(0L)
        val ZeroBch = bitcoinCash(0L)
        val ZeroEth = CryptoValue(CryptoCurrency.ETHER, BigInteger.ZERO)

        fun bitcoin(amount: Long) = CryptoValue(CryptoCurrency.BTC, amount.toBigInteger())
        fun bitcoinCash(amount: Long) = CryptoValue(CryptoCurrency.BCH, amount.toBigInteger())
    }

    /**
     * Amount in the major value of the currency, Bitcoin/Ether for example.
     */
    fun toMajorUnitDouble() = toMajorUnit().toDouble()
}
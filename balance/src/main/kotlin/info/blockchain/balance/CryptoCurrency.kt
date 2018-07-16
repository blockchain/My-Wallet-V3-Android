package info.blockchain.balance

import java.math.BigDecimal
import java.math.BigInteger

enum class CryptoCurrency(val symbol: String, val unit: String, val dp: Int) {
    BTC("BTC", "Bitcoin", 8),
    ETHER("ETH", "Ether", 18),
    BCH("BCH", "Bitcoin Cash", 8);

    internal fun smallestUnitValueToBigDecimal(amount: BigInteger): BigDecimal {
        return amount.toBigDecimal().movePointLeft(dp)
    }

    companion object {

        fun fromSymbol(symbol: String): CryptoCurrency? =
            CryptoCurrency.values().firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
    }
}

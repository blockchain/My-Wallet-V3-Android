package info.blockchain.balance

import java.math.BigDecimal
import java.math.BigInteger

enum class CryptoCurrency(
    val symbol: String,
    val unit: String,
    val dp: Int,
    val requiredConfirmations: Int
) {
    BTC(
        symbol = "BTC",
        unit = "Bitcoin",
        dp = 8,
        requiredConfirmations = 3
    ),
    ETHER(
        symbol = "ETH",
        unit = "Ether",
        dp = 18,
        requiredConfirmations = 12
    ),
    BCH(
        symbol = "BCH",
        unit = "Bitcoin Cash",
        dp = 8,
        requiredConfirmations = 3
    );

    internal fun smallestUnitValueToBigDecimal(amount: BigInteger): BigDecimal {
        return amount.toBigDecimal().movePointLeft(dp)
    }

    companion object {

        fun fromSymbol(symbol: String?): CryptoCurrency? =
            CryptoCurrency.values().firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }

        fun fromSymbolOrThrow(symbol: String?): CryptoCurrency =
            fromSymbol(symbol) ?: throw Exception("Bad currency symbol \"$symbol\"")
    }
}

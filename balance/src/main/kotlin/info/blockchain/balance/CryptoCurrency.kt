package info.blockchain.balance

enum class CryptoCurrency(
    val symbol: String,
    val unit: String,
    val dp: Int,
    internal val userDp: Int,
    val requiredConfirmations: Int
) {
    BTC(
        symbol = "BTC",
        unit = "Bitcoin",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 3
    ),
    ETHER(
        symbol = "ETH",
        unit = "Ether",
        dp = 18,
        userDp = 8,
        requiredConfirmations = 12
    ),
    BCH(
        symbol = "BCH",
        unit = "Bitcoin Cash",
        dp = 8,
        userDp = 8,
        requiredConfirmations = 3
    );

    companion object {

        fun fromSymbol(symbol: String?): CryptoCurrency? =
            CryptoCurrency.values().firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }

        fun fromSymbolOrThrow(symbol: String?): CryptoCurrency =
            fromSymbol(symbol) ?: throw IllegalArgumentException("Bad currency symbol \"$symbol\"")
    }
}

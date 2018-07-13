package info.blockchain.balance

enum class CryptoCurrency(val symbol: String, val unit: String) {
    BTC("BTC", "Bitcoin"),
    ETHER("ETH", "Ether"),
    BCH("BCH", "Bitcoin Cash");

    companion object {

        fun fromSymbol(symbol: String): CryptoCurrency? =
            CryptoCurrency.values().firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
    }
}

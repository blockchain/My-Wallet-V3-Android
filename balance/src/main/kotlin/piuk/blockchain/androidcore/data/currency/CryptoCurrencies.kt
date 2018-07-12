package piuk.blockchain.androidcore.data.currency

enum class CryptoCurrencies(val symbol: String, val unit: String) {
    BTC("BTC", "Bitcoin"),
    ETHER("ETH", "Ether"),
    BCH("BCH", "Bitcoin Cash");

    companion object {

        fun fromSymbol(symbol: String): CryptoCurrencies? =
            CryptoCurrencies.values().firstOrNull { it.symbol.equals(symbol, ignoreCase = true) }
    }
}

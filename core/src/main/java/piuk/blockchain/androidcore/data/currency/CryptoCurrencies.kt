package piuk.blockchain.androidcore.data.currency

enum class CryptoCurrencies(val symbol: String, val unit: String) {
    BTC("BTC", "Bitcoin"),
    ETHER("ETH", "Ether"),
    BCH("BCH", "Bitcoin Cash");

    companion object {

        fun fromString(text: String): CryptoCurrencies? =
            CryptoCurrencies.values().firstOrNull { it.symbol.equals(text, ignoreCase = true) }
    }
}

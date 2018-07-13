package info.blockchain.balance

/**
 * Account key describes a part of you wallet. It doesn't have value or labels, but can be used in methods that enables
 * those to be looked up or calculated.
 */
sealed class AccountKey(val currency: CryptoCurrency) {

    /**
     * Represents your entire wallet in the given [currency]
     */
    class EntireWallet(currency: CryptoCurrency) : AccountKey(currency)

    /**
     * Represents just the imported addresses in the given [currency]
     */
    class OnlyImported(currency: CryptoCurrency) : AccountKey(currency)

    /**
     * Represents just a single [address] within your wallet of the given [currency]
     */
    class SingleAddress(currency: CryptoCurrency, val address: String) : AccountKey(currency)
}

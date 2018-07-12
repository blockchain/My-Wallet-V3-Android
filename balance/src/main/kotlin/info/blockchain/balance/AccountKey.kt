package info.blockchain.balance

import piuk.blockchain.androidcore.data.currency.CryptoCurrencies

/**
 * Account key describes a part of you wallet. It doesn't have value or labels, but can be used in methods that enables
 * those to be looked up or calculated.
 */
sealed class AccountKey(val currency: CryptoCurrencies) {

    /**
     * Represents your entire wallet in the given [currency]
     */
    class EntireWallet(currency: CryptoCurrencies) : AccountKey(currency)

    /**
     * Represents just the imported addresses in the given [currency]
     */
    class OnlyImported(currency: CryptoCurrencies) : AccountKey(currency)

    /**
     * Represents just a single [address] within your wallet of the given [currency]
     */
    class SingleAddress(currency: CryptoCurrencies, val address: String) : AccountKey(currency)
}

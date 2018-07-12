package info.blockchain.balance

import piuk.blockchain.androidcore.data.currency.CryptoCurrencies

data class CryptoValue(
    val currency: CryptoCurrencies,
    val amount: Long
)

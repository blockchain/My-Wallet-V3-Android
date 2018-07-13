package info.blockchain.balance

import piuk.blockchain.androidcore.data.currency.CryptoCurrency

data class CryptoValue(
    val currency: CryptoCurrency,
    val amount: Long
)

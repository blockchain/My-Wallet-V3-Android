package com.blockchain.morph.quote

import com.blockchain.morph.CoinPair
import info.blockchain.balance.CryptoValue

/**
 * [ExchangeQuote] is a response to a [ExchangeQuoteRequest] that comes from the Morph api.
 */
sealed class ExchangeQuote {
    class Error(val error: String) : ExchangeQuote()

    class Success(
        val pair: CoinPair,
        val withdrawalAmount: CryptoValue,
        val depositAmount: CryptoValue
    ) : ExchangeQuote()
}

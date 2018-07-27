package com.blockchain.morph.quote

import com.blockchain.morph.CoinPair
import com.blockchain.morph.to
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue

sealed class ExchangeQuoteRequest(val pair: CoinPair) {

    class Selling(val offering: CryptoValue, val wanted: CryptoCurrency) :
        ExchangeQuoteRequest(offering.currency to wanted)

    class Buying(val offering: CryptoCurrency, val wanted: CryptoValue) :
        ExchangeQuoteRequest(offering to wanted.currency)
}

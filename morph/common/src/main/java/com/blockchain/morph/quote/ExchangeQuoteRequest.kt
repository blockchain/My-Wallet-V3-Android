package com.blockchain.morph.quote

import com.blockchain.morph.CoinPair
import com.blockchain.morph.to
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue

sealed class ExchangeQuoteRequest(val pair: CoinPair) {

    class Selling(
        val offering: CryptoValue,
        val wanted: CryptoCurrency
    ) : ExchangeQuoteRequest(offering.currency to wanted)

    class Buying(
        val offering: CryptoCurrency,
        val wanted: CryptoValue
    ) : ExchangeQuoteRequest(offering to wanted.currency)

    class SellingFiatLinked(
        val offering: CryptoCurrency,
        val wanted: CryptoCurrency,
        val offeringFiatValue: FiatValue
    ) : ExchangeQuoteRequest(offering to wanted)

    class BuyingFiatLinked(
        val offering: CryptoCurrency,
        val wanted: CryptoCurrency,
        val wantedFiatValue: FiatValue
    ) : ExchangeQuoteRequest(offering to wanted)
}

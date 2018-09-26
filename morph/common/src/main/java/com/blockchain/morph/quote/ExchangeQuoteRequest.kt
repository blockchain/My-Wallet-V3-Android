package com.blockchain.morph.quote

import com.blockchain.morph.CoinPair
import com.blockchain.morph.to
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue

sealed class ExchangeQuoteRequest(val pair: CoinPair) {

    abstract val fiatSymbol: String

    class Selling(
        val offering: CryptoValue,
        val wanted: CryptoCurrency,
        val indicativeFiatSymbol: String = ""
    ) : ExchangeQuoteRequest(offering.currency to wanted) {

        override val fiatSymbol: String
            get() = indicativeFiatSymbol
    }

    class Buying(
        val offering: CryptoCurrency,
        val wanted: CryptoValue,
        val indicativeFiatSymbol: String = ""
    ) : ExchangeQuoteRequest(offering to wanted.currency) {

        override val fiatSymbol: String
            get() = indicativeFiatSymbol
    }

    class SellingFiatLinked(
        val offering: CryptoCurrency,
        val wanted: CryptoCurrency,
        val offeringFiatValue: FiatValue
    ) : ExchangeQuoteRequest(offering to wanted) {

        override val fiatSymbol: String
            get() = offeringFiatValue.currencyCode
    }

    class BuyingFiatLinked(
        val offering: CryptoCurrency,
        val wanted: CryptoCurrency,
        val wantedFiatValue: FiatValue
    ) : ExchangeQuoteRequest(offering to wanted) {

        override val fiatSymbol: String
            get() = wantedFiatValue.currencyCode
    }
}

package com.blockchain.morph.homebrew

import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.serialization.JsonSerializable
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.withMajorValue
import java.math.BigDecimal

internal data class QuoteMessageJson(
    val sequenceNumber: Int,
    val channel: String,
    val type: String,
    val quote: QuoteJson?
) : JsonSerializable

internal data class QuoteJson(
    val pair: String,
    val fiatCurrency: String,
    val fix: String,
    val volume: BigDecimal,
    val currencyRatio: CurrencyRatio
) : JsonSerializable

internal data class CurrencyRatio(
    val base: CryptoAndFiat,
    val counter: CryptoAndFiat,
    val baseToFiatRate: String,
    val baseToCounterRate: String,
    val counterToBaseRate: String,
    val counterToFiatRate: String
) : JsonSerializable

internal data class CryptoAndFiat(
    val fiat: Value,
    val crypto: Value
) : JsonSerializable

internal data class Value(
    val symbol: String,
    val value: BigDecimal
) : JsonSerializable

internal fun QuoteJson.mapToQuote(): Quote {
    return Quote(
        from = currencyRatio.base.mapToQuoteValue(),
        to = currencyRatio.counter.mapToQuoteValue(),
        rawQuote = this
    )
}

private fun CryptoAndFiat.mapToQuoteValue(): Quote.Value {
    return Quote.Value(
        cryptoValue = crypto.toCryptoValue(),
        fiatValue = fiat.toFiatValue()
    )
}

private fun Value.toFiatValue() =
    FiatValue.fromMajor(symbol, value)

private fun Value.toCryptoValue() =
    CryptoCurrency.fromSymbolOrThrow(symbol).withMajorValue(value)

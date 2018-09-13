package com.blockchain.morph.homebrew

import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.serialization.JsonSerializable
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue

internal data class QuoteMessageJson(
    val sequenceNumber: Int,
    val channel: String,
    val type: String,
    val quote: QuoteJson
) : JsonSerializable

internal data class QuoteJson(
    val pair: String,
    val fiatCurrency: String,
    val fix: String,
    val volume: Double,
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
    val value: Double
) : JsonSerializable

internal fun CurrencyRatio.mapToQuote(): Quote {
    return Quote(
        from = base.mapToQuoteValue(),
        to = counter.mapToQuoteValue()
    )
}

private fun CryptoAndFiat.mapToQuoteValue(): Quote.Value {
    return Quote.Value(
        cryptoValue = crypto.toCryptoValue(),
        fiatValue = fiat.toFiatValue()
    )
}

private fun Value.toFiatValue() =
    FiatValue.fromMajor(symbol, value.toBigDecimal())

private fun Value.toCryptoValue() =
    CryptoValue.fromMajor(
        CryptoCurrency.fromSymbol(symbol) ?: throw Exception("Bad currency symbol $symbol"),
        value.toBigDecimal()
    )

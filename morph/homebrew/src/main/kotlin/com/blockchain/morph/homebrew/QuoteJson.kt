package com.blockchain.morph.homebrew

import com.blockchain.morph.exchange.mvi.Quote
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue

internal data class QuoteJson(
    val sequenceNumber: Int,
    val channel: String,
    val type: String,
    val pair: String,
    val fiatCurrency: String,
    val fix: String,
    val volume: Double,
    val advice: Advice
)

internal data class Advice(
    val base: CryptoAndFiat,
    val counter: CryptoAndFiat
)

internal data class CryptoAndFiat(
    val fiat: Value,
    val crypto: Value
)

internal data class Value(
    val symbol: String,
    val value: Double
)

internal fun Advice.mapToQuote(): Quote {
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
    FiatValue(symbol, value.toBigDecimal())

private fun Value.toCryptoValue() =
    CryptoValue.fromMajor(
        CryptoCurrency.fromSymbol(symbol) ?: throw Exception("Bad currency symbol $symbol"),
        value.toBigDecimal()
    )

package com.blockchain.morph.homebrew

import com.blockchain.morph.exchange.mvi.Fix
import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.nabu.api.CryptoAndFiat
import com.blockchain.nabu.api.QuoteJson
import com.blockchain.nabu.api.Value
import com.blockchain.serialization.JsonSerializable
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.balance.withMajorValue

internal data class QuoteMessageJson(
    val sequenceNumber: Int,
    val channel: String,
    val type: String,
    val quote: QuoteJson?
) : JsonSerializable

internal fun QuoteJson.mapToQuote(): Quote {
    return Quote(
        fix = fix.stringToFix(),
        from = currencyRatio.base.mapToQuoteValue(),
        to = currencyRatio.counter.mapToQuoteValue(),
        rawQuote = this
    )
}

internal fun String.stringToFix() = when (this) {
    "base" -> Fix.BASE_CRYPTO
    "baseInFiat" -> Fix.BASE_FIAT
    "counter" -> Fix.COUNTER_CRYPTO
    "counterInFiat" -> Fix.COUNTER_FIAT
    else -> throw IllegalArgumentException("Unknown fix \"$this\"")
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

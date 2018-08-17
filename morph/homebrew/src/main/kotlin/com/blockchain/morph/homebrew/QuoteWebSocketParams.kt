package com.blockchain.morph.homebrew

import com.blockchain.morph.quote.ExchangeQuoteRequest
import info.blockchain.balance.format
import io.reactivex.Observable
import java.util.Locale

data class QuoteWebSocketParams(
    val pair: String,
    val volume: String,
    val volumeCurrency: String,
    val isBase: Boolean?
)

fun Observable<ExchangeQuoteRequest>.mapToSocketParameters(): Observable<QuoteWebSocketParams> =
    map(ExchangeQuoteRequest::mapToSocketParameters)

internal fun ExchangeQuoteRequest.mapToSocketParameters() =
    when (this) {
        is ExchangeQuoteRequest.Selling ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = offering.format(),
                volumeCurrency = offering.currency.symbol,
                isBase = null
            )
        is ExchangeQuoteRequest.SellingFiatLinked ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = offeringFiatValue.toStringWithoutSymbol(Locale.US),
                volumeCurrency = offeringFiatValue.currencyCode,
                isBase = true
            )
        is ExchangeQuoteRequest.Buying ->
            QuoteWebSocketParams(
                pair = pair.inverse().pairCodeUpper,
                volume = wanted.format(),
                volumeCurrency = wanted.currency.symbol,
                isBase = null
            )
        is ExchangeQuoteRequest.BuyingFiatLinked ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = wantedFiatValue.toStringWithoutSymbol(Locale.US),
                volumeCurrency = wantedFiatValue.currencyCode,
                isBase = false
            )
    }

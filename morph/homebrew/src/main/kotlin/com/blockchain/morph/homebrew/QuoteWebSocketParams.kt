package com.blockchain.morph.homebrew

import com.blockchain.morph.quote.ExchangeQuoteRequest
import info.blockchain.balance.format
import io.reactivex.Observable
import java.util.Locale

data class QuoteWebSocketParams(
    val pair: String,
    val volume: String,
    val fiatCurrency: String,
    val fix: String
)

fun Observable<ExchangeQuoteRequest>.mapToSocketParameters(): Observable<QuoteWebSocketParams> =
    map(ExchangeQuoteRequest::mapToSocketParameters)

internal fun ExchangeQuoteRequest.mapToSocketParameters() =
    when (this) {
        is ExchangeQuoteRequest.Selling ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = offering.format(),
                fiatCurrency = indicativeFiatSymbol,
                fix = "base"
            )
        is ExchangeQuoteRequest.SellingFiatLinked ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = offeringFiatValue.toStringWithoutSymbol(Locale.US),
                fiatCurrency = offeringFiatValue.currencyCode,
                fix = "baseInFiat"
            )
        is ExchangeQuoteRequest.Buying ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = wanted.format(),
                fiatCurrency = indicativeFiatSymbol,
                fix = "counter"
            )
        is ExchangeQuoteRequest.BuyingFiatLinked ->
            QuoteWebSocketParams(
                pair = pair.pairCodeUpper,
                volume = wantedFiatValue.toStringWithoutSymbol(Locale.US),
                fiatCurrency = wantedFiatValue.currencyCode,
                fix = "counterInFiat"
            )
    }

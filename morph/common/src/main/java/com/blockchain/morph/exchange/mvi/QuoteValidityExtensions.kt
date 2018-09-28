package com.blockchain.morph.exchange.mvi

import io.reactivex.Observable
import java.util.concurrent.TimeUnit

fun Observable<ExchangeViewState>.allQuoteClearingConditions(): Observable<ClearQuoteIntent> =
    Observable.merge(
        quoteOutDated(),
        quoteMismatch()
    )

fun Observable<ExchangeViewState>.quoteOutDated(): Observable<ClearQuoteIntent> {
    return quoteTimeout()
        .map { ClearQuoteIntent }
}

fun Observable<ExchangeViewState>.quoteMismatch(): Observable<ClearQuoteIntent> =
    quoteMatchesUserInput()
        .filter { !it }
        .map { ClearQuoteIntent }

private fun Observable<ExchangeViewState>.quoteTimeout() =
    distinctUntilChanged { a, b ->
        a.latestQuote === b.latestQuote
    }.debounce(10, TimeUnit.SECONDS)

private fun Observable<ExchangeViewState>.quoteMatchesUserInput(): Observable<Boolean> {
    return map {
        it.lastUserValue == it.latestQuote?.fixValue
    }.debounce(250, TimeUnit.MILLISECONDS)
}

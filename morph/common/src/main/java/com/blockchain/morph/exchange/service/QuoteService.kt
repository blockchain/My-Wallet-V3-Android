package com.blockchain.morph.exchange.service

import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.quote.ExchangeQuoteRequest
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

interface ReadOnlyQuoteStream {

    /**
     * Stream of quotes
     */
    val quotes: Observable<Quote>
}

interface QuoteStream : ReadOnlyQuoteStream {
    /**
     * Replace the last quote request with a new request
     */
    fun updateQuoteRequest(quoteRequest: ExchangeQuoteRequest)
}

interface QuoteService : QuoteStream {

    /**
     * Start the service. On [Disposable.dispose] it will close.
     */
    fun openAsDisposable(): Disposable

    /**
     * Stream of connection status
     */
    val connectionStatus: Observable<Status>

    enum class Status {
        Open,
        Closed,
        Error
    }
}

interface QuoteServiceFactory {

    fun createQuoteService(): QuoteService
}

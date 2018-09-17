package com.blockchain.morph.homebrew

import com.blockchain.morph.quote.ExchangeQuoteRequest
import com.blockchain.network.websocket.WebSocket
import com.blockchain.network.websocket.toJsonSocket
import com.squareup.moshi.Moshi
import io.reactivex.Observable

import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.homebrew.json.Out
import com.blockchain.network.websocket.ConnectionEvent
import com.blockchain.network.websocket.openAsDisposable

class QuoteWebSocket(private val underlyingSocket: WebSocket<String, String>, moshi: Moshi) : QuoteService {

    private val socket = underlyingSocket.toJsonSocket<Out, QuoteMessageJson>(moshi)

    override fun updateQuoteRequest(quoteRequest: ExchangeQuoteRequest) {
        updateSocketParameters(quoteRequest.mapToSocketParameters())
    }

    override val quotes: Observable<Quote>
        get() = socket.responses
            .filter { it.quote != null }
            .map { it.quote!!.currencyRatio.mapToQuote() }

    override val connectionStatus: Observable<QuoteService.Status>
        get() = socket.connectionEvents.map {
            when (it) {
                is ConnectionEvent.ClientDisconnect -> QuoteService.Status.Closed
                is ConnectionEvent.Connected -> QuoteService.Status.Closed
                is ConnectionEvent.Authenticated -> QuoteService.Status.Open
                is ConnectionEvent.Failure -> QuoteService.Status.Error
            }
        }

    override fun openAsDisposable() = underlyingSocket.openAsDisposable()

    private var params: QuoteWebSocketParams? = null

    private fun updateSocketParameters(newSocketParameters: QuoteWebSocketParams) {
        val oldParams = params
        if (oldParams == newSocketParameters) return
        params = newSocketParameters
        oldParams?.let { socket.send(Out.UnsubscribePair(it)) }
        socket.send(Out.Subscribe(newSocketParameters))
    }
}

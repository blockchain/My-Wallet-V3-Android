package com.blockchain.morph.homebrew

import com.blockchain.morph.quote.ExchangeQuoteRequest
import com.blockchain.network.websocket.WebSocket
import com.blockchain.network.websocket.toJsonSocket
import com.squareup.moshi.Moshi
import io.reactivex.Observable

import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.service.ExchangeRateStream
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.exchange.service.QuoteStream
import com.blockchain.network.websocket.ConnectionEvent
import com.blockchain.network.websocket.WebSocketChannel
import com.blockchain.network.websocket.channelAware
import com.blockchain.network.websocket.openAsDisposable

class QuoteWebSocketStream(underlyingSocket: WebSocket<String, String>, moshi: Moshi) : QuoteStream {

    private val socket = underlyingSocket.toJsonSocket<String, QuoteMessageJson>(moshi)
    private val channelAware = underlyingSocket.channelAware()

    override fun updateQuoteRequest(quoteRequest: ExchangeQuoteRequest) {
        updateSocketParameters(quoteRequest.mapToSocketParameters())
    }

    override val quotes: Observable<Quote>
        get() = socket.responses
            .filter { it.quote != null }
            .map { it.quote!!.mapToQuote() }

    private var params: QuoteWebSocketParams? = null

    private var conversionChannel: WebSocketChannel<String>? = null

    private fun updateSocketParameters(newSocketParameters: QuoteWebSocketParams) {
        val oldParams = params
        if (oldParams == newSocketParameters) return
        params = newSocketParameters
        oldParams?.let { conversionChannel?.close(QuoteWebSocketUnsubscribeParams(it.pair, "conversionPair")) }
        conversionChannel = channelAware.openChannel("conversion", newSocketParameters)
    }
}

class QuoteWebSocket(
    private val underlyingSocket: WebSocket<String, String>,
    moshi: Moshi,
    private val quoteWebSocketStream: QuoteStream = QuoteWebSocketStream(underlyingSocket, moshi),
    private val exchangeRateStream: ExchangeRateStream = ExchangeWebSocket(underlyingSocket, moshi)
) : QuoteService,
    QuoteStream by quoteWebSocketStream,
    ExchangeRateStream by exchangeRateStream {

    override fun updateQuoteRequest(quoteRequest: ExchangeQuoteRequest) {
        quoteWebSocketStream.updateQuoteRequest(quoteRequest)
        exchangeRateStream.updateQuoteRequest(quoteRequest)
    }

    override val connectionStatus: Observable<QuoteService.Status>
        get() = underlyingSocket.connectionEvents.map {
            when (it) {
                is ConnectionEvent.ClientDisconnect -> QuoteService.Status.Closed
                is ConnectionEvent.Connected -> QuoteService.Status.Closed
                is ConnectionEvent.Authenticated -> QuoteService.Status.Open
                is ConnectionEvent.Failure -> QuoteService.Status.Error
            }
        }

    override fun openAsDisposable() = underlyingSocket.openAsDisposable()
}

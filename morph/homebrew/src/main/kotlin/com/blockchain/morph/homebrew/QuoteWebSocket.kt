package com.blockchain.morph.homebrew

import com.blockchain.morph.quote.ExchangeQuoteRequest
import com.blockchain.network.websocket.WebSocket
import com.blockchain.network.websocket.toJsonSocket
import com.squareup.moshi.Moshi
import io.reactivex.Observable

import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.homebrew.json.Out

class QuoteWebSocket(underlyingSocket: WebSocket<String, String>, moshi: Moshi) : QuoteService {

    private val socket = underlyingSocket.toJsonSocket<Out, QuoteMessageJson>(moshi)

    override fun subscribe(quoteRequest: ExchangeQuoteRequest) {
        updateSocketParameters(quoteRequest.mapToSocketParameters())
    }

    override val quotes: Observable<Quote>
        get() = socket.responses
            .filter { it.quote != null }
            .map { it.quote!!.currencyRatio.mapToQuote() }

    private var params: QuoteWebSocketParams? = null

    private fun updateSocketParameters(newSocketParameters: QuoteWebSocketParams) {
        val oldParams = params
        if (oldParams == newSocketParameters) return
        params = newSocketParameters
        oldParams?.let { socket.send(Out.UnsubscribePair(it)) }
        socket.send(Out.Subscribe(newSocketParameters))
    }
}

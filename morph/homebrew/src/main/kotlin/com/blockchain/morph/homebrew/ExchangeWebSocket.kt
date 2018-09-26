package com.blockchain.morph.homebrew

import com.blockchain.morph.exchange.service.ExchangeRateStream
import com.blockchain.morph.quote.ExchangeQuoteRequest
import com.blockchain.network.websocket.WebSocket
import com.blockchain.network.websocket.WebSocketChannel
import com.blockchain.network.websocket.channelAware
import com.blockchain.network.websocket.channelMessageFilter
import com.blockchain.network.websocket.toJsonReceive
import com.blockchain.serialization.JsonSerializable
import com.squareup.moshi.Moshi
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import io.reactivex.Observable
import java.util.concurrent.atomic.AtomicReference

private const val ChannelNameExchangeRate = "exchange_rate"

class ExchangeWebSocket(private val underlyingSocket: WebSocket<String, String>, moshi: Moshi) : ExchangeRateStream {

    private val ratesSocket = underlyingSocket
        .channelMessageFilter(ChannelNameExchangeRate, throwErrors = false)
        .toJsonReceive<ExchangeRateJson>(moshi)

    private val channelAware = underlyingSocket.channelAware()

    override fun updateQuoteRequest(quoteRequest: ExchangeQuoteRequest) {
        updateExchangeRatesParameters(quoteRequest.mapToExchangeRateSocketParameters())
    }
    override val rates: Observable<ExchangeRate>
        get() = ratesSocket.responses
            .flatMapIterable {
                if (it.rates == null) {
                    emptyList()
                } else {
                    it.rates.map { rate ->
                        val split = rate.pair.split("-")
                        val lhs = split.firstOrNull()
                        val rhs = split.lastOrNull()
                        val fromCrypto = CryptoCurrency.fromSymbol(rhs)
                        if (fromCrypto != null && lhs != null) {
                            ExchangeRate.CryptoToFiat(fromCrypto, lhs, rate.price)
                        } else {
                            null
                        }
                    }.filter { it != null }
                }
            }

    private var exchangeChannel: WebSocketChannel<String>? = null

    private var lastExchangeRatePair = AtomicReference<ExchangeRateSocketParametersKey?>()

    private fun updateExchangeRatesParameters(parameters: ExchangeRateSocketParametersKey) {
        if (lastExchangeRatePair.getAndSet(parameters) == parameters) return
        exchangeChannel = channelAware.openChannel(ChannelNameExchangeRate, parameters.toSocketParams())
    }
}

private fun ExchangeRateSocketParametersKey.toSocketParams() =
    ExchangeRateSocketParameters(pairs = listOf(pair))

private fun ExchangeQuoteRequest.mapToExchangeRateSocketParameters() =
    ExchangeRateSocketParametersKey(pair.from, fiatSymbol)

private data class ExchangeRateSocketParametersKey(
    val fromCrypto: CryptoCurrency,
    val toFiat: String
) {
    val pair = "${fromCrypto.symbol}-$toFiat"
}

internal class ExchangeRateSocketParameters(
    val type: String = "exchangeRates",
    @Suppress("unused") val pairs: List<String>
) : JsonSerializable

package com.blockchain.morph.homebrew

import com.blockchain.morph.exchange.mvi.Quote
import com.blockchain.morph.exchange.service.TradeExecutionService
import com.blockchain.nabu.api.TradeRequest
import com.blockchain.nabu.service.NabuMarketsService

internal class HomeBrewTradeExecutionService(private val marketsService: NabuMarketsService) : TradeExecutionService {

    override fun executeTrade(quote: Quote, destinationAddress: String, refundAddress: String) {
        val rawQuote = quote.rawQuote ?: throw IllegalArgumentException("No quote supplied")
        val quoteJson = rawQuote as? QuoteJson ?: throw IllegalArgumentException("Quote is not expected type")

        marketsService.executeTrade(
            TradeRequest(
                destinationAddress = destinationAddress,
                refundAddress = refundAddress,
                quote = quoteJson
            )
        )
    }
}

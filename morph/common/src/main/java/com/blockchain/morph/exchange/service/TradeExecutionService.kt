package com.blockchain.morph.exchange.service

import com.blockchain.morph.exchange.mvi.Quote

interface TradeExecutionService {

    fun executeTrade(
        quote: Quote,
        destinationAddress: String,
        refundAddress: String
    )
}

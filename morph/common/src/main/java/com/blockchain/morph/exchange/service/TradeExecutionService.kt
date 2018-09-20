package com.blockchain.morph.exchange.service

import com.blockchain.morph.CoinPair
import com.blockchain.morph.exchange.mvi.Quote
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import java.math.BigDecimal

interface TradeExecutionService {

    fun executeTrade(
        quote: Quote,
        destinationAddress: String,
        refundAddress: String
    ): Single<TradeTransaction>
}

interface TradeTransaction {
    val id: String
    val createdAt: String
    val pair: CoinPair
    val rate: BigDecimal
    val fee: CryptoValue
    val fiatValue: FiatValue
    val refundAddress: String
    val depositAddress: String
    val deposit: CryptoValue
    val withdrawalAddress: String
    val withdrawal: CryptoValue
    val hashOut: String?
}

package com.blockchain.nabu.api

import com.blockchain.morph.CoinPair
import com.blockchain.morph.exchange.service.TradeTransaction
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue

data class NabuTransaction(
    override val id: String,
    override val createdAt: String,
    override val pair: CoinPair,
    override val fee: CryptoValue,
    override val fiatValue: FiatValue,
    override val refundAddress: String,
    override val depositAddress: String,
    override val deposit: CryptoValue,
    override val withdrawalAddress: String,
    override val withdrawal: CryptoValue,
    val state: TransactionState,
    override val hashOut: String? = null
) : TradeTransaction

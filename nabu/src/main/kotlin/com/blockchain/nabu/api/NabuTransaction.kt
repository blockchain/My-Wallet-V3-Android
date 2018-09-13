package com.blockchain.nabu.api

import com.blockchain.morph.CoinPair
import info.blockchain.balance.CryptoValue
import java.math.BigDecimal

data class NabuTransaction(
    val id: String,
    val createdAt: String,
    val pair: CoinPair,
    val rate: BigDecimal,
    val refundAddress: String,
    val depositAddress: String,
    val deposit: CryptoValue,
    val withdrawalAddress: String,
    val withdrawal: CryptoValue,
    val state: TransactionState,
    val hashOut: String? = null
)
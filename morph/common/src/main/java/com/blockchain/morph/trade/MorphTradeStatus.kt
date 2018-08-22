package com.blockchain.morph.trade

import com.blockchain.morph.CoinPair
import com.blockchain.morph.to
import info.blockchain.balance.CryptoValue

interface MorphTradeStatus {

    val incomingValue: CryptoValue
    val outgoingValue: CryptoValue

    val address: String
    val transaction: String

    val status: MorphTrade.Status

    val pair: CoinPair get() = incomingValue.currency to outgoingValue.currency
}

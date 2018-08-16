package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue

data class Quote(val from: Value, val to: Value) {

    data class Value(
        val cryptoValue: CryptoValue,
        val fiatValue: FiatValue
    )
}

package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money

enum class Fix {
    BASE_FIAT,
    BASE_CRYPTO,
    COUNTER_FIAT,
    COUNTER_CRYPTO
}

data class Quote(
    val fix: Fix,
    val from: Value,
    val to: Value,
    val rawQuote: Any? = null
) {
    val fixValue: Money =
        when (fix) {
            Fix.BASE_FIAT -> from.fiatValue
            Fix.BASE_CRYPTO -> from.cryptoValue
            Fix.COUNTER_FIAT -> to.fiatValue
            Fix.COUNTER_CRYPTO -> to.cryptoValue
        }

    data class Value(
        val cryptoValue: CryptoValue,
        val fiatValue: FiatValue
    )
}

val Fix.isCounter: Boolean
    get() = this == Fix.COUNTER_CRYPTO || this == Fix.COUNTER_FIAT

val Fix.isBase: Boolean
    get() = this == Fix.BASE_CRYPTO || this == Fix.BASE_FIAT

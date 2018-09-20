package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import java.math.BigDecimal

data class ExchangeViewModel(
    val from: Value,
    val to: Value,
    val latestQuote: Quote? = null
)

data class Value(
    val cryptoValue: CryptoValue,
    val fiatValue: FiatValue,
    val cryptoMode: Mode,
    val fiatMode: Mode
) {
    enum class Mode {
        UserEntered,
        UpToDate,
        OutOfDate
    }
}

fun initial(fiatCode: String, from: CryptoCurrency, to: CryptoCurrency) =
    ExchangeViewModel(
        from = Value(
            CryptoValue.zero(from),
            FiatValue.fromMajor(fiatCode, BigDecimal.ZERO),
            Value.Mode.UpToDate,
            Value.Mode.UpToDate
        ),
        to = Value(
            CryptoValue.zero(to),
            FiatValue.fromMajor(fiatCode, BigDecimal.ZERO),
            Value.Mode.UpToDate,
            Value.Mode.UpToDate
        )
    )
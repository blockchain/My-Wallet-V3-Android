package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import java.math.BigDecimal

data class ExchangeViewModel(
    val fromAccount: AccountReference,
    val toAccount: AccountReference,
    val from: Value,
    val to: Value,
    val latestQuote: Quote? = null
) {
    fun isValid(): Boolean =
        latestQuote != null &&
        latestQuote.fix == fixedField &&
        latestQuote.fixValue == fixedMoneyValue

    val fromCryptoCurrency = fromAccount.cryptoCurrency
    val toCryptoCurrency = toAccount.cryptoCurrency
}

val ExchangeViewModel.fixedField: Fix
    get() = when {
        to.cryptoMode == Value.Mode.UserEntered -> Fix.COUNTER_CRYPTO
        to.fiatMode == Value.Mode.UserEntered -> Fix.COUNTER_FIAT
        from.fiatMode == Value.Mode.UserEntered -> Fix.BASE_FIAT
        else -> Fix.BASE_CRYPTO
    }

val ExchangeViewModel.fixedMoneyValue: Money
    get() = when (fixedField) {
        Fix.BASE_CRYPTO -> from.cryptoValue
        Fix.COUNTER_CRYPTO -> to.cryptoValue
        Fix.BASE_FIAT -> from.fiatValue
        Fix.COUNTER_FIAT -> to.fiatValue
    }

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

fun initial(fiatCode: String, from: AccountReference, to: AccountReference) =
    ExchangeViewModel(
        fromAccount = from,
        toAccount = to,
        from = Value(
            CryptoValue.zero(from.cryptoCurrency),
            FiatValue.fromMajor(fiatCode, BigDecimal.ZERO),
            Value.Mode.UpToDate,
            Value.Mode.UpToDate
        ),
        to = Value(
            CryptoValue.zero(to.cryptoCurrency),
            FiatValue.fromMajor(fiatCode, BigDecimal.ZERO),
            Value.Mode.UpToDate,
            Value.Mode.UpToDate
        )
    )
package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.withMajorValue
import io.reactivex.Observable
import java.math.BigDecimal

/**
 * The dialog is the conversation between the User and the System.
 */
class ExchangeDialog(intents: Observable<ExchangeIntent>, initial: ExchangeViewModel) {

    val viewStates: Observable<ExchangeViewState> =
        intents.scan(initial.toInternalState()) { previousState, intent ->
            when (intent) {
                is SimpleFieldUpdateIntent -> previousState.map(intent)
                is SwapIntent -> previousState.mapSwap()
                is QuoteIntent -> previousState.mapQuote(intent)
                is ChangeCryptoFromAccount -> previousState.map(intent)
                is ChangeCryptoToAccount -> previousState.map(intent)
                is ToggleFiatCryptoIntent -> previousState.toggleFiatCrypto()
                is ToggleFromToIntent -> previousState.toggleFromTo()
                is SetFixIntent -> previousState.mapSetFix(intent)
                is SetTradeLimits -> previousState.mapTradeLimits(intent)
                is ApplyMinimumLimit -> previousState.applyLimit(previousState.minTradeLimit)
                is ApplyMaximumLimit -> previousState.applyLimit(previousState.maxTradeLimit)
                is FiatExchangeRateIntent -> previousState.setFiatRate(intent.c2fRate)
                is SpendableValueIntent -> previousState.setSpendable(intent.cryptoValue)
            }
        }

    val viewModels: Observable<ExchangeViewModel> =
        viewStates.map {
            it.toViewModel()
        }
}

private fun ExchangeViewState.applyLimit(tradeLimit: FiatValue?) =
    tradeLimit?.let {
        copy(
            fix = Fix.BASE_FIAT,
            fromFiat = it
        )
    } ?: this

private fun ExchangeViewState.setSpendable(cryptoValue: CryptoValue): ExchangeViewState {
    if (cryptoValue.currency != fromAccount.cryptoCurrency) {
        return this
    }
    return copy(maxSpendable = cryptoValue)
}

private fun ExchangeViewState.setFiatRate(c2fRate: ExchangeRate.CryptoToFiat): ExchangeViewState {
    if (c2fRate.to != fromFiat.currencyCode || c2fRate.from != fromAccount.cryptoCurrency) {
        return this
    }
    return copy(c2fRate = c2fRate)
}

private fun ExchangeViewState.mapTradeLimits(intent: SetTradeLimits): ExchangeViewState {
    if (intent.min.currencyCode != fromFiat.currencyCode) return this
    return copy(
        minTradeLimit = intent.min,
        maxTradeLimit = intent.max
    )
}

private fun ExchangeViewModel.toInternalState(): ExchangeViewState {
    return ExchangeViewState(
        fromAccount = fromAccount,
        toAccount = toAccount,
        fix = fixedField,
        upToDate = true,
        fromCrypto = from.cryptoValue,
        fromFiat = from.fiatValue,
        toFiat = to.fiatValue,
        toCrypto = to.cryptoValue,
        latestQuote = latestQuote
    )
}

private fun ExchangeViewState.resetToZero(): ExchangeViewState {
    return copy(
        fromFiat = FiatValue.fromMajor(fromFiat.currencyCode, BigDecimal.ZERO),
        toFiat = FiatValue.fromMajor(toFiat.currencyCode, BigDecimal.ZERO),
        fromCrypto = CryptoValue.zero(fromAccount.cryptoCurrency),
        toCrypto = CryptoValue.zero(toAccount.cryptoCurrency),
        upToDate = false
    )
}

private fun ExchangeViewState.mapSetFix(intent: SetFixIntent): ExchangeViewState {
    return copy(fix = intent.fix)
}

fun ExchangeViewState.toViewModel(): ExchangeViewModel {
    return ExchangeViewModel(
        fromAccount = fromAccount,
        toAccount = toAccount,
        from = Value(
            cryptoValue = fromCrypto,
            fiatValue = fromFiat,
            cryptoMode = mode(fix, Fix.BASE_CRYPTO, fromCrypto, upToDate),
            fiatMode = mode(fix, Fix.BASE_FIAT, fromFiat, upToDate)
        ),
        to = Value(
            cryptoValue = toCrypto,
            fiatValue = toFiat,
            cryptoMode = mode(fix, Fix.COUNTER_CRYPTO, toCrypto, upToDate),
            fiatMode = mode(fix, Fix.COUNTER_FIAT, toFiat, upToDate)
        ),
        latestQuote = latestQuote
    )
}

data class ExchangeViewState(
    val fromAccount: AccountReference,
    val toAccount: AccountReference,
    val fix: Fix,
    val upToDate: Boolean,
    val fromCrypto: CryptoValue,
    val toCrypto: CryptoValue,
    val fromFiat: FiatValue,
    val toFiat: FiatValue,
    val latestQuote: Quote?,
    val minTradeLimit: FiatValue? = null,
    val maxTradeLimit: FiatValue? = null,
    val c2fRate: ExchangeRate.CryptoToFiat? = null,
    val maxSpendable: CryptoValue? = null
) {
    val lastUserValue: Money =
        when (fix) {
            Fix.BASE_FIAT -> fromFiat
            Fix.BASE_CRYPTO -> fromCrypto
            Fix.COUNTER_FIAT -> toFiat
            Fix.COUNTER_CRYPTO -> toCrypto
        }
}

private fun ExchangeViewState.map(intent: SimpleFieldUpdateIntent): ExchangeViewState {
    return when (fix) {
        Fix.BASE_FIAT -> copy(
            fromFiat = FiatValue.fromMajor(fromFiat.currencyCode, intent.userValue),
            upToDate = false
        )
        Fix.BASE_CRYPTO -> copy(fromCrypto = fromCrypto.currency.withMajorValue(intent.userValue), upToDate = false)
        Fix.COUNTER_FIAT -> copy(
            toFiat = FiatValue.fromMajor(toFiat.currencyCode, intent.userValue),
            upToDate = false
        )
        Fix.COUNTER_CRYPTO -> copy(toCrypto = toCrypto.currency.withMajorValue(intent.userValue), upToDate = false)
    }
}

private fun ExchangeViewState.toggleFiatCrypto() = copy(fix = fix.toggleFiatCrypto())

private fun Fix.toggleFiatCrypto() =
    when (this) {
        Fix.BASE_FIAT -> Fix.BASE_CRYPTO
        Fix.BASE_CRYPTO -> Fix.BASE_FIAT
        Fix.COUNTER_FIAT -> Fix.COUNTER_CRYPTO
        Fix.COUNTER_CRYPTO -> Fix.COUNTER_FIAT
    }

private fun ExchangeViewState.toggleFromTo() = copy(fix = fix.toggleFromTo())

private fun Fix.toggleFromTo() =
    when (this) {
        Fix.BASE_FIAT -> Fix.COUNTER_FIAT
        Fix.BASE_CRYPTO -> Fix.COUNTER_CRYPTO
        Fix.COUNTER_FIAT -> Fix.BASE_FIAT
        Fix.COUNTER_CRYPTO -> Fix.BASE_CRYPTO
    }

private fun ExchangeViewState.map(intent: ChangeCryptoFromAccount): ExchangeViewState {
    if (intent.from.cryptoCurrency == toAccount.cryptoCurrency) {
        return copy(
            fromAccount = intent.from,
            toAccount = fromAccount
        ).resetToZero()
    }
    return copy(fromAccount = intent.from).resetToZero()
}

private fun ExchangeViewState.map(intent: ChangeCryptoToAccount): ExchangeViewState {
    if (intent.to.cryptoCurrency == fromAccount.cryptoCurrency) {
        return copy(
            fromAccount = toAccount,
            toAccount = intent.to
        ).resetToZero()
    }
    return copy(toAccount = intent.to).resetToZero()
}

private fun ExchangeViewState.mapQuote(intent: QuoteIntent) =
    if (intent.quote.fix == fix &&
        intent.quote.fixValue == lastUserValue &&
        fromCurrencyMatch(intent) &&
        toCurrencyMatch(intent)
    ) {
        copy(
            fromCrypto = intent.quote.from.cryptoValue,
            fromFiat = intent.quote.from.fiatValue,
            toCrypto = intent.quote.to.cryptoValue,
            toFiat = intent.quote.to.fiatValue,
            latestQuote = intent.quote,
            upToDate = true
        )
    } else {
        this
    }

private fun ExchangeViewState.fromCurrencyMatch(intent: QuoteIntent) =
    currencyMatch(intent.quote.from, fromCrypto, fromFiat)

private fun ExchangeViewState.toCurrencyMatch(intent: QuoteIntent) =
    currencyMatch(intent.quote.to, toCrypto, toFiat)

private fun currencyMatch(
    quote: Quote.Value,
    vmValue: CryptoValue,
    vmFiatValue: FiatValue
) =
    quote.fiatValue.currencyCode == vmFiatValue.currencyCode &&
        quote.cryptoValue.currency == vmValue.currency

private fun ExchangeViewState.mapSwap() =
    copy(
        fromAccount = toAccount,
        toAccount = fromAccount
    ).resetToZero()

private fun mode(
    fieldEntered: Fix,
    field: Fix,
    value: Money,
    upToDate: Boolean = true
): Value.Mode {
    return when {
        fieldEntered == field -> Value.Mode.UserEntered
        value.isPositive -> if (upToDate) Value.Mode.UpToDate else Value.Mode.OutOfDate
        else -> Value.Mode.OutOfDate
    }
}

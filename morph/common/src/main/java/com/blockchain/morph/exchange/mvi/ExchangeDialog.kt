package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import info.blockchain.balance.compareTo
import info.blockchain.balance.times
import info.blockchain.balance.withMajorValue
import io.reactivex.Observable

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
                is ChangeCryptoFromAccount -> previousState.mapNewFromAccount(intent)
                is ChangeCryptoToAccount -> previousState.mapNewToAccount(intent)
                is ToggleFiatCryptoIntent -> previousState.toggleFiatCrypto()
                is ToggleFromToIntent -> previousState.toggleFromTo()
                is SetFixIntent -> previousState.mapSetFix(intent)
                is SetTradeLimits -> previousState.mapTradeLimits(intent)
                is ApplyMinimumLimit -> previousState.applyLimit(previousState.minTradeLimit)
                is ApplyMaximumLimit -> previousState.applyLimit(previousState.maxTrade)
                is FiatExchangeRateIntent -> previousState.setFiatRate(intent.c2fRate)
                is SpendableValueIntent -> previousState.setSpendable(intent.cryptoValue)
                is ClearQuoteIntent -> previousState.clearQuote()
                is SetUserTier -> previousState.copy(userTier = intent.tier)
                is SetTierLimit -> previousState.mapTierLimits(intent)
            }
        }

    val viewModels: Observable<ExchangeViewModel> =
        viewStates.map {
            it.toViewModel()
        }
}

private fun ExchangeViewState.clearQuote() =
    copy(
        latestQuote = null,
        fromFiat = if (fix != Fix.BASE_FIAT) fromFiat.toZero() else fromFiat,
        toFiat = if (fix != Fix.COUNTER_FIAT) toFiat.toZero() else toFiat,
        fromCrypto = if (fix != Fix.BASE_CRYPTO) fromCrypto.toZero() else fromCrypto,
        toCrypto = if (fix != Fix.COUNTER_CRYPTO) toCrypto.toZero() else toCrypto
    )

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

private fun ExchangeViewState.applyLimit(tradeLimit: Money?) =
    when (tradeLimit) {
        null -> this
        is FiatValue -> tradeLimit.let {
            copy(
                fix = Fix.BASE_FIAT,
                fromFiat = it
            )
        }
        is CryptoValue -> tradeLimit.let {
            copy(
                fix = Fix.BASE_CRYPTO,
                fromCrypto = it
            )
        }
        else -> this
    }

private fun ExchangeViewState.mapTradeLimits(intent: SetTradeLimits): ExchangeViewState {
    if (intent.min.currencyCode != fromFiat.currencyCode) return this
    return copy(
        minTradeLimit = intent.min,
        maxTradeLimit = intent.max
    )
}

private fun ExchangeViewState.mapTierLimits(intent: SetTierLimit): ExchangeViewState {
    if (intent.availableOnTier.currencyCode != fromFiat.currencyCode) return this
    return copy(
        maxTierLimit = intent.availableOnTier
    )
}

internal fun ExchangeViewModel.toInternalState(): ExchangeViewState {
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
        fromFiat = fromFiat.toZero(),
        toFiat = toFiat.toZero(),
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
        latestQuote = latestQuote,
        isValid = isValid()
    )
}

enum class QuoteValidity {
    Valid,
    NoQuote,
    MissMatch,
    UnderMinTrade,
    OverMaxTrade,
    OverTierLimit,
    OverUserBalance
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
    val maxTierLimit: FiatValue? = null,
    val c2fRate: ExchangeRate.CryptoToFiat? = null,
    val maxSpendable: CryptoValue? = null,
    val decimalCursor: Int = 0,
    val userTier: Int = 0
) {
    private val maxTradeOrTierLimit: FiatValue?
        get() {
            if (maxTradeLimit != null && maxTierLimit != null) {
                return if (maxTradeLimit < maxTierLimit) maxTradeLimit else maxTierLimit
            }
            return maxTradeLimit ?: maxTierLimit
        }

    val lastUserValue: Money =
        when (fix) {
            Fix.BASE_FIAT -> fromFiat
            Fix.BASE_CRYPTO -> fromCrypto
            Fix.COUNTER_FIAT -> toFiat
            Fix.COUNTER_CRYPTO -> toCrypto
        }

    val maxTrade: Money?
        get() {
            val limit = maxTradeOrTierLimit
            val maxSpendableFiat = maxSpendable * c2fRate ?: return limit
            if (maxSpendableFiat.currencyCode != fromFiat.currencyCode) return limit
            if (limit == null) return maxSpendableFiat
            if (limit.currencyCode != maxSpendableFiat.currencyCode) return null
            return if (maxSpendableFiat > limit) {
                limit
            } else {
                maxSpendable
            }
        }

    private val fixedMoneyValue: Money
        get() = when (fix) {
            Fix.BASE_CRYPTO -> fromCrypto
            Fix.COUNTER_CRYPTO -> toCrypto
            Fix.BASE_FIAT -> fromFiat
            Fix.COUNTER_FIAT -> toFiat
        }

    fun isValid() = validity() == QuoteValidity.Valid

    fun validity(): QuoteValidity {
        if (latestQuote == null) return QuoteValidity.NoQuote
        if (!quoteMatchesFixAndValue(latestQuote)) return QuoteValidity.MissMatch
        if (!enoughFundsIfKnown(latestQuote)) return QuoteValidity.OverUserBalance
        if (exceedsTheFiatLimit(latestQuote, maxTradeLimit)) return QuoteValidity.OverMaxTrade
        if (exceedsTheFiatLimit(latestQuote, maxTierLimit)) return QuoteValidity.OverTierLimit
        if (underTheFiatLimit(latestQuote, minTradeLimit)) return QuoteValidity.UnderMinTrade
        return QuoteValidity.Valid
    }

    private fun exceedsTheFiatLimit(latestQuote: Quote, maxTradeLimit: FiatValue?): Boolean {
        if (maxTradeLimit == null) return false
        return latestQuote.from.fiatValue > maxTradeLimit
    }

    private fun underTheFiatLimit(latestQuote: Quote, minTradeLimit: FiatValue?): Boolean {
        if (minTradeLimit == null) return false
        return latestQuote.from.fiatValue < minTradeLimit
    }

    private fun quoteMatchesFixAndValue(latestQuote: Quote) =
        latestQuote.fix == fix &&
            latestQuote.fixValue == fixedMoneyValue

    private fun enoughFundsIfKnown(latestQuote: Quote): Boolean {
        if (maxSpendable == null) return true
        if (maxSpendable.currency != latestQuote.from.cryptoValue.currency) return true
        return maxSpendable >= latestQuote.from.cryptoValue
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
    }.copy(decimalCursor = intent.decimalCursor)
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

private fun ExchangeViewState.mapNewFromAccount(intent: ChangeCryptoFromAccount) =
    changeAccounts(
        newFrom = intent.from,
        newTo = if (intent.from.cryptoCurrency == toAccount.cryptoCurrency) {
            fromAccount
        } else {
            toAccount
        }
    )

private fun ExchangeViewState.mapNewToAccount(intent: ChangeCryptoToAccount) =
    changeAccounts(
        newFrom = if (intent.to.cryptoCurrency == fromAccount.cryptoCurrency) {
            toAccount
        } else {
            fromAccount
        },
        newTo = intent.to
    )

private fun ExchangeViewState.mapSwap() =
    changeAccounts(
        newFrom = toAccount,
        newTo = fromAccount
    )

private fun ExchangeViewState.changeAccounts(
    newFrom: AccountReference,
    newTo: AccountReference
) =
    copy(fromAccount = newFrom, toAccount = newTo)
        .resetToZeroKeepingUserFiat()

private fun ExchangeViewState.resetToZeroKeepingUserFiat() =
    resetToZero()
        .copy(
            fromFiat = if (fix.isFiat) this.fromFiat else this.fromFiat.toZero(),
            toFiat = if (fix.isFiat) this.toFiat else this.fromFiat.toZero()
        )

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

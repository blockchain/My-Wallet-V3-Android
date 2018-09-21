package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Observable

/**
 * The dialog is the conversation between the User and the System.
 */
class ExchangeDialog(intents: Observable<ExchangeIntent>, initial: ExchangeViewModel) {

    val viewModel: Observable<ExchangeViewModel> =
        intents.scan(InnerState(initial)) { previousState, intent ->
            when (intent) {
                is FieldUpdateIntent -> previousState.map(intent)
                is SwapIntent -> previousState.mapSwap()
                is QuoteIntent -> previousState.mapQuote(intent)
                is ChangeCryptoFromAccount -> previousState.map(intent)
                is ChangeCryptoToAccount -> previousState.map(intent)
                is ToggleFiatCryptoIntent -> previousState.toggleFiatCrypto()
            }
        }.map { it.vm }
}

private fun InnerState.toggleFiatCrypto(): InnerState {
    val newFix = when (this.lastUserInputField) {
        Fix.BASE_FIAT -> Fix.BASE_CRYPTO
        Fix.BASE_CRYPTO -> Fix.BASE_FIAT
        Fix.COUNTER_FIAT -> Fix.COUNTER_CRYPTO
        Fix.COUNTER_CRYPTO -> Fix.COUNTER_FIAT
    }
    return this.changeFix(newFix)
}

private fun InnerState.map(intent: ChangeCryptoFromAccount): InnerState {
    val from = intent.from
    val to = vm.toAccount
    if (from.cryptoCurrency == to.cryptoCurrency) {
        return InnerState(
            initial(
                fiatCode = vm.to.fiatValue.currencyCode,
                from = from,
                to = vm.fromAccount
            )
        )
    }
    return InnerState(
        initial(
            fiatCode = vm.to.fiatValue.currencyCode,
            from = from,
            to = to
        )
    )
}

private fun InnerState.map(intent: ChangeCryptoToAccount): InnerState {
    val from = vm.fromAccount
    val to = intent.to
    if (from.cryptoCurrency == to.cryptoCurrency) {
        return InnerState(
            initial(
                fiatCode = vm.to.fiatValue.currencyCode,
                from = vm.toAccount,
                to = to
            )
        )
    }
    return InnerState(
        initial(
            fiatCode = vm.to.fiatValue.currencyCode,
            from = from,
            to = to
        )
    )
}

private fun InnerState.mapQuote(intent: QuoteIntent) =
    if (intent.quote.fix == lastUserInputField &&
        intent.quote.fixValue == lastUserValue &&
        fromCurrencyMatch(intent) &&
        toCurrencyMatch(intent)
    ) {
        copy(
            vm = vm.copy(
                from = Value(
                    cryptoValue = intent.quote.from.cryptoValue,
                    cryptoMode = mode(lastUserInputField, Fix.BASE_CRYPTO),
                    fiatValue = intent.quote.from.fiatValue,
                    fiatMode = mode(lastUserInputField, Fix.BASE_FIAT)
                ),
                to = Value(
                    cryptoValue = intent.quote.to.cryptoValue,
                    cryptoMode = mode(lastUserInputField, Fix.COUNTER_CRYPTO),
                    fiatValue = intent.quote.to.fiatValue,
                    fiatMode = mode(lastUserInputField, Fix.COUNTER_FIAT)
                ),
                latestQuote = intent.quote
            )
        )
    } else {
        this
    }

private fun InnerState.fromCurrencyMatch(intent: QuoteIntent) =
    currencyMatch(intent.quote.from, vm.from)

private fun InnerState.toCurrencyMatch(intent: QuoteIntent) =
    currencyMatch(intent.quote.to, vm.to)

private fun currencyMatch(
    quote: Quote.Value,
    vmValue: Value
) =
    quote.fiatValue.currencyCode == vmValue.fiatValue.currencyCode &&
        quote.cryptoValue.currency == vmValue.cryptoValue.currency

private fun InnerState.mapSwap() =
    copy(
        fromFiatRate = toFiatRate,
        toFiatRate = fromFiatRate,
        fromToCryptoRate = toFromCryptoRateForSwaps,
        toFromCryptoRateForSwaps = fromToCryptoRate,
        vm = initial(
            vm.to.fiatValue.currencyCode,
            from = vm.toAccount,
            to = vm.fromAccount
        )
    )

private fun InnerState.map(intent: FieldUpdateIntent): InnerState {
    return copy(
        vm = when (intent.fixedField) {
            Fix.BASE_CRYPTO -> {
                val newFrom = CryptoValue.fromMajor(
                    vm.from.cryptoValue.currency,
                    intent.userValue
                )
                vm.copy(
                    from = vm.from.copy(
                        cryptoValue = newFrom
                    )
                )
            }
            Fix.COUNTER_CRYPTO -> {
                val newToValue = CryptoValue.fromMajor(
                    vm.to.cryptoValue.currency,
                    intent.userValue
                )
                vm.copy(
                    to = vm.to.copy(
                        cryptoValue = newToValue
                    )
                )
            }
            Fix.BASE_FIAT -> vm.copy(
                from = vm.from.copy(
                    fiatValue = FiatValue.fromMajor(
                        vm.from.fiatValue.currencyCode,
                        intent.userValue
                    )
                )
            )
            Fix.COUNTER_FIAT -> vm.copy(
                to = vm.to.copy(
                    fiatValue = FiatValue.fromMajor(
                        vm.to.fiatValue.currencyCode,
                        intent.userValue
                    )
                )
            )
        }
    )
}

private fun InnerState.changeFix(newFix: Fix): InnerState {
    return this.copy(
        vm = vm.copy(
            from = vm.from.copy(
                cryptoMode = mode(newFix, Fix.BASE_CRYPTO, vm.from),
                fiatMode = mode(newFix, Fix.BASE_FIAT, vm.from)
            ),
            to = vm.to.copy(
                cryptoMode = mode(newFix, Fix.COUNTER_CRYPTO, vm.to),
                fiatMode = mode(newFix, Fix.COUNTER_FIAT, vm.to)
            )
        )
    )
}

private fun mode(
    fieldEntered: Fix,
    field: Fix,
    value: Any?
): Value.Mode {
    return when {
        fieldEntered == field -> Value.Mode.UserEntered
        value != null -> Value.Mode.UpToDate
        else -> Value.Mode.OutOfDate
    }
}

private fun mode(
    fieldEntered: Fix,
    field: Fix
): Value.Mode {
    return if (fieldEntered == field) {
        Value.Mode.UserEntered
    } else {
        Value.Mode.UpToDate
    }
}

private data class InnerState(
    val vm: ExchangeViewModel,

    val fromToCryptoRate: ExchangeRate.CryptoToCrypto? = null,

    /**
     * This inverse rate should only be used for allowing swaps to know the rate of the new conversion.
     * divide by [fromToCryptoRate] is usual way to get from a "to" to a "from" value.
     */
    val toFromCryptoRateForSwaps: ExchangeRate.CryptoToCrypto? = null,

    val fromFiatRate: ExchangeRate.CryptoToFiat? = null,

    val toFiatRate: ExchangeRate.CryptoToFiat? = null
) {
    val lastUserInputField: Fix = vm.fixedField

    val lastUserValue: Money = vm.fixedMoneyValue
}

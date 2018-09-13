package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.div
import info.blockchain.balance.times
import io.reactivex.Observable
import java.math.BigDecimal

/**
 * The dialog is the conversation between the User and the System.
 */
class ExchangeDialog(intents: Observable<ExchangeIntent>, initial: ExchangeViewModel) {

    val viewModel: Observable<ExchangeViewModel> =
        intents.scan(InnerState(initial)) { previousState, intent ->
            when (intent) {
                is FieldUpdateIntent -> previousState.map(intent)
                is CoinExchangeRateUpdateIntent -> previousState.map(intent)
                is FiatExchangeRateUpdateIntent -> previousState.map(intent)
                is SwapIntent -> previousState.mapSwap()
                is QuoteIntent -> previousState.mapQuote(intent)
            }
        }.map { it.vm }
}

private fun InnerState.mapQuote(intent: QuoteIntent) =
    if (fromCurrencyMatch(intent) && toCurrencyMatch(intent)) {
        copy(
            vm = vm.copy(
                from = Value(
                    cryptoValue = intent.quote.from.cryptoValue,
                    cryptoMode = mode(lastUserInputField, FieldUpdateIntent.Field.FROM_CRYPTO),
                    fiatValue = intent.quote.from.fiatValue,
                    fiatMode = mode(lastUserInputField, FieldUpdateIntent.Field.FROM_FIAT)
                ),
                to = Value(
                    cryptoValue = intent.quote.to.cryptoValue,
                    cryptoMode = mode(lastUserInputField, FieldUpdateIntent.Field.TO_CRYPTO),
                    fiatValue = intent.quote.to.fiatValue,
                    fiatMode = mode(lastUserInputField, FieldUpdateIntent.Field.TO_FIAT)
                )
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
            from = vm.to.cryptoValue.currency,
            to = vm.from.cryptoValue.currency
        )
    )

private fun InnerState.map(intent: FiatExchangeRateUpdateIntent): InnerState {
    if (intent.cryptoCurrency == toCrypto && intent.fiatCode == vm.to.fiatValue.currencyCode) {
        return copy(
            toFiatRate = intent.exchangeRate
        ).run { copy(vm = makeVm()) }
    }

    if (intent.cryptoCurrency == fromCrypto && intent.fiatCode == vm.from.fiatValue.currencyCode) {
        return copy(
            fromFiatRate = intent.exchangeRate
        ).run { copy(vm = makeVm()) }
    }

    return this
}

private fun InnerState.map(rateUpdateIntent: CoinExchangeRateUpdateIntent): InnerState {
    if (rateUpdateIntent.from == fromCrypto &&
        rateUpdateIntent.to == toCrypto
    ) {
        return copy(
            fromToCryptoRate = rateUpdateIntent.exchangeRate
        ).run { copy(vm = makeVm()) }
    }

    if (rateUpdateIntent.from == toCrypto &&
        rateUpdateIntent.to == fromCrypto
    ) {
        return copy(
            toFromCryptoRateForSwaps = rateUpdateIntent.exchangeRate
        ).run { copy(vm = makeVm()) }
    }
    return this
}

private fun InnerState.map(intent: FieldUpdateIntent): InnerState {
    return copy(
        vm = when (intent.field) {
            FieldUpdateIntent.Field.FROM_CRYPTO -> {
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
            FieldUpdateIntent.Field.TO_CRYPTO -> {
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
            FieldUpdateIntent.Field.FROM_FIAT -> vm.copy(
                from = vm.from.copy(
                    fiatValue = FiatValue.fromMajor(
                        vm.from.fiatValue.currencyCode,
                        intent.userValue
                    )
                )
            )
            FieldUpdateIntent.Field.TO_FIAT -> vm.copy(
                to = vm.to.copy(
                    fiatValue = FiatValue.fromMajor(
                        vm.to.fiatValue.currencyCode,
                        intent.userValue
                    )
                )
            )
        }
    ).run {
        copy(vm = makeVm(intent.field))
    }
}

private fun InnerState.makeVm(intentField: FieldUpdateIntent.Field? = null): ExchangeViewModel {
    var fromCrypto: CryptoValue? = null
    var toCrypto: CryptoValue? = null

    var fromFiat: FiatValue? = null
    var toFiat: FiatValue? = null

    val field = intentField ?: this.lastUserInputField

    when (field) {
        com.blockchain.morph.exchange.mvi.FieldUpdateIntent.Field.FROM_CRYPTO -> {
            fromCrypto = vm.from.cryptoValue
            toCrypto = fromCrypto * fromToCryptoRate

            fromFiat = fromCrypto * fromFiatRate
            toFiat = toCrypto * toFiatRate
        }

        com.blockchain.morph.exchange.mvi.FieldUpdateIntent.Field.TO_CRYPTO -> {
            toCrypto = vm.to.cryptoValue
            fromCrypto = toCrypto / fromToCryptoRate

            fromFiat = fromCrypto * fromFiatRate
            toFiat = toCrypto * toFiatRate
        }

        com.blockchain.morph.exchange.mvi.FieldUpdateIntent.Field.FROM_FIAT -> {
            fromFiat = vm.from.fiatValue

            fromCrypto = fromFiat / fromFiatRate
            toCrypto = fromCrypto * fromToCryptoRate

            toFiat = toCrypto * toFiatRate
        }

        com.blockchain.morph.exchange.mvi.FieldUpdateIntent.Field.TO_FIAT -> {
            toFiat = vm.to.fiatValue

            toCrypto = toFiat / toFiatRate
            fromCrypto = toCrypto / fromToCryptoRate

            fromFiat = fromCrypto * fromFiatRate
        }
    }

    return ExchangeViewModel(
        from = Value(
            cryptoValue = fromCrypto ?: CryptoValue.zero(vm.from.cryptoValue.currency),
            cryptoMode = mode(field, FieldUpdateIntent.Field.FROM_CRYPTO, fromCrypto),
            fiatValue = fromFiat ?: FiatValue.fromMajor(vm.from.fiatValue.currencyCode, BigDecimal.ZERO),
            fiatMode = mode(field, FieldUpdateIntent.Field.FROM_FIAT, fromFiat)
        ),
        to = Value(
            cryptoValue = toCrypto ?: CryptoValue.zero(vm.to.cryptoValue.currency),
            cryptoMode = mode(field, FieldUpdateIntent.Field.TO_CRYPTO, toCrypto),
            fiatValue = toFiat ?: FiatValue.fromMajor(vm.to.fiatValue.currencyCode, BigDecimal.ZERO),
            fiatMode = mode(field, FieldUpdateIntent.Field.TO_FIAT, toFiat)
        )
    )
}

private fun mode(
    fieldEntered: FieldUpdateIntent.Field,
    field: FieldUpdateIntent.Field,
    value: Any?
): Value.Mode {
    return when {
        fieldEntered == field -> Value.Mode.UserEntered
        value != null -> Value.Mode.UpToDate
        else -> Value.Mode.OutOfDate
    }
}

private fun mode(
    fieldEntered: FieldUpdateIntent.Field,
    field: FieldUpdateIntent.Field
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
    val fromCrypto: CryptoCurrency = vm.from.cryptoValue.currency
    val toCrypto: CryptoCurrency = vm.to.cryptoValue.currency

    val lastUserInputField: FieldUpdateIntent.Field
        get() = when {
            vm.to.cryptoMode == Value.Mode.UserEntered -> FieldUpdateIntent.Field.TO_CRYPTO
            vm.to.fiatMode == Value.Mode.UserEntered -> FieldUpdateIntent.Field.TO_FIAT
            vm.from.fiatMode == Value.Mode.UserEntered -> FieldUpdateIntent.Field.FROM_FIAT
            else -> FieldUpdateIntent.Field.FROM_CRYPTO
        }
}

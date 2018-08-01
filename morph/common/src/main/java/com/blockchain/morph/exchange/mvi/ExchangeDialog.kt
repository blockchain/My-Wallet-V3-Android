package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
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
            }
        }.map { it.vm }
}

private fun InnerState.mapSwap() =
    copy(
        fromFiatRate = toFiatRate,
        toFiatRate = fromFiatRate,
        toFromCryptoRate = fromToCryptoRate,
        fromToCryptoRate = toFromCryptoRate,
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
            toFromCryptoRate = rateUpdateIntent.exchangeRate
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
                    intent.userText.toBigDecimal()
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
                    intent.userText.toBigDecimal()
                )
                vm.copy(
                    to = vm.to.copy(
                        cryptoValue = newToValue
                    )
                )
            }
            FieldUpdateIntent.Field.FROM_FIAT -> vm.copy(
                from = vm.from.copy(
                    fiatValue = FiatValue(
                        vm.from.fiatValue.currencyCode,
                        intent.userText.toBigDecimal()
                    )
                )
            )
            FieldUpdateIntent.Field.TO_FIAT -> vm.copy(
                to = vm.to.copy(
                    fiatValue = FiatValue(
                        vm.to.fiatValue.currencyCode,
                        intent.userText.toBigDecimal()
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

            fromFiat = fromFiatRate?.applyRate(fromCrypto)
            toFiat = toFiatRate?.applyRate(toCrypto)
        }

        com.blockchain.morph.exchange.mvi.FieldUpdateIntent.Field.TO_CRYPTO -> {
            toCrypto = vm.to.cryptoValue
            fromCrypto = toCrypto * toFromCryptoRate

            fromFiat = fromFiatRate?.applyRate(fromCrypto)
            toFiat = toFiatRate?.applyRate(toCrypto)
        }

        com.blockchain.morph.exchange.mvi.FieldUpdateIntent.Field.FROM_FIAT -> {
            fromFiat = vm.from.fiatValue

            fromCrypto = fromFiatRate?.inverse()?.applyRate(fromFiat)
            toCrypto = fromToCryptoRate?.applyRate(fromCrypto)

            toFiat = toFiatRate?.applyRate(toCrypto)
        }

        com.blockchain.morph.exchange.mvi.FieldUpdateIntent.Field.TO_FIAT -> {
            toFiat = vm.to.fiatValue

            toCrypto = toFiatRate?.inverse()?.applyRate(toFiat)

            fromCrypto = toFromCryptoRate?.applyRate(toCrypto)

            fromFiat = fromFiatRate?.applyRate(fromCrypto)
        }
    }

    return ExchangeViewModel(
        from = Value(
            cryptoValue = fromCrypto ?: CryptoValue.fromMajor(vm.from.cryptoValue.currency, BigDecimal.ZERO),
            cryptoMode = mode(field, FieldUpdateIntent.Field.FROM_CRYPTO, fromCrypto),
            fiatValue = fromFiat ?: FiatValue(vm.from.fiatValue.currencyCode, BigDecimal.ZERO),
            fiatMode = mode(field, FieldUpdateIntent.Field.FROM_FIAT, fromFiat)
        ),
        to = Value(
            cryptoValue = toCrypto ?: CryptoValue.fromMajor(vm.to.cryptoValue.currency, BigDecimal.ZERO),
            cryptoMode = mode(field, FieldUpdateIntent.Field.TO_CRYPTO, toCrypto),
            fiatValue = toFiat ?: FiatValue(vm.to.fiatValue.currencyCode, BigDecimal.ZERO),
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

private data class InnerState(
    val vm: ExchangeViewModel,

    val fromToCryptoRate: ExchangeRate.CryptoToCrypto? = null,

    val toFromCryptoRate: ExchangeRate.CryptoToCrypto? = null,

    val fromFiatRate: ExchangeRate.CryptoToFiat? = null,

    val toFiatRate: ExchangeRate.CryptoToFiat? = null
) {
    val fromCrypto: CryptoCurrency = vm.from.cryptoValue.currency
    val toCrypto: CryptoCurrency = vm.to.cryptoValue.currency

    val lastUserInputField: FieldUpdateIntent.Field
        get() {
            if (vm.to.cryptoMode == Value.Mode.UserEntered) return FieldUpdateIntent.Field.TO_CRYPTO
            if (vm.to.fiatMode == Value.Mode.UserEntered) return FieldUpdateIntent.Field.TO_FIAT
            if (vm.from.fiatMode == Value.Mode.UserEntered) return FieldUpdateIntent.Field.FROM_FIAT
            return FieldUpdateIntent.Field.FROM_CRYPTO
        }
}

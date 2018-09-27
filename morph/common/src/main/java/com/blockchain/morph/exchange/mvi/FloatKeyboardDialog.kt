package com.blockchain.morph.exchange.mvi

import io.reactivex.Observable
import io.reactivex.rxkotlin.toObservable
import java.math.BigDecimal
import java.math.BigInteger

sealed class FloatKeyboardIntent {
    class NumericKey(val key: Int) : FloatKeyboardIntent()
    class Period : FloatKeyboardIntent()
    class Backspace : FloatKeyboardIntent()
    class Clear : FloatKeyboardIntent()
    class SetMaximums(val maximums: Maximums) : FloatKeyboardIntent()
    class SetMaxDp(val maxDp: Int) : FloatKeyboardIntent()
    data class SetValue(val maxDp: Int, val value: BigDecimal) : FloatKeyboardIntent()
}

class FloatKeyboardDialog(intents: Observable<FloatKeyboardIntent>) {
    val states: Observable<FloatEntryViewState> =
        intents.scan(initialState) { previous, intent ->
            when (intent) {
                is FloatKeyboardIntent.Backspace -> previous.previous ?: previous.copy(shake = true)
                is FloatKeyboardIntent.Clear -> initialState.copy(
                    maxDecimal = previous.maxDecimal,
                    maximums = previous.maximums
                )
                is FloatKeyboardIntent.Period -> mapPeriodPress(previous)
                is FloatKeyboardIntent.NumericKey -> mapKeyPress(previous, intent.key)
                is FloatKeyboardIntent.SetMaxDp -> previous.copy(maxDecimal = intent.maxDp)
                is FloatKeyboardIntent.SetValue -> constructViewStateFromValue(previous, initialState, intent)
                is FloatKeyboardIntent.SetMaximums -> previous.setMaximum(intent)
            }
        }.distinctUntilChanged { a, b -> a === b }
}

private fun FloatEntryViewState.setMaximum(intent: FloatKeyboardIntent.SetMaximums) =
    if (maximums == intent.maximums) this else copy(maximums = intent.maximums)

private fun constructViewStateFromValue(
    previous: FloatEntryViewState,
    initialState: FloatEntryViewState,
    intent: FloatKeyboardIntent.SetValue
): FloatEntryViewState {
    if (previous.userDecimal.compareTo(intent.value) == 0 && previous.maxDecimal == intent.maxDp) return previous

    val intents = listOf(
        FloatKeyboardIntent.SetMaximums(previous.maximums)
    ) + numberToIntents(intent)
    return FloatKeyboardDialog(intents.toObservable())
        .states
        .last(initialState)
        .blockingGet()
        .copy(shake = false)
}

private fun numberToIntents(intent: FloatKeyboardIntent.SetValue): List<FloatKeyboardIntent> {
    val integer = intent.value.toBigInteger()
    val onePlusFraction =
        (BigDecimal.ONE + intent.value - integer.toBigDecimal()).movePointRight(intent.maxDp).toBigInteger()

    val integerIntents = listOf(FloatKeyboardIntent.SetMaxDp(intent.maxDp)) + intToIntents(integer, false)
    val onePlusFractionIntents = intToIntents(onePlusFraction, true)

    return if (onePlusFractionIntents.size > 1) {
        val fractionIntents = onePlusFractionIntents.takeLast(onePlusFractionIntents.size - 1)
        integerIntents + FloatKeyboardIntent.Period() + fractionIntents
    } else {
        integerIntents
    }
}

private fun intToIntents(int: BigInteger, ignoreTrailingZeros: Boolean): List<FloatKeyboardIntent> {
    var int1 = int
    val intents = mutableListOf<FloatKeyboardIntent>()
    while (int1.signum() == 1) {
        val key = int1.remainder(BigInteger.TEN).toInt()
        if (!ignoreTrailingZeros || key > 0 || !intents.isEmpty()) {
            intents.add(FloatKeyboardIntent.NumericKey(key))
        }
        int1 = int1.divide(BigInteger.TEN)
    }
    intents.reverse()
    return intents
}

private fun mapPeriodPress(previous: FloatEntryViewState): FloatEntryViewState {
    return when {
        previous.maxDecimal <= 0 -> previous.copy(shake = true)
        previous.decimalCursor > 1 -> previous.copy(shake = true)
        else -> previous.copy(
            userDecimal = previous
                .userDecimal
                .setScale(previous.maxDecimal),
            decimalCursor = 1,
            shake = false,
            previous = previous
        )
    }
}

private fun mapKeyPress(previous: FloatEntryViewState, key: Int): FloatEntryViewState {
    return when {
        previous.decimalCursor > previous.maxDecimal -> previous.copy(shake = true)
        previous.userDecimal.scale() > 0 -> previous.copy(
            userDecimal = previous
                .userDecimal
                .add(key.toBigDecimal().movePointLeft(previous.decimalCursor)),
            decimalCursor = previous.decimalCursor + 1,
            shake = false,
            previous = previous
        )
        else -> previous.copy(
            userDecimal = previous
                .userDecimal
                .scaleByPowerOfTen(1)
                .add(key.toBigDecimal()),
            shake = false,
            previous = previous
        )
    }.let { new ->
        val integerDigits = new.userDecimal.log10() + 1
        val decimalDigitCount = Math.max(0, new.decimalCursor - 1)
        val allDigits = integerDigits + decimalDigitCount
        if (previous.maximums.maxIntLength in 1..(integerDigits - 1)) {
            previous.shake()
        } else if (previous.maximums.maxDigits in 1..(allDigits - 1)) {
            previous.shake()
        } else if (previous.maximums.maxValue > BigDecimal.ZERO && new.userDecimal > previous.maximums.maxValue) {
            constructViewStateFromValue(
                previous,
                previous,
                FloatKeyboardIntent.SetValue(previous.maxDecimal, previous.maximums.maxValue)
            ).shake()
        } else {
            new
        }
    }
}

private fun FloatEntryViewState.shake() = copy(shake = true)

private fun BigDecimal.log10(): Int {
    return Math.log10(toDouble()).toInt()
}

private val initialState = FloatEntryViewState()

data class FloatEntryViewState(
    val userDecimal: BigDecimal = BigDecimal.ZERO,
    val decimalCursor: Int = 0,
    val maxDecimal: Int = 2,
    val shake: Boolean = false,
    val previous: FloatEntryViewState? = null,
    val maximums: Maximums = Maximums()
)

data class Maximums(
    val maxValue: BigDecimal = BigDecimal.ZERO,
    val maxIntLength: Int = 0,
    val maxDigits: Int = 0
)

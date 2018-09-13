package com.blockchain.morph.exchange.mvi

import io.reactivex.Observable
import java.math.BigDecimal

sealed class FloatKeyboardIntent {
    class NumericKey(val key: Int) : FloatKeyboardIntent()
    class Period : FloatKeyboardIntent()
    class Backspace : FloatKeyboardIntent()
    class Clear : FloatKeyboardIntent()
    class SetMaxDp(val maxDp: Int) : FloatKeyboardIntent()
}

class FloatKeyboardDialog(intents: Observable<FloatKeyboardIntent>) {

    val states: Observable<FloatEntryViewState> =
        intents.scan(initialState) { previous, intent ->
            when (intent) {
                is FloatKeyboardIntent.Backspace -> previous.previous ?: previous.copy(shake = true)
                is FloatKeyboardIntent.Clear -> initialState.copy(maxDecimal = previous.maxDecimal)
                is FloatKeyboardIntent.Period -> mapPeriodPress(previous)
                is FloatKeyboardIntent.NumericKey -> mapKeyPress(previous, intent.key)
                is FloatKeyboardIntent.SetMaxDp -> previous.copy(maxDecimal = intent.maxDp)
            }
        }
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
    }
}

private val initialState = FloatEntryViewState()

data class FloatEntryViewState(
    val userDecimal: BigDecimal = BigDecimal.ZERO,
    val decimalCursor: Int = 0,
    val maxDecimal: Int = 2,
    val shake: Boolean = false,
    val previous: FloatEntryViewState? = null
)

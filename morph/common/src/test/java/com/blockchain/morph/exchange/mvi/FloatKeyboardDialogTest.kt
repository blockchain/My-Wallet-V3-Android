package com.blockchain.morph.exchange.mvi

import io.reactivex.Observable
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigDecimal

class FloatKeyboardDialogTest {

    @Test
    fun `initial state`() {
        lastStateGivenIntents()
            .assertNoShake()
            .userDecimal `should be` BigDecimal.ZERO
    }

    @Test
    fun `press key`() {
        lastStateGivenIntents(userKey('1'))
            .assertNoShake()
            .userDecimal `should equal` BigDecimal.ONE
    }

    @Test
    fun `press two keys`() {
        lastStateGivenIntents(*keys("12"))
            .assertNoShake()
            .userDecimal `should equal` 12.toBigDecimal()
    }

    @Test
    fun `press two keys and period`() {
        lastStateGivenIntents(*keys("12."))
            .assertNoShake()
            .userDecimal `should equal` "12.00".toBigDecimal()
    }

    @Test
    fun `press two keys and period then one key`() {
        lastStateGivenIntents(*keys("12.3"))
            .assertNoShake()
            .userDecimal `should equal` "12.30".toBigDecimal()
    }

    @Test
    fun `press two keys and period then two keys`() {
        lastStateGivenIntents(*keys("12.34"))
            .assertNoShake()
            .userDecimal `should equal` "12.34".toBigDecimal()
    }

    @Test
    fun `press two keys and period then three keys`() {
        lastStateGivenIntents(*keys("12.345"))
            .assertShake()
            .userDecimal `should equal` "12.34".toBigDecimal()
    }

    @Test
    fun `backspace a decimal`() {
        lastStateGivenIntents(*keys("12.34<"))
            .assertNoShake()
            .userDecimal `should equal` "12.30".toBigDecimal()
    }

    @Test
    fun `many backspaces`() {
        lastStateGivenIntents(*keys("<12.34<<<99"))
            .assertNoShake()
            .userDecimal `should equal` "1299".toBigDecimal()
    }

    @Test
    fun `backspace from empty`() {
        lastStateGivenIntents(userKey('<'))
            .assertShake()
    }

    @Test
    fun `multiple periods in a row - no shake`() {
        lastStateGivenIntents(*keys("1.."))
            .assertNoShake()
            .userDecimal `should equal` "1.00".toBigDecimal()
    }

    @Test
    fun `multiple periods not in a row - shake`() {
        lastStateGivenIntents(*keys("1.2."))
            .assertShake()
            .userDecimal `should equal` "1.20".toBigDecimal()
    }

    @Test
    fun `multiple periods in a row`() {
        lastStateGivenIntents(*keys("12..34"))
            .assertNoShake()
            .userDecimal `should equal` "12.34".toBigDecimal()
    }

    @Test
    fun `increase max dp`() {
        lastStateGivenIntents(setMaxDp(3), *keys("12."))
            .assertNoShake()
            .userDecimal `should equal` "12.000".toBigDecimal()
    }

    @Test
    fun `increase max dp and press 3 keys`() {
        lastStateGivenIntents(setMaxDp(3), *keys("12.345"))
            .assertNoShake()
            .userDecimal `should equal` "12.345".toBigDecimal()
    }

    @Test
    fun `increase max dp and press extra key`() {
        lastStateGivenIntents(setMaxDp(3), *keys("12.3456"))
            .assertShake()
            .userDecimal `should equal` "12.345".toBigDecimal()
    }

    @Test
    fun `decrease max dp`() {
        lastStateGivenIntents(setMaxDp(0), *keys("12."))
            .assertShake()
            .userDecimal `should equal` "12".toBigDecimal()
    }

    @Test
    fun `clear clears all`() {
        lastStateGivenIntents(*keys("12.30-"))
            .assertNoShake()
            .apply {
                userDecimal `should equal` "0".toBigDecimal()
                previous `should be` null
            }
    }

    @Test
    fun `clear clears all and can enter a new value`() {
        lastStateGivenIntents(*keys("12.34-45.65"))
            .assertNoShake()
            .userDecimal `should equal` "45.65".toBigDecimal()
    }

    @Test
    fun `after a clear the dp is still set`() {
        lastStateGivenIntents(setMaxDp(3), FloatKeyboardIntent.Clear())
            .assertNoShake()
            .maxDecimal `should equal` 3
    }
}

private fun setMaxDp(maxDp: Int): FloatKeyboardIntent = FloatKeyboardIntent.SetMaxDp(maxDp)

private fun keys(s: String) = s.map(::userKey).toTypedArray()

private fun lastStateGivenIntents(vararg intents: FloatKeyboardIntent): FloatEntryViewState =
    FloatKeyboardDialog(Observable.fromArray(*intents))
        .states
        .test()
        .values()
        .last()

private fun userKey(key: Char) =
    when (key) {
        '<' -> FloatKeyboardIntent.Backspace()
        '-' -> FloatKeyboardIntent.Clear()
        '.' -> FloatKeyboardIntent.Period()
        else -> FloatKeyboardIntent.NumericKey(key - '0')
    }

private fun FloatEntryViewState.assertNoShake(): FloatEntryViewState {
    shake `should be` false
    return this
}

private fun FloatEntryViewState.assertShake(): FloatEntryViewState {
    shake `should be` true
    return this
}

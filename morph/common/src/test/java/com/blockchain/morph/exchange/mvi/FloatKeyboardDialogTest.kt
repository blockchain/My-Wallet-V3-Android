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
    fun `backspace from empty twice`() {
        FloatKeyboardDialog(Observable.fromArray(*keys("<<")))
            .states
            .test()
            .values()
            .apply { size `should be` 3 }
            .apply { get(1).assertShake() }
            .apply { get(2).assertShake() }
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

    @Test
    fun `can set the value`() {
        lastStateGivenIntents(setValue(4, 3.1234))
            .assertNoShake()
            .userDecimal `should equal` 3.1234.toBigDecimal()
    }

    @Test
    fun `can set the value and then go back`() {
        lastStateGivenIntents(setValue(4, 3.1234), *keys("<"))
            .assertNoShake()
            .userDecimal `should equal` "3.1230".toBigDecimal()
    }

    @Test
    fun `can set the value and then go back before the decimal point`() {
        lastStateGivenIntents(setValue(3, 13.123), *keys("<<<<"))
            .assertNoShake()
            .userDecimal `should equal` "13".toBigDecimal()
    }

    @Test
    fun `can set the value and then go back before all the way and get shake`() {
        lastStateGivenIntents(*keys("123"), setValue(3, 13.123), *keys("<<<<<<<"))
            .assertShake()
            .userDecimal `should equal` "0".toBigDecimal()
    }

    @Test
    fun `only emits one state on setValue`() {
        FloatKeyboardDialog(Observable.just(setValue(3, 13.123)))
            .states
            .test()
            .values()
            .apply { size `should be` 2 }
            .last()
            .userDecimal `should equal` "13.123".toBigDecimal()
    }

    @Test
    fun `does not emit an additional state if it matches the current one`() {
        FloatKeyboardDialog(
            Observable.just(
                setValue(3, 13.123),
                setValue(3, 13.123)
            )
        )
            .states
            .test()
            .values()
            .apply { size `should be` 2 }
            .last()
            .userDecimal `should equal` "13.123".toBigDecimal()
    }

    @Test
    fun `does not emit an additional state if it matches the current one, by value`() {
        FloatKeyboardDialog(
            Observable.just(
                setValue(3, 13.100),
                setValue(3, 13.1)
            )
        )
            .states
            .test()
            .values()
            .apply { size `should be` 2 }
            .last()
            .userDecimal `should equal` "13.100".toBigDecimal()
    }

    @Test
    fun `does not emits an additional state if it matches the current one`() {
        FloatKeyboardDialog(
            Observable.just(
                setValue(3, 13.123),
                setValue(3, 13.123)
            )
        )
            .states
            .test()
            .values()
            .apply { size `should be` 2 }
            .last()
            .userDecimal `should equal` "13.123".toBigDecimal()
    }

    @Test
    fun `does not emit an additional state if the previous one was an error but the setValue is the same`() {
        FloatKeyboardDialog(
            Observable.just(
                setValue(3, 13.123),
                userKey('1'),
                setValue(3, 13.123)
            )
        )
            .states
            .test()
            .values()
            .apply { size `should be` 3 }
            .last()
            .userDecimal `should equal` "13.123".toBigDecimal()
    }

    @Test
    fun `set value to 0 with 8 dp`() {
        lastStateGivenIntents(setValue(8, BigDecimal.ZERO.setScale(8)))
            .apply {
                userDecimal `should equal` BigDecimal.ZERO
                decimalCursor `should be` 0
            }
    }

    @Test
    fun `set value to non-0 with 8 dp`() {
        lastStateGivenIntents(setValue(8, "0.1234000".toBigDecimal()))
            .apply {
                userDecimal `should equal` "0.12340000".toBigDecimal()
                decimalCursor `should be` 5
            }
    }

    @Test
    fun `set value to non-0 with 8 dp with zeros`() {
        lastStateGivenIntents(setValue(8, "0.10101000".toBigDecimal()))
            .apply {
                userDecimal `should equal` "0.10101000".toBigDecimal()
                decimalCursor `should be` 6
            }
    }

    @Test
    fun `set value with leading fractional zeros`() {
        lastStateGivenIntents(setValue(8, "0.00123".toBigDecimal()))
            .apply {
                userDecimal `should equal` "0.00123000".toBigDecimal()
                decimalCursor `should be` 6
            }
    }

    @Test
    fun `set value to integer with trailing values`() {
        lastStateGivenIntents(setValue(3, "10".toBigDecimal()))
            .apply {
                userDecimal `should equal` "10".toBigDecimal()
                decimalCursor `should be` 0
            }
    }

    @Test
    fun `maximum integer digits`() {
        lastStateGivenIntents(setMaximums(Maximums(maxIntLength = 4)), *keys("12345"))
            .assertShake()
            .apply {
                userDecimal `should equal` "1234".toBigDecimal()
            }
    }

    @Test
    fun `maximum absolute value`() {
        lastStateGivenIntents(setMaximums(Maximums(maxValue = 99.9.toBigDecimal())), *keys("99.91"))
            .assertShake()
            .apply {
                userDecimal `should equal` "99.90".toBigDecimal()
            }
    }

    @Test
    fun `maximum absolute value - over`() {
        lastStateGivenIntents(setMaximums(Maximums(maxValue = 99.9.toBigDecimal())), *keys("100"))
            .assertShake()
            .apply {
                userDecimal `should equal` "99.90".toBigDecimal()
            }
    }

    @Test
    fun `maximum absolute value - over, then back`() {
        lastStateGivenIntents(setMaximums(Maximums(maxValue = 99.9.toBigDecimal())), *keys("100<"))
            .assertNoShake()
            .apply {
                userDecimal `should equal` "99.00".toBigDecimal()
            }
    }

    @Test
    fun `maximum absolute value - over, then back all the way`() {
        lastStateGivenIntents(setMaximums(Maximums(maxValue = 99.9.toBigDecimal())), *keys("100<<<<<"))
            .assertShake()
            .apply {
                userDecimal `should equal` "0".toBigDecimal()
            }
    }

    @Test
    fun `maximum digits`() {
        lastStateGivenIntents(setMaximums(Maximums(maxDigits = 4)), *keys("112.34"))
            .assertShake()
            .apply {
                userDecimal `should equal` "112.30".toBigDecimal()
            }
    }

    @Test
    fun `maximum digits all ints`() {
        lastStateGivenIntents(setMaximums(Maximums(maxDigits = 4)), *keys("11234"))
            .assertShake()
            .apply {
                userDecimal `should equal` "1123".toBigDecimal()
            }
    }

    @Test
    fun `applying same maximums yields same state`() {
        FloatKeyboardDialog(
            Observable.just(
                setMaximums(Maximums(maxDigits = 8)),
                setMaximums(Maximums(maxDigits = 8))
            )
        )
            .states
            .test()
            .values()
            .size `should equal` 2
    }

    @Test
    fun `setValue keeps maximum`() {
        lastStateGivenIntents(setMaximums(Maximums(maxDigits = 4)), setValue(3, "11.234".toBigDecimal()))
            .apply {
                userDecimal `should equal` "11.230".toBigDecimal()
                maximums.maxDigits `should equal` 4
            }
    }

    @Test
    fun `clear keeps maximum`() {
        lastStateGivenIntents(
            setMaximums(Maximums(maxDigits = 5)),
            setValue(3, "11.234".toBigDecimal()),
            FloatKeyboardIntent.Clear()
        ).apply {
            userDecimal `should equal` "0".toBigDecimal()
            maximums.maxDigits `should equal` 5
        }
    }

    @Test
    fun `set value does not emit an error`() {
        lastStateGivenIntents(setValue(2, 13.123))
            .assertNoShake()
    }
}

private fun setValue(dp: Int, d: Double): FloatKeyboardIntent = setValue(dp, d.toBigDecimal())

private fun setValue(dp: Int, bd: BigDecimal): FloatKeyboardIntent = FloatKeyboardIntent.SetValue(dp, bd)

private fun setMaximums(maximums: Maximums): FloatKeyboardIntent = FloatKeyboardIntent.SetMaximums(maximums)

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

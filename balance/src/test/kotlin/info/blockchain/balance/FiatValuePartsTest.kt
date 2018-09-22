package info.blockchain.balance

import org.amshove.kluent.`should equal`
import org.junit.Test
import java.util.Locale

class FiatValuePartsTest {

    @Test
    fun `extract GBP parts in UK`() {
        1.2.gbp()
            .toStringParts(Locale.UK).apply {
                symbol `should equal` "£"
                major `should equal` "1"
                minor `should equal` "20"
                majorAndMinor `should equal` "1.20"
            }
    }

    @Test
    fun `extract USD parts in US`() {
        9.89.usd()
            .toStringParts(Locale.US).apply {
                symbol `should equal` "$"
                major `should equal` "9"
                minor `should equal` "89"
                majorAndMinor `should equal` "9.89"
            }
    }

    @Test
    fun `extract USD parts in UK`() {
        5.86.usd()
            .toStringParts(Locale.UK).apply {
                symbol `should equal` "USD"
                major `should equal` "5"
                minor `should equal` "86"
                majorAndMinor `should equal` "5.86"
            }
    }

    @Test
    fun `extract JPY parts in Japan`() {
        512.jpy()
            .toStringParts(Locale.JAPAN).apply {
                symbol `should equal` "￥"
                major `should equal` "512"
                minor `should equal` ""
                majorAndMinor `should equal` "512"
            }
    }

    @Test
    fun `extract USD parts in France`() {
        1512.99.usd()
            .toStringParts(Locale.FRANCE).apply {
                symbol `should equal` "USD"
                major `should equal` "1 512"
                minor `should equal` "99"
                majorAndMinor `should equal` "1 512,99"
            }
    }

    @Test
    fun `extract Euro parts in Italy`() {
        2356.32.eur()
            .toStringParts(Locale.FRANCE).apply {
                symbol `should equal` "€"
                major `should equal` "2 356"
                minor `should equal` "32"
                majorAndMinor `should equal` "2 356,32"
            }
    }

    @Test
    fun `extract Euro parts in Germany`() {
        4567.98.eur()
            .toStringParts(Locale.GERMANY).apply {
                symbol `should equal` "€"
                major `should equal` "4.567"
                minor `should equal` "98"
                majorAndMinor `should equal` "4.567,98"
            }
    }
}

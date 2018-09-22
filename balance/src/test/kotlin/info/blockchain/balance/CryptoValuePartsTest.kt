package info.blockchain.balance

import org.amshove.kluent.`should equal`
import org.junit.Test
import java.util.Locale

class CryptoValuePartsTest {

    @Test
    fun `extract BTC parts in UK`() {
        1.2.bitcoin()
            .toParts(Locale.UK).apply {
                symbol `should equal` "BTC"
                major `should equal` "1"
                minor `should equal` "2"
            }
    }

    @Test
    fun `extract ETH parts in US`() {
        9.89.ether()
            .toParts(Locale.US).apply {
                symbol `should equal` "ETH"
                major `should equal` "9"
                minor `should equal` "89"
            }
    }

    @Test
    fun `extract max DP ETHER parts in UK`() {
        5.12345678.ether()
            .toParts(Locale.UK).apply {
                symbol `should equal` "ETH"
                major `should equal` "5"
                minor `should equal` "12345678"
            }
    }

    @Test
    fun `extract parts from large number in UK`() {
        5345678.ether()
            .toParts(Locale.UK).apply {
                symbol `should equal` "ETH"
                major `should equal` "5,345,678"
                minor `should equal` "0"
            }
    }

    @Test
    fun `extract parts from large number in France`() {
        5345678.987.ether()
            .toParts(Locale.FRANCE).apply {
                symbol `should equal` "ETH"
                major `should equal` "5 345 678"
                minor `should equal` "987"
            }
    }

    @Test
    fun `extract parts from large number in Italy`() {
        9345678.987.ether()
            .toParts(Locale.ITALY).apply {
                symbol `should equal` "ETH"
                major `should equal` "9.345.678"
                minor `should equal` "987"
            }
    }
}

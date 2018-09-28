package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.cad
import com.blockchain.testutils.ether
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should equal`
import org.junit.Test

class ClearQuoteTest {

    @Test
    fun `can clear and keep the fiat base entry`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.BASE_FIAT),
            SimpleFieldUpdateIntent(123.toBigDecimal()),
            ClearQuoteIntent
        ) {
            fromFiat `should equal` 123.cad()
            toFiat `should equal` 0.cad()
            fromCrypto `should equal` 0.bitcoin()
            toCrypto `should equal` 0.ether()
        }
    }

    @Test
    fun `can clear and keep the crypto base entry`() {
        given(
            initial("CAD", CryptoCurrency.BCH to CryptoCurrency.BTC).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.BASE_CRYPTO),
            SimpleFieldUpdateIntent(123.toBigDecimal()),
            ClearQuoteIntent
        ) {
            fromFiat `should equal` 0.cad()
            toFiat `should equal` 0.cad()
            fromCrypto `should equal` 123.bitcoinCash()
            toCrypto `should equal` 0.bitcoin()
        }
    }

    @Test
    fun `can clear and keep the fiat counter entry`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.COUNTER_FIAT),
            SimpleFieldUpdateIntent(123.toBigDecimal()),
            ClearQuoteIntent
        ) {
            fromFiat `should equal` 0.cad()
            toFiat `should equal` 123.cad()
            fromCrypto `should equal` 0.bitcoin()
            toCrypto `should equal` 0.ether()
        }
    }

    @Test
    fun `can clear and keep the crypto counter entry`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.COUNTER_FIAT),
            SimpleFieldUpdateIntent(123.toBigDecimal()),
            ClearQuoteIntent
        ) {
            fromFiat `should equal` 0.cad()
            toFiat `should equal` 123.cad()
            fromCrypto `should equal` 0.bitcoin()
            toCrypto `should equal` 0.ether()
        }
    }

    @Test
    fun `can clear the latest quote`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.BASE_CRYPTO),
            SimpleFieldUpdateIntent(123.toBigDecimal()),
            Quote(
                fix = Fix.BASE_CRYPTO,
                from = 123.bitcoin() `equivalent to` 99.12.cad(),
                to = 25.ether() `equivalent to` 95.32.cad()
            ).toIntent(),
            ClearQuoteIntent
        ) {
            latestQuote `should equal` null
        }
    }
}

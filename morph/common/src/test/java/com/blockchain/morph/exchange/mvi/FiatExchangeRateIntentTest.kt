package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import org.amshove.kluent.`should be`
import org.junit.Test

class FiatExchangeRateIntentTest {

    @Test
    fun `can set the fiat rate`() {
        val c2fRate = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 1000.toBigDecimal())
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            FiatExchangeRateIntent(c2fRate)
        ) {
            this.c2fRate `should be` c2fRate
        }
    }

    @Test
    fun `ignores the fiat rate if it doesn't match by fiat`() {
        val c2fRate = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 1000.toBigDecimal())
        given(
            initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            FiatExchangeRateIntent(c2fRate)
        ) {
            this.c2fRate `should be` null
        }
    }

    @Test
    fun `ignores the fiat rate if it doesn't match by crypto`() {
        val c2fRate = ExchangeRate.CryptoToFiat(CryptoCurrency.BCH, "USD", 1000.toBigDecimal())
        given(
            initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            FiatExchangeRateIntent(c2fRate)
        ) {
            this.c2fRate `should be` null
        }
    }

    @Test
    fun `keeps old rate if it doesn't match by fiat`() {
        val c2fRate = ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 1000.toBigDecimal())
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            FiatExchangeRateIntent(c2fRate),
            FiatExchangeRateIntent(ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "USD", 1000.toBigDecimal()))
        ) {
            this.c2fRate `should be` c2fRate
        }
    }
}

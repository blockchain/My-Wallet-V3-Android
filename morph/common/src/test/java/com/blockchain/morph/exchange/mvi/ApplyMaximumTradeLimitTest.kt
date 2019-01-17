package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.cad
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class ApplyMaximumTradeLimitTest {

    @Test
    fun `can apply the maximum trade limit`() {
        given(
            initial("CAD")
        ).onLastStateAfter(
            SetTradeLimits(10.cad(), 200.cad()),
            ApplyMaximumLimit()
        ) {
            fix `should be` Fix.BASE_FIAT
            fromFiat `should equal` 200.cad()
        }
    }

    @Test
    fun `can't apply the maximum trade limit if it's not set`() {
        given(
            initial("CAD")
        ).onLastStateAfter(
            SetFixIntent(Fix.BASE_CRYPTO),
            ApplyMaximumLimit()
        ) {
            fix `should be` Fix.BASE_CRYPTO
            fromFiat `should equal` 0.cad()
        }
    }

    @Test
    fun `before apply maximum given a spendable balance under the fiat maximum`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SetTradeLimits(0.cad(), 1000.cad()),
            FiatExchangeRateIntent(ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 200.toBigDecimal())),
            SpendableValueIntent(0.01.bitcoin())
        ) {
            maxTrade `should equal` 0.01.bitcoin()
        }
    }

    @Test
    fun `apply maximum given a spendable balance under the fiat maximum`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SetTradeLimits(0.cad(), 1000.cad()),
            FiatExchangeRateIntent(ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 200.toBigDecimal())),
            SpendableValueIntent(0.01.bitcoin()),
            ApplyMaximumLimit()
        ) {
            lastUserValue `should equal` 0.01.bitcoin()
            fix `should be` Fix.BASE_CRYPTO
            fromCrypto `should equal` 0.01.bitcoin()
            maxTrade `should equal` 0.01.bitcoin()
        }
    }

    @Test
    fun `before apply maximum given a spendable balance over the fiat maximum`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SetTradeLimits(0.cad(), 1000.cad()),
            FiatExchangeRateIntent(ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 1001.toBigDecimal())),
            SpendableValueIntent(1.bitcoin())
        ) {
            maxTrade `should equal` 1000.cad()
        }
    }

    @Test
    fun `before apply maximum given a spendable balance over the fiat tier maximum`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SetTierLimit(1000.cad()),
            FiatExchangeRateIntent(ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 1001.toBigDecimal())),
            SpendableValueIntent(1.bitcoin())
        ) {
            maxTrade `should equal` 1000.cad()
        }
    }

    @Test
    fun `applies ths smallest of tier and max trade`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SetTierLimit(1000.cad()),
            SetTradeLimits(0.cad(), 900.cad()),
            FiatExchangeRateIntent(ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 1001.toBigDecimal())),
            SpendableValueIntent(1.bitcoin())
        ) {
            maxTrade `should equal` 900.cad()
        }
    }

    @Test
    fun `apply maximum given a spendable balance over the fiat maximum`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SetTradeLimits(0.cad(), 1000.cad()),
            FiatExchangeRateIntent(ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 1001.toBigDecimal())),
            SpendableValueIntent(1.bitcoin()),
            ApplyMaximumLimit()
        ) {
            lastUserValue `should equal` 1000.cad()
            fix `should be` Fix.BASE_FIAT
            fromFiat `should equal` 1000.cad()
        }
    }

    @Test
    fun `apply maximum given a spendable balance exactly equal to the fiat maximum`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SetTradeLimits(0.cad(), 1000.cad()),
            FiatExchangeRateIntent(ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 1000.toBigDecimal())),
            SpendableValueIntent(1.bitcoin()),
            ApplyMaximumLimit()
        ) {
            lastUserValue `should equal` 1.bitcoin()
            fix `should be` Fix.BASE_CRYPTO
            fromCrypto `should equal` 1.bitcoin()
        }
    }

    @Test
    fun `before maximum given a spendable balance exactly equal to the fiat maximum`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SetTradeLimits(0.cad(), 1000.cad()),
            FiatExchangeRateIntent(ExchangeRate.CryptoToFiat(CryptoCurrency.BTC, "CAD", 1000.toBigDecimal())),
            SpendableValueIntent(1.bitcoin())
        ) {
            maxTrade `should equal` 1.bitcoin()
        }
    }

    @Test
    fun `if the account changes, we might have an old rate on there, make sure it doesn't come out`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SpendableValueIntent(1.bitcoin()),
            ChangeCryptoFromAccount(fakeAccountReference(CryptoCurrency.ETHER)),
            SetTradeLimits(0.cad(), 1000.cad()),
            FiatExchangeRateIntent(ExchangeRate.CryptoToFiat(CryptoCurrency.ETHER, "CAD", 1001.toBigDecimal())),
            ApplyMaximumLimit()
        ) {
            lastUserValue `should equal` 1000.cad()
            fix `should be` Fix.BASE_FIAT
            fromFiat `should equal` 1000.cad()
        }
    }
}

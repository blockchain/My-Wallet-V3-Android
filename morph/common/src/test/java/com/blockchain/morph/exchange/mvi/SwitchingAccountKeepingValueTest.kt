package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.cad
import com.blockchain.testutils.ether
import com.blockchain.testutils.usd
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class SwitchingAccountKeepingValueTest {

    @Test
    fun `in base fiat mode, when switching from account, keep the value`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.BASE_FIAT),
            SimpleFieldUpdateIntent(55.toBigDecimal()),
            ChangeCryptoFromAccount(fakeAccountReference(CryptoCurrency.ETHER))
        ) {
            fromCrypto `should equal` 0.ether()
            fromFiat `should equal` 55.cad()
            fix `should be` Fix.BASE_FIAT
        }
    }

    @Test
    fun `in counter fiat mode, when switching from account, keep the value`() {
        given(
            initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.COUNTER_FIAT),
            SimpleFieldUpdateIntent(99.toBigDecimal()),
            ChangeCryptoFromAccount(fakeAccountReference(CryptoCurrency.ETHER))
        ) {
            toFiat `should equal` 99.usd()
            fix `should be` Fix.COUNTER_FIAT
        }
    }

    @Test
    fun `in base crypto mode, when switching from account, clear the value`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.BASE_CRYPTO),
            SimpleFieldUpdateIntent(55.toBigDecimal()),
            ChangeCryptoFromAccount(fakeAccountReference(CryptoCurrency.ETHER))
        ) {
            fromFiat `should equal` 0.cad()
            toFiat `should equal` 0.cad()
            fromCrypto `should equal` 0.ether()
            toCrypto `should equal` 0.bitcoin()
            fix `should be` Fix.BASE_CRYPTO
        }
    }

    @Test
    fun `in counter crypto mode, when switching from account, clear the value`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.COUNTER_CRYPTO),
            SimpleFieldUpdateIntent(55.toBigDecimal()),
            ChangeCryptoFromAccount(fakeAccountReference(CryptoCurrency.ETHER))
        ) {
            fromFiat `should equal` 0.cad()
            toFiat `should equal` 0.cad()
            fromCrypto `should equal` 0.ether()
            toCrypto `should equal` 0.bitcoin()
            fix `should be` Fix.COUNTER_CRYPTO
        }
    }

    @Test
    fun `in base fiat mode, when using swap intent, keep the value`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.BASE_FIAT),
            SimpleFieldUpdateIntent(55.toBigDecimal()),
            SwapIntent()
        ) {
            fromCrypto `should equal` 0.ether()
            fromFiat `should equal` 55.cad()
            fix `should be` Fix.BASE_FIAT
        }
    }

    @Test
    fun `in base fiat mode, when switching to account, keep the value`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.BASE_FIAT),
            SimpleFieldUpdateIntent(55.toBigDecimal()),
            ChangeCryptoToAccount(fakeAccountReference(CryptoCurrency.BTC))
        ) {
            fromCrypto `should equal` 0.ether()
            fromFiat `should equal` 55.cad()
            fix `should be` Fix.BASE_FIAT
        }
    }

    @Test
    fun `in counter fiat mode, when switching to account, keep the value`() {
        given(
            initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER).setSomeValues()
        ).onLastStateAfter(
            SetFixIntent(Fix.COUNTER_FIAT),
            SimpleFieldUpdateIntent(100.toBigDecimal()),
            ChangeCryptoToAccount(fakeAccountReference(CryptoCurrency.BTC))
        ) {
            toCrypto `should equal` 0.bitcoin()
            toFiat `should equal` 100.usd()
            fix `should be` Fix.COUNTER_FIAT
        }
    }
}

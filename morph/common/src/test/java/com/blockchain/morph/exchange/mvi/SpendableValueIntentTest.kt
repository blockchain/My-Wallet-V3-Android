package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class SpendableValueIntentTest {

    @Test
    fun `can set the spendable value`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SpendableValueIntent(100.bitcoin())
        ) {
            maxSpendable `should equal` 100.bitcoin()
        }
    }

    @Test
    fun `ignores max spendable from in another currency`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).onLastStateAfter(
            SpendableValueIntent(100.ether())
        ) {
            maxSpendable `should be` null
        }
    }
}

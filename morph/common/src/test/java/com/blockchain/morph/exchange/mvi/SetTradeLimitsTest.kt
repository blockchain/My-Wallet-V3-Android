package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.cad
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class SetTradeLimitsTest {

    @Test
    fun `can set the trade limits`() {
        given(
            initial("CAD")
        ).onLastStateAfter(
            SetTradeLimits(10.cad(), 200.cad())
        ) {
            minTradeLimit `should equal` 10.cad()
            maxTradeLimit `should equal` 200.cad()
        }
    }

    @Test
    fun `ignores trade limit in the wrong currency`() {
        given(
            initial("USD")
        ).onLastStateAfter(
            SetTradeLimits(10.cad(), 200.cad())
        ) {
            minTradeLimit `should be` null
            maxTradeLimit `should be` null
        }
    }
}

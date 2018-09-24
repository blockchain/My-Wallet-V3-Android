package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.cad
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class ApplyMaximumTradeLimit {

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
            ApplyMaximumLimit()
        ) {
            fix `should be` Fix.BASE_CRYPTO
            fromFiat `should equal` 0.cad()
        }
    }
}

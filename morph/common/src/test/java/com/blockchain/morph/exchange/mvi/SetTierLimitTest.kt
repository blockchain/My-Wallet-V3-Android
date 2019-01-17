package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.cad
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class SetTierLimitTest {

    @Test
    fun `default tier limit`() {
        given(
            initial("CAD")
        ).onLastStateAfter {
            maxTierLimit `should be` null
        }
    }

    @Test
    fun `can set the tier limit`() {
        given(
            initial("CAD")
        ).onLastStateAfter(
            SetTierLimit(200.cad())
        ) {
            maxTierLimit `should equal` 200.cad()
        }
    }

    @Test
    fun `ignores tier limit in the wrong currency`() {
        given(
            initial("USD")
        ).onLastStateAfter(
            SetTierLimit(200.cad())
        ) {
            maxTierLimit `should be` null
        }
    }
}

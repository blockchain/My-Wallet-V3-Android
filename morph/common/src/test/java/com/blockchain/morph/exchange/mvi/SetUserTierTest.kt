package com.blockchain.morph.exchange.mvi

import org.amshove.kluent.`should equal`
import org.junit.Test

class SetUserTierTest {

    @Test
    fun `can set the users tier`() {
        given(
            initial("CAD")
        ).onLastStateAfter(
            SetUserTier(1)
        ) {
            userTier `should equal` 1
        }
    }

    @Test
    fun `can set the users tier 2`() {
        given(
            initial("CAD")
        ).onLastStateAfter(
            SetUserTier(2)
        ) {
            userTier `should equal` 2
        }
    }
}

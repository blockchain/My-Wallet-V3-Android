package com.blockchain.morph.exchange.mvi

import org.amshove.kluent.`should be`
import org.junit.Test

class FixTest {

    @Test
    fun `base fiat`() {
        Fix.BASE_FIAT.apply {
            isBase `should be` true
            isFiat `should be` true
            isCounter `should be` false
            isCrypto `should be` false
        }
    }

    @Test
    fun `base crypto`() {
        Fix.BASE_CRYPTO.apply {
            isBase `should be` true
            isFiat `should be` false
            isCounter `should be` false
            isCrypto `should be` true
        }
    }

    @Test
    fun `counter fiat`() {
        Fix.COUNTER_FIAT.apply {
            isBase `should be` false
            isFiat `should be` true
            isCounter `should be` true
            isCrypto `should be` false
        }
    }

    @Test
    fun `counter crypto`() {
        Fix.COUNTER_CRYPTO.apply {
            isBase `should be` false
            isFiat `should be` false
            isCounter `should be` true
            isCrypto `should be` true
        }
    }
}

package com.blockchain.morph.homebrew

import com.blockchain.morph.exchange.mvi.Fix
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should throw the Exception`
import org.amshove.kluent.`with message`
import org.junit.Test

class StringToFixTest {

    @Test
    fun `base crypto`() {
        "base".stringToFix() `should be` Fix.BASE_CRYPTO
    }

    @Test
    fun `base fiat`() {
        "baseInFiat".stringToFix() `should be` Fix.BASE_FIAT
    }

    @Test
    fun `counter crypto`() {
        "counter".stringToFix() `should be` Fix.COUNTER_CRYPTO
    }

    @Test
    fun `counter fiat`() {
        "counterInFiat".stringToFix() `should be` Fix.COUNTER_FIAT
    }

    @Test
    fun `unknown string`() {
        {
            "fiat".stringToFix()
        } `should throw the Exception` IllegalArgumentException::class `with message` "Unknown fix \"fiat\""
    }
}

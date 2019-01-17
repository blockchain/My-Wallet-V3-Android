package com.blockchain.kyc.models.nabu

import com.blockchain.testutils.gbp
import com.blockchain.testutils.usd
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class LimitsJsonFiatValuesTest {

    @Test
    fun `null daily fiat`() {
        LimitsJson(
            currency = "USD",
            daily = null,
            annual = 100.toBigDecimal()
        ).dailyFiat `should be` null
    }

    @Test
    fun `null annual fiat`() {
        LimitsJson(
            currency = "USD",
            daily = 100.toBigDecimal(),
            annual = null
        ).annualFiat `should be` null
    }

    @Test
    fun `can get daily fiat`() {
        LimitsJson(
            currency = "USD",
            daily = 100.toBigDecimal(),
            annual = null
        ).dailyFiat `should equal` 100.usd()
    }

    @Test
    fun `can get annual fiat`() {
        LimitsJson(
            currency = "GBP",
            daily = 100.toBigDecimal(),
            annual = 50.12.toBigDecimal()
        ).annualFiat `should equal` 50.12.gbp()
    }
}

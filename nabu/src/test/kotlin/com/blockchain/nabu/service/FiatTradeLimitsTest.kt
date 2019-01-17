package com.blockchain.nabu.service

import com.blockchain.morph.exchange.service.FiatPeriodicLimit
import com.blockchain.morph.exchange.service.FiatTradesLimits
import com.blockchain.testutils.usd
import info.blockchain.balance.FiatValue
import org.amshove.kluent.`should equal`
import org.junit.Test

class FiatTradeLimitsTest {

    @Test
    fun `min available is daily`() {
        givenLimits(daily = 1.usd(), annual = 2.usd(), weekly = 3.usd())
            .minAvailable() `should equal` 1.usd()
    }

    @Test
    fun `min available is weekly`() {
        givenLimits(daily = 4.usd(), annual = 5.usd(), weekly = 3.usd())
            .minAvailable() `should equal` 3.usd()
    }

    @Test
    fun `min available is annual`() {
        givenLimits(daily = 4.usd(), annual = 2.usd(), weekly = 3.usd())
            .minAvailable() `should equal` 2.usd()
    }

    @Test
    fun `min available is daily, rest null`() {
        givenLimits(daily = 1.usd(), annual = null, weekly = null)
            .minAvailable() `should equal` 1.usd()
    }

    @Test
    fun `min available is weekly, rest null`() {
        givenLimits(weekly = 1.usd(), annual = null, daily = null)
            .minAvailable() `should equal` 1.usd()
    }

    @Test
    fun `min available is annual, rest null`() {
        givenLimits(annual = 1.usd(), daily = null, weekly = null)
            .minAvailable() `should equal` 1.usd()
    }

    @Test
    fun `when all null, zero`() {
        givenLimits(annual = null, daily = null, weekly = null)
            .minAvailable() `should equal` 0.usd()
    }

    private fun givenLimits(
        daily: FiatValue?,
        annual: FiatValue?,
        weekly: FiatValue?
    ): FiatTradesLimits {
        return FiatTradesLimits(
            0.99.usd(), 0.99.usd(), 0.99.usd(),
            daily = FiatPeriodicLimit(
                limit = 0.usd(),
                available = daily,
                used = 0.usd()
            ),
            weekly = FiatPeriodicLimit(
                limit = 0.usd(),
                available = weekly,
                used = 0.usd()
            ),
            annual = FiatPeriodicLimit(
                limit = 0.usd(),
                available = annual,
                used = 0.usd()
            )
        )
    }
}

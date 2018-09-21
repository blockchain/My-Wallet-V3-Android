package com.blockchain.datamanagers.fees

import org.amshove.kluent.`should equal`
import org.junit.Test

class BitcoinLikeFeesTest {

    @Test
    fun `given fee per byte, should return fee per kb`() {
        BitcoinLikeFees(10, 1000)
            .apply {
                regularFeePerKb `should equal` (10 * 1000).toBigInteger()
                priorityFeePerKb `should equal` (1000 * 1000).toBigInteger()
            }
    }
}
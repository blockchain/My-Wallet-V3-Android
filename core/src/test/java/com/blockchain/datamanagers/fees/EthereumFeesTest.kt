package com.blockchain.datamanagers.fees

import info.blockchain.balance.CryptoValue
import org.amshove.kluent.`should equal`
import org.junit.Test

class EthereumFeesTest {

    @Test
    fun `given gas price and limit in gwei, should return wei`() {
        // One Gwei = 1000,000,000 wei
        EthereumFees(
            1,
            1
        ).apply {
            gasPriceWei `should equal` (1000_000_000).toBigInteger()
            gasLimitWei `should equal` (1000_000_000).toBigInteger()
        }
    }

    @Test
    fun `given gas price and limit, should return absolute fee in wei`() {
        EthereumFees(
            5,
            5
        ).apply {
            absoluteFee `should equal` CryptoValue.etherFromWei(25_000_000_000.toBigInteger())
        }
    }
}
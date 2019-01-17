package com.blockchain.balance

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import info.blockchain.balance.CryptoCurrency
import org.amshove.kluent.`should equal`
import org.junit.Test

class TotalBalanceBalanceTest {

    @Test
    fun `zero bitcoin`() {
        TotalBalance.Balance.zero(CryptoCurrency.BTC)
            .apply {
                spendable `should equal` 0.bitcoin()
                watchOnly `should equal` 0.bitcoin()
                coldStorage `should equal` 0.bitcoin()
            }
    }

    @Test
    fun `zero either`() {
        TotalBalance.Balance.zero(CryptoCurrency.ETHER)
            .apply {
                spendable `should equal` 0.ether()
                watchOnly `should equal` 0.ether()
                coldStorage `should equal` 0.ether()
            }
    }

    @Test
    fun `total spendable and cold storage`() {
        TotalBalance.Balance(
            spendable = 100.bitcoin(),
            coldStorage = 20.bitcoin(),
            watchOnly = 30.bitcoin()
        ).spendableAndColdStorage `should equal` 120.bitcoin()
    }
}

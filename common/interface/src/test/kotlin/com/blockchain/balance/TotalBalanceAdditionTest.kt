package com.blockchain.balance

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test

class TotalBalanceAdditionTest {

    @Test
    fun `can sum total balances`() {
        (givenTotalBalance(
            TotalBalance.Balance(
                spendable = 1.bitcoin(),
                watchOnly = 2.bitcoin(),
                coldStorage = 3.bitcoin()
            )
        ) + givenTotalBalance(
            TotalBalance.Balance(
                spendable = 0.1.bitcoin(),
                watchOnly = 0.2.bitcoin(),
                coldStorage = 0.3.bitcoin()
            )
        ))
            .totalBalance(CryptoCurrency.BTC)
            .test().values().single() `should equal`
            TotalBalance.Balance(
                spendable = 1.1.bitcoin(),
                watchOnly = 2.2.bitcoin(),
                coldStorage = 3.3.bitcoin()
            )
    }

    @Test
    fun `can sum total balances - Bitcoin cash`() {
        (givenTotalBalance(
            TotalBalance.Balance(
                spendable = 10.bitcoinCash(),
                watchOnly = 20.bitcoinCash(),
                coldStorage = 30.bitcoinCash()
            )
        ) + givenTotalBalance(
            TotalBalance.Balance(
                spendable = 0.01.bitcoinCash(),
                watchOnly = 0.02.bitcoinCash(),
                coldStorage = 0.03.bitcoinCash()
            )
        ))
            .totalBalance(CryptoCurrency.BCH)
            .test().values().single() `should equal`
            TotalBalance.Balance(
                spendable = 10.01.bitcoinCash(),
                watchOnly = 20.02.bitcoinCash(),
                coldStorage = 30.03.bitcoinCash()
            )
    }

    private fun givenTotalBalance(totalBalance: TotalBalance.Balance): TotalBalance =
        mock {
            on { totalBalance(totalBalance.coldStorage.currency) } `it returns` Single.just(
                totalBalance
            )
        }
}

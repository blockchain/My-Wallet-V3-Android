package com.blockchain.lockbox.balance

import com.blockchain.accounts.AsyncAccountList
import com.blockchain.balance.AsyncAccountBalanceReporter
import com.blockchain.balance.TotalBalance
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Maybe
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test

class LockboxTotalBalanceTest {

    @Test
    fun `balances of BTC accounts`() {
        givenBalances(
            AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "xpub1") to 123.bitcoin(),
            AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "xpub2") to 0.456.bitcoin()
        )
            .totalBalance(CryptoCurrency.BTC)
            .test()
            .assertNoErrors()
            .values()
            .single() `should equal`
            TotalBalance.Balance(
                coldStorage = 123.456.bitcoin(),
                watchOnly = 0.bitcoin(),
                spendable = 0.bitcoin()
            )
    }

    @Test
    fun `balances of ETH accounts`() {
        givenBalances(
            AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "xpub1") to 123.bitcoin(),
            AccountReference.BitcoinLike(CryptoCurrency.BTC, "", "xpub2") to 0.456.bitcoin(),
            AccountReference.Ethereum("", "0xAddr1") to 99.ether(),
            AccountReference.Ethereum("", "0xAddr2") to 100.ether()
        )
            .totalBalance(CryptoCurrency.ETHER)
            .test()
            .assertNoErrors()
            .values()
            .single() `should equal`
            TotalBalance.Balance(
                coldStorage = 199.ether(),
                watchOnly = 0.ether(),
                spendable = 0.ether()
            )
    }

    private fun givenBalances(vararg pairs: Pair<AccountReference, CryptoValue>): TotalBalance {
        val lockboxAccountList = mock<AsyncAccountList> {
            on { accounts() } `it returns` Single.just(pairs.map { (accountRef) -> accountRef })
        }
        val accountBalanceReporter = mock<AsyncAccountBalanceReporter> {
            on { balanceOf(any()) } `it returns` Maybe.empty()
        }
        pairs.forEach { (key, value) ->
            whenever(accountBalanceReporter.balanceOf(key)) `it returns` Maybe.just(value)
        }
        return LockboxTotalBalance(lockboxAccountList, accountBalanceReporter)
    }
}

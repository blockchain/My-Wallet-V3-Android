package com.blockchain.lockbox.balance

import com.blockchain.accounts.AsyncAccountList
import com.blockchain.balance.AsyncAccountBalanceReporter
import com.blockchain.balance.TotalBalance
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single

internal class LockboxTotalBalance(
    private val lockboxAccountList: AsyncAccountList,
    private val accountBalanceReporter: AsyncAccountBalanceReporter
) : TotalBalance {

    override fun totalBalance(cryptoCurrency: CryptoCurrency): Single<TotalBalance.Balance> {
        val zero = CryptoValue.zero(cryptoCurrency)
        return lockboxAccountList.accounts()
            .map { it.filter { account -> account.cryptoCurrency == cryptoCurrency } }
            .toObservable().concatMapIterable { it }
            .flatMapSingle {
                accountBalanceReporter.balanceOf(it).toSingle(zero)
            }
            .reduceWith(
                { zero },
                { sum, value ->
                    sum + value
                }
            )
            .map { TotalBalance.Balance(spendable = zero, coldStorage = it, watchOnly = zero) }
    }
}

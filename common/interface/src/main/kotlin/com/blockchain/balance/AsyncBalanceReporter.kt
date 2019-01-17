package com.blockchain.balance

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import io.reactivex.Maybe
import io.reactivex.Single

interface AsyncBalanceReporter {
    fun entireBalance(): Single<CryptoValue>
    fun watchOnlyBalance(): Single<CryptoValue>
    fun importedAddressBalance(): Single<CryptoValue>
    fun addressBalance(address: String): Single<CryptoValue>
}

interface AsyncAddressBalanceReporter {

    fun getBalance(address: String): Single<CryptoValue>
}

interface AsyncAccountBalanceReporter {

    /**
     * Returns balance, or if reference does not match a reference type instance can process, empty maybe.
     * Idea is they can be chained with [plus].
     */
    fun balanceOf(accountReference: AccountReference): Maybe<CryptoValue>
}

operator fun AsyncAccountBalanceReporter.plus(other: AsyncAccountBalanceReporter): AsyncAccountBalanceReporter =
    object : AsyncAccountBalanceReporter {
        override fun balanceOf(accountReference: AccountReference) =
            this@plus.balanceOf(accountReference).switchIfEmpty(Maybe.defer { other.balanceOf(accountReference) })
    }

package com.blockchain.balance

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles

interface TotalBalance {

    data class Balance(
        /**
         * The total spendable balance.
         */
        val spendable: CryptoValue,

        /**
         * The total cold storage balance.
         */
        val coldStorage: CryptoValue,

        /**
         * This is the watch only balance, a balance we do not have the keys for.
         */
        val watchOnly: CryptoValue
    ) {

        val spendableAndColdStorage = spendable + coldStorage

        companion object {

            fun zero(currency: CryptoCurrency): Balance {
                val zero = CryptoValue.zero(currency)
                return Balance(zero, zero, zero)
            }
        }
    }

    /**
     * Returns Total balance as (Spendable, ColdStorage, Watch Only) triple
     */
    fun totalBalance(cryptoCurrency: CryptoCurrency): Single<Balance>
}

operator fun TotalBalance.plus(other: TotalBalance): TotalBalance =
    object : TotalBalance {
        override fun totalBalance(cryptoCurrency: CryptoCurrency) =
            Singles.zip(
                this@plus.totalBalance(cryptoCurrency),
                other.totalBalance(cryptoCurrency)
            ) { a, b ->
                TotalBalance.Balance(
                    spendable = a.spendable + b.spendable,
                    watchOnly = a.watchOnly + b.watchOnly,
                    coldStorage = a.coldStorage + b.coldStorage
                )
            }
    }

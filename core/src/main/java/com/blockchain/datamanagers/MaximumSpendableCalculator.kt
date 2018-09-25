package com.blockchain.datamanagers

import com.blockchain.datamanagers.fees.getFeeOptions
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import piuk.blockchain.androidcore.data.fees.FeeDataManager

interface MaximumSpendableCalculator {

    fun getMaximumSpendable(accountReference: AccountReference): Single<CryptoValue>
}

internal class MaximumSpendableCalculatorImplementation(
    private val transactionSendDataManager: TransactionSendDataManager,
    private val feeDataManager: FeeDataManager
) : MaximumSpendableCalculator {

    override fun getMaximumSpendable(accountReference: AccountReference): Single<CryptoValue> =
        feeDataManager.getFeeOptions(accountReference.cryptoCurrency)
            .flatMap { fees ->
                transactionSendDataManager.getMaximumSpendable(accountReference, fees)
            }
}

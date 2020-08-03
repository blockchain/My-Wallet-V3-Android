package com.blockchain.datamanagers

import com.blockchain.datamanagers.fees.getFeeOptions
import com.blockchain.fees.FeeType
import com.blockchain.logging.SwapDiagnostics
import com.blockchain.transactions.Memo
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoCurrency.Companion.IS_ERC20
import info.blockchain.balance.CryptoValue
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import timber.log.Timber

internal class SelfFeeCalculatingTransactionExecutor(
    private val transactionExecutor: TransactionExecutor,
    private val balanceCalculator: BalanceCalculator,
    private val feeDataManager: FeeDataManager,
    private val feeType: FeeType
) : TransactionExecutorWithoutFees, TransactionExecutorAddresses by transactionExecutor {

    override fun hasEnoughEthFeesForTheTransaction(
        amount: CryptoValue,
        sendingAccount: AccountReference
    ): Single<Boolean> {
        return if (!amount.currency.hasFeature(IS_ERC20)) {
            return Single.just(true)
        } else {
            balanceCalculator.balance(CryptoCurrency.ETHER).singleOrError()
                .zipWith(getFeeForTransaction(amount, sendingAccount)) { balance, fee ->
                    balance >= fee
                }
        }
    }

    override fun getFeeForTransaction(amount: CryptoValue, account: AccountReference): Single<CryptoValue> {
        return feeDataManager.getFeeOptions(amount.currency)
            .flatMap { fees ->
                transactionExecutor.getFeeForTransaction(
                    amount,
                    account,
                    fees,
                    feeType
                )
            }
    }

    override fun executeTransaction(
        amount: CryptoValue,
        destination: String,
        sourceAccount: AccountReference,
        memo: Memo?,
        diagnostics: SwapDiagnostics?
    ): Single<String> {
        return feeDataManager.getFeeOptions(amount.currency)
            .flatMap { fees ->
                diagnostics?.log("executing Transaction.")
                transactionExecutor.executeTransaction(
                    amount,
                    destination,
                    sourceAccount,
                    fees,
                    feeType,
                    memo,
                    diagnostics
                )
            }
    }

    override fun getMaximumSpendable(accountReference: AccountReference): Single<CryptoValue> {
        return feeDataManager.getFeeOptions(accountReference.cryptoCurrency)
            .flatMap { fees -> transactionExecutor.getMaximumSpendable(accountReference, fees, feeType) }
            .doOnError(Timber::e)
            .onErrorReturn { CryptoValue.zero(accountReference.cryptoCurrency) }
    }
}

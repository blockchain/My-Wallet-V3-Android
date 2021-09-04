package piuk.blockchain.android.coincore.impl.txEngine

import info.blockchain.wallet.api.dust.data.DustInput
import io.reactivex.rxjava3.core.Single
import org.bitcoinj.core.Transaction
import piuk.blockchain.android.coincore.PendingTx

interface EngineTransaction {
    val encodedMsg: String
    val msgSize: Int
    val txHash: String
}

interface PaymentClientEngine {
    fun doPrepareTransaction(pendingTx: PendingTx): Single<Pair<Transaction, DustInput?>>
    fun doSignTransaction(
        tx: Transaction,
        pendingTx: PendingTx,
        secondPassword: String
    ): Single<EngineTransaction>

    fun doOnTransactionSuccess(pendingTx: PendingTx)
    fun doOnTransactionFailed(pendingTx: PendingTx, e: Throwable)
}
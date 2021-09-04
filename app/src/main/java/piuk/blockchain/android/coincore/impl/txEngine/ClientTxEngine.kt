package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import org.bitcoinj.core.Transaction
import org.reactivestreams.Subscription
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.InvoiceTarget
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.copyAndPut
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.ClientDataManager
import piuk.blockchain.android.data.api.bitpay.analytics.BitPayEvent
import piuk.blockchain.android.data.api.bitpay.models.BitPayTransaction
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.util.concurrent.TimeUnit

const val PAYMENT_TIMER_SUB = "payment_timer"
private val PendingTx.paymentTimer: Subscription?
    get() = (this.engineState[PAYMENT_TIMER_SUB] as? Subscription)

abstract class ClientTxEngine(
    private val clientDataManager: ClientDataManager,
    private val assetEngine: OnChainTxEngineBase,
    private val walletPrefs: WalletStatus,
    private val analytics: Analytics
) : TxEngine() {

    private val executionClient: PaymentClientEngine by unsafeLazy {
        assetEngine as PaymentClientEngine
    }

    private val invoiceTarget: InvoiceTarget by unsafeLazy {
        txTarget as InvoiceTarget
    }

    override fun start(
        sourceAccount: BlockchainAccount,
        txTarget: TransactionTarget,
        exchangeRates: ExchangeRatesDataManager,
        refreshTrigger: RefreshTrigger
    ) {
        super.start(sourceAccount, txTarget, exchangeRates, refreshTrigger)
        assetEngine.start(sourceAccount, txTarget, exchangeRates, refreshTrigger)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        assetEngine.doInitialiseTx()
            .map { tx ->
                tx.copy(
                    amount = invoiceTarget.amount!!,
                    feeSelection = tx.feeSelection.copy(
                        selectedLevel = FeeLevel.Priority,
                        availableLevels = AVAILABLE_FEE_LEVELS
                    )
                )
            }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        assetEngine.doUpdateAmount(invoiceTarget.amount!!, pendingTx)
            .flatMap { assetEngine.doBuildConfirmations(it) }
            .map { pTx ->
                startTimerIfNotStarted(pTx)
            }.map { pTx ->
                pTx.addOrReplaceOption(
                    TxConfirmationValue.BitPayCountdown(timeRemainingSecs()),
                    true
                )
            }

    override fun doRefreshConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx.addOrReplaceOption(TxConfirmationValue.BitPayCountdown(timeRemainingSecs()), true))

    private fun startTimerIfNotStarted(pendingTx: PendingTx): PendingTx =
        if (pendingTx.paymentTimer == null) {
            pendingTx.copy(
                engineState = pendingTx.engineState.copyAndPut(
                    PAYMENT_TIMER_SUB, startCountdownTimer(timeRemainingSecs())
                )
            )
        } else {
            pendingTx
        }

    private fun timeRemainingSecs() =
        (invoiceTarget.expireTimeMs - System.currentTimeMillis()) / 1000

    private fun startCountdownTimer(remainingTime: Long): Disposable {
        var remaining = remainingTime
        return Observable.interval(1, TimeUnit.SECONDS)
            .doOnEach { remaining-- }
            .map { remaining }
            .doOnNext { updateCountdownConfirmation() }
            .takeUntil { it <= TIMEOUT_STOP }
            .doOnComplete { handleCountdownComplete() }
            .subscribe()
    }

    private fun updateCountdownConfirmation() {
        refreshConfirmations(false)
    }

    private fun handleCountdownComplete() {
        Timber.d("Lunu Invoice Countdown expired")
        refreshConfirmations(true)
    }

    // Don't set the amount here, it is fixed so we can do it in the confirmation building step
    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx)

    override fun doUpdateFeeLevel(
        pendingTx: PendingTx,
        level: FeeLevel,
        customFeeAmount: Long
    ): Single<PendingTx> {
        require(pendingTx.feeSelection.availableLevels.contains(level))
        return Single.just(pendingTx)
    }

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        assetEngine.doValidateAmount(pendingTx)

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        doValidateTimeout(pendingTx)
            .flatMap { assetEngine.doValidateAll(pendingTx) }
            .updateTxValidity(pendingTx)

    private fun doValidateTimeout(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(pendingTx)
            .map { pTx ->
                if (timeRemainingSecs() <= TIMEOUT_STOP) {
                    analytics.logEvent(BitPayEvent.InvoiceExpired)
                    throw TxValidationFailure(ValidationState.INVOICE_EXPIRED)
                }
                pTx
            }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        executionClient.doPrepareTransaction(pendingTx)
            .flatMap { (tx, _) ->
                doVerifyTransaction(invoiceTarget.invoiceId, tx)
            }.flatMap { txVerified ->
                executionClient.doSignTransaction(txVerified, pendingTx, secondPassword)
            }.flatMap { engineTx ->
                doExecuteTransaction(invoiceTarget.invoiceId, engineTx)
            }.doOnSuccess {
                walletPrefs.setBitPaySuccess()
                analytics.logEvent(BitPayEvent.TxSuccess(pendingTx.amount as CryptoValue))
                executionClient.doOnTransactionSuccess(pendingTx)
            }.doOnError { e ->
                analytics.logEvent(BitPayEvent.TxFailed(e.message ?: e.toString()))
                executionClient.doOnTransactionFailed(pendingTx, e)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    fun doVerifyTransaction(
        invoiceId: String,
        tx: Transaction
    ): Single<Transaction> =
        clientDataManager.paymentVerificationRequest(
            invoiceId = invoiceId,
            paymentRequest = BitPaymentRequest(
                sourceAsset.ticker,
                listOf(
                    BitPayTransaction(
                        String(Hex.encode(tx.bitcoinSerialize())),
                        tx.messageSize
                    )
                )
            )
        ).andThen(Single.just(tx))

    fun doExecuteTransaction(
        invoiceId: String,
        tx: EngineTransaction
    ): Single<String> =
        clientDataManager.paymentSubmitRequest(
            invoiceId = invoiceId,
            paymentRequest = BitPaymentRequest(
                sourceAsset.ticker,
                listOf(
                    BitPayTransaction(
                        tx.encodedMsg,
                        tx.msgSize
                    )
                )
            )
        ).andThen(Single.just(tx.txHash))

    companion object {
        private const val TIMEOUT_STOP = 2
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Priority)
    }
}

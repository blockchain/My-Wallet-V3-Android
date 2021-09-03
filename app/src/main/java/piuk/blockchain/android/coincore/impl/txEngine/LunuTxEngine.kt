package piuk.blockchain.android.coincore.impl.txEngine

import com.blockchain.core.price.ExchangeRatesDataManager
import com.blockchain.notifications.analytics.Analytics
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.api.dust.data.DustInput
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.Disposable
import org.bitcoinj.core.Transaction
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.android.coincore.BlockchainAccount
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TransactionTarget
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxEngine
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.copyAndPut
import piuk.blockchain.android.coincore.impl.BitPayInvoiceTarget
import piuk.blockchain.android.coincore.impl.CryptoNonCustodialAccount
import piuk.blockchain.android.coincore.impl.LunuInvoiceTarget
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.android.data.api.bitpay.BitPayDataManager
import piuk.blockchain.android.data.api.bitpay.LUNU_LIVE_BASE
import piuk.blockchain.android.data.api.bitpay.PATH_LUNU_INVOICE
import piuk.blockchain.android.data.api.bitpay.analytics.BitPayEvent
import piuk.blockchain.android.data.api.bitpay.models.BitPayTransaction
import piuk.blockchain.android.data.api.bitpay.models.BitPaymentRequest
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import rx.Subscription
import timber.log.Timber
import java.util.concurrent.TimeUnit

/*
const val BITPAY_TIMER_SUB = "bitpay_timer"
private val PendingTx.bitpayTimer: Subscription?
    get() = (this.engineState[BITPAY_TIMER_SUB] as? Subscription)

interface EngineTransaction {
    val encodedMsg: String
    val msgSize: Int
    val txHash: String
}
*/

interface LunuClientEngine {
    fun doPrepareTransaction(pendingTx: PendingTx): Single<Pair<Transaction, DustInput?>>
    fun doSignTransaction(
        tx: Transaction,
        pendingTx: PendingTx,
        secondPassword: String
    ): Single<EngineTransaction>

    fun doOnTransactionSuccess(pendingTx: PendingTx)
    fun doOnTransactionFailed(pendingTx: PendingTx, e: Throwable)
}

class LunuTxEngine(
    private val bitPayDataManager: BitPayDataManager,
    private val assetEngine: OnChainTxEngineBase,
    private val walletPrefs: WalletStatus,
    private val analytics: Analytics
) : TxEngine() {
    private val INVOICE_PREFIX = "$LUNU_LIVE_BASE$PATH_LUNU_INVOICE"

    override fun assertInputsValid() {
        // Only support non-custodial BTC & BCH bitpay at this time
        val supportedCryptoCurrencies = listOf(CryptoCurrency.BTC, CryptoCurrency.BCH)
        check(supportedCryptoCurrencies.contains(sourceAsset))
        check(sourceAccount is CryptoNonCustodialAccount)
        check(txTarget is LunuInvoiceTarget)
        require(assetEngine is LunuClientEngine)
        assetEngine.assertInputsValid()
    }

    private val executionClient: LunuClientEngine by unsafeLazy {
        assetEngine as LunuClientEngine
    }

    private val lunuInvoice: LunuInvoiceTarget by unsafeLazy {
        txTarget as LunuInvoiceTarget
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
                    amount = lunuInvoice.amount,
                    feeSelection = tx.feeSelection.copy(
                        selectedLevel = FeeLevel.Priority,
                        availableLevels = AVAILABLE_FEE_LEVELS
                    )
                )
            }

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        assetEngine.doUpdateAmount(lunuInvoice.amount, pendingTx)
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
        if (pendingTx.bitpayTimer == null) {
            pendingTx.copy(
                engineState = pendingTx.engineState.copyAndPut(
                    BITPAY_TIMER_SUB, startCountdownTimer(timeRemainingSecs())
                )
            )
        } else {
            pendingTx
        }

    private fun timeRemainingSecs() =
        (lunuInvoice.expireTimeMs - System.currentTimeMillis()) / 1000

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
        Timber.d("BitPay Invoice Countdown expired")
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
                doVerifyTransaction(lunuInvoice.invoiceId, tx)
            }.flatMap { txVerified ->
                executionClient.doSignTransaction(txVerified, pendingTx, secondPassword)
            }.flatMap { engineTx ->
                doExecuteTransaction(lunuInvoice.invoiceId, engineTx)
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

    private fun doVerifyTransaction(
        invoiceId: String,
        tx: Transaction
    ): Single<Transaction> =
        bitPayDataManager.paymentVerificationRequest(
            path = INVOICE_PREFIX,
            invoiceId,
            BitPaymentRequest(
                sourceAsset.ticker,
                listOf(
                    BitPayTransaction(
                        String(Hex.encode(tx.bitcoinSerialize())),
                        tx.messageSize
                    )
                )
            )
        ).andThen(Single.just(tx))

    private fun doExecuteTransaction(
        invoiceId: String,
        tx: EngineTransaction
    ): Single<String> =
        bitPayDataManager.paymentSubmitRequest(
            path = INVOICE_PREFIX,
            invoiceId,
            BitPaymentRequest(
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

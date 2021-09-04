package piuk.blockchain.android.coincore.bch

import com.blockchain.core.chains.bitcoincash.BchDataManager
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.api.dust.data.DustInput
import info.blockchain.wallet.bch.BchMainNetParams
import info.blockchain.wallet.exceptions.HDWalletException
import info.blockchain.wallet.keys.SigningKey
import info.blockchain.wallet.payload.data.XPub
import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import org.bitcoinj.core.Transaction
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.copyAndPut
import piuk.blockchain.android.coincore.impl.txEngine.EngineTransaction
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.impl.txEngine.PaymentClientEngine
import piuk.blockchain.android.coincore.toUserFiat
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.android.ui.transactionflow.flow.FeeInfo
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigInteger

private const val STATE_UTXO = "bch_utxo"

private val PendingTx.unspentOutputBundle: SpendableUnspentOutputs
    get() = (this.engineState[STATE_UTXO] as SpendableUnspentOutputs)

class BchOnChainTxEngine(
    private val bchDataManager: BchDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val feeManager: FeeDataManager,
    walletPreferences: WalletStatus,
    requireSecondPassword: Boolean
) : OnChainTxEngineBase(
    requireSecondPassword,
    walletPreferences
), PaymentClientEngine {

    private val bchSource: BchCryptoWalletAccount by unsafeLazy {
        sourceAccount as BchCryptoWalletAccount
    }

    private val bchTarget: CryptoAddress
        get() = txTarget as CryptoAddress

    override fun assertInputsValid() {
        check(txTarget is CryptoAddress)
        check((txTarget as CryptoAddress).asset == CryptoCurrency.BCH)
        check(sourceAsset == CryptoCurrency.BCH)
    }

    override fun doInitialiseTx(): Single<PendingTx> =
        Single.just(
            PendingTx(
                amount = CryptoValue.zero(sourceAsset),
                totalBalance = CryptoValue.zero(sourceAsset),
                availableBalance = CryptoValue.zero(sourceAsset),
                feeForFullAvailable = CryptoValue.zero(sourceAsset),
                feeAmount = CryptoValue.zero(sourceAsset),
                feeSelection = FeeSelection(
                    selectedLevel = FeeLevel.Regular,
                    availableLevels = AVAILABLE_FEE_LEVELS,
                    asset = sourceAsset
                ),
                selectedFiat = userFiat
            )
        )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> {
        require(amount is CryptoValue)
        require(amount.currency == sourceAsset)

        return Singles.zip(
            sourceAccount.accountBalance.map { it as CryptoValue },
            getUnspentApiResponse(bchSource.xpubAddress),
            getDynamicFeePerKb()
        ) { balance, coins, feePerKb ->
            updatePendingTx(amount, balance, pendingTx, feePerKb, coins)
        }.onErrorReturn {
            pendingTx.copy(
                validationState = ValidationState.INSUFFICIENT_FUNDS
            )
        }
    }

    private fun getUnspentApiResponse(address: String): Single<List<Utxo>> =
        if (bchDataManager.getAddressBalance(address) > CryptoValue.zero(sourceAsset)) {
            sendDataManager.getUnspentBchOutputs(address)
                // If we get here, we should have balance and valid UTXOs. IF we don't, then, um... we'd best fail hard
                .map { utxo ->
                    if (utxo.isEmpty()) {
                        Timber.e("No BTC UTXOs found for non-zero balance!")
                        throw IllegalStateException("No BTC UTXOs found for non-zero balance")
                    } else {
                        utxo
                    }
                }
        } else {
            Single.error(Throwable("No BCH funds"))
        }

    private fun updatePendingTx(
        amount: CryptoValue,
        balance: CryptoValue,
        pendingTx: PendingTx,
        feePerKb: CryptoValue,
        coins: List<Utxo>
    ): PendingTx {
        val targetOutputType = payloadDataManager.getAddressOutputType(bchTarget.address)
        val changeOutputType = payloadDataManager.getXpubFormatOutputType(XPub.Format.LEGACY)

        val available = sendDataManager.getMaximumAvailable(
            asset = sourceAsset,
            targetOutputType = targetOutputType,
            unspentCoins = coins,
            feePerKb = feePerKb
        )

        val unspentOutputs = sendDataManager.getSpendableCoins(
            unspentCoins = coins,
            targetOutputType = targetOutputType,
            changeOutputType = changeOutputType,
            paymentAmount = amount,
            feePerKb = feePerKb
        )

        return pendingTx.copy(
            amount = amount,
            totalBalance = balance,
            availableBalance = available.maxSpendable,
            feeForFullAvailable = available.feeForMax,
            feeAmount = CryptoValue.fromMinor(sourceAsset, unspentOutputs.absoluteFee),
            engineState = pendingTx.engineState.copyAndPut(STATE_UTXO, unspentOutputs)
        )
    }

    private fun getDynamicFeePerKb(): Single<CryptoValue> =
        feeManager.bchFeeOptions
            .map { feeOptions ->
                feeToCrypto(feeOptions.regularFee)
            }.singleOrError()

    private fun feeToCrypto(feePerKb: Long): CryptoValue =
        CryptoValue.fromMinor(sourceAsset, (feePerKb * 1000).toBigInteger())

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .updateTxValidity(pendingTx)

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            val amount = pendingTx.amount.toBigInteger()
            if (amount < Payment.DUST || amount > MAX_BCH_AMOUNT || amount <= BigInteger.ZERO) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (!pendingTx.hasSufficientFunds()) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }
        }

    private fun PendingTx.hasSufficientFunds() =
        availableBalance >= amount && unspentOutputBundle.spendableOutputs.isNotEmpty()

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            buildConfirmations(pendingTx)
        )

    private fun buildConfirmations(pendingTx: PendingTx): PendingTx =
        pendingTx.copy(
            confirmations = listOfNotNull(
                TxConfirmationValue.From(sourceAccount, sourceAsset),
                TxConfirmationValue.To(
                    txTarget, AssetAction.Send, sourceAccount
                ),
                TxConfirmationValue.CompoundNetworkFee(
                    sendingFeeInfo = if (!pendingTx.feeAmount.isZero) {
                        FeeInfo(
                            pendingTx.feeAmount,
                            pendingTx.feeAmount.toUserFiat(exchangeRates),
                            sourceAsset
                        )
                    } else null,
                    feeLevel = pendingTx.feeSelection.selectedLevel
                ),
                TxConfirmationValue.Total(
                    totalWithFee = (pendingTx.amount as CryptoValue).plus(
                        pendingTx.feeAmount as CryptoValue
                    ),
                    exchange = pendingTx.amount.toUserFiat(exchangeRates)
                        .plus(pendingTx.feeAmount.toUserFiat(exchangeRates))
                )
            )
        )

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAddress()
            .then { validateAmounts(pendingTx) }
            .then { validateSufficientFunds(pendingTx) }
            .updateTxValidity(pendingTx)

    private fun validateAddress() =
        Completable.fromCallable {
            if (!FormatsUtil.isValidBCHAddress(bchTarget.address) &&
                !FormatsUtil.isValidBitcoinAddress(bchTarget.address)
            ) {
                throw TxValidationFailure(ValidationState.INVALID_ADDRESS)
            }
        }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        doPrepareTransaction(pendingTx)
            .flatMap { (tx, dustInput) ->
                doSignTransaction(tx, pendingTx, secondPassword).map { it to dustInput }
            }.flatMap { (engineTx, dustInput) ->
                val bchPreparedTx = engineTx as BchPreparedTx
                dustInput?.let {
                    sendDataManager.submitBchPayment(bchPreparedTx.bchTx, it)
                }
            }.doOnSuccess {
                doOnTransactionSuccess(pendingTx)
            }.doOnError { e ->
                doOnTransactionFailed(pendingTx, e)
            }.onErrorResumeNext {
                Single.error(TransactionError.ExecutionFailed)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    private fun getBchChangeAddress(): Single<String> {
        val position = bchDataManager.getAccountMetadataList()
            .indexOfFirst {
                it.xpubs().default.address == bchSource.xpubAddress
            }
        return bchDataManager.getNextChangeCashAddress(position).singleOrError()
    }

    private fun getBchKeys(pendingTx: PendingTx, secondPassword: String): Single<List<SigningKey>> {
        if (payloadDataManager.isDoubleEncrypted) {
            payloadDataManager.decryptHDWallet(secondPassword)
            bchDataManager.decryptWatchOnlyWallet(payloadDataManager.mnemonic)
        }

        val hdAccountList = bchDataManager.getAccountList()
        val acc = hdAccountList.find {
            val networkParams = BchMainNetParams.get()
            val xpub = bchSource.xpubAddress
            val node = it.node.serializePubB58(networkParams)
            node == xpub
        } ?: throw HDWalletException("No matching private key found for ${bchSource.xpubAddress}")

        return Single.just(
            bchDataManager.getHDKeysForSigning(
                acc,
                pendingTx.unspentOutputBundle.spendableOutputs
            )
        )
    }

    private fun incrementBchReceiveAddress(pendingTx: PendingTx) {
        val xpub = bchSource.xpubAddress
        bchDataManager.incrementNextChangeAddress(xpub)
        bchDataManager.incrementNextReceiveAddress(xpub)
        updateInternalBchBalances(pendingTx, xpub)
    }

    private fun updateInternalBchBalances(pendingTx: PendingTx, xpub: String) {
        try {
            bchDataManager.subtractAmountFromAddressBalance(xpub, pendingTx.totalSent.toBigInteger())
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        super.doPostExecute(pendingTx, txResult)
            .doOnComplete { bchSource.forceRefresh() }

    companion object {
        private val AVAILABLE_FEE_LEVELS = setOf(FeeLevel.Regular)
        private val MAX_BCH_AMOUNT = 2_100_000_000_000_000L.toBigInteger()
    }

    class BchPreparedTx(
        val bchTx: Transaction
    ) : EngineTransaction {
        override val encodedMsg: String
            get() = String(Hex.encode(bchTx.bitcoinSerialize()))
        override val msgSize: Int = bchTx.messageSize
        override val txHash: String = bchTx.hashAsString
    }

    override fun doOnTransactionSuccess(pendingTx: PendingTx) {
        incrementBchReceiveAddress(pendingTx)
    }

    override fun doOnTransactionFailed(pendingTx: PendingTx, e: Throwable) {
        Timber.e("BCH Send failed: $e")
    }

    override fun doSignTransaction(
        tx: Transaction,
        pendingTx: PendingTx,
        secondPassword: String
    ): Single<EngineTransaction> =
        getBchKeys(pendingTx, secondPassword).map { keys ->
            BchPreparedTx(
                sendDataManager.getSignedBchTransaction(
                    tx,
                    keys
                )
            )
        }

    override fun doPrepareTransaction(
        pendingTx: PendingTx
    ): Single<Pair<Transaction, DustInput?>> =
        getBchChangeAddress().flatMap {
            sendDataManager.getBchTransaction(
                pendingTx.unspentOutputBundle,
                FormatsUtil.makeFullBitcoinCashAddress(bchTarget.address),
                it,
                pendingTx.feeAmount.toBigInteger(),
                pendingTx.amount.toBigInteger()
            ).firstOrError()
        }
}

private val PendingTx.totalSent: Money
    get() = amount + feeAmount
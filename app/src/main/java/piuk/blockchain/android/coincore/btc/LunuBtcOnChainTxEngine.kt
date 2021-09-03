package piuk.blockchain.android.coincore.btc

import com.blockchain.logging.CrashLogger
import com.blockchain.nabu.datamanagers.TransactionError
import com.blockchain.preferences.WalletStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.Money
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.api.dust.data.DustInput
import info.blockchain.wallet.payload.data.XPubs
import info.blockchain.wallet.payload.model.Utxo
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.payment.SpendableUnspentOutputs
import info.blockchain.wallet.util.FormatsUtil
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.Singles
import org.bitcoinj.core.Transaction
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.spongycastle.util.encoders.Hex
import piuk.blockchain.android.coincore.AssetAction
import piuk.blockchain.android.coincore.CryptoAddress
import piuk.blockchain.android.coincore.FeeLevel
import piuk.blockchain.android.coincore.FeeLevelRates
import piuk.blockchain.android.coincore.FeeSelection
import piuk.blockchain.android.coincore.PendingTx
import piuk.blockchain.android.coincore.TxConfirmation
import piuk.blockchain.android.coincore.TxConfirmationValue
import piuk.blockchain.android.coincore.TxResult
import piuk.blockchain.android.coincore.TxValidationFailure
import piuk.blockchain.android.coincore.ValidationState
import piuk.blockchain.android.coincore.copyAndPut
import piuk.blockchain.android.coincore.impl.txEngine.BitPayClientEngine
import piuk.blockchain.android.coincore.impl.txEngine.EngineTransaction
import piuk.blockchain.android.coincore.impl.txEngine.LunuClientEngine
import piuk.blockchain.android.coincore.impl.txEngine.OnChainTxEngineBase
import piuk.blockchain.android.coincore.toFiat
import piuk.blockchain.android.coincore.toUserFiat
import piuk.blockchain.android.coincore.updateTxValidity
import piuk.blockchain.android.ui.transactionflow.flow.FeeInfo
import piuk.blockchain.androidcore.data.fees.FeeDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.androidcore.utils.extensions.then
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger

private const val STATE_UTXO = "btc_utxo"
private const val FEE_OPTIONS = "fee_options"

private val PendingTx.utxoBundle: SpendableUnspentOutputs
    get() = (this.engineState[STATE_UTXO] as? SpendableUnspentOutputs) ?: SpendableUnspentOutputs()

private val PendingTx.feeOptions: FeeOptions
    get() = (this.engineState[FEE_OPTIONS] as? FeeOptions) ?: FeeOptions()

private class LunuBtcPreparedTx(
    val btcTx: Transaction
) : EngineTransaction {
    override val encodedMsg: String
        get() = String(Hex.encode(btcTx.bitcoinSerialize()))
    override val msgSize: Int = btcTx.messageSize
    override val txHash: String = btcTx.hashAsString
}

class LunuBtcOnChainTxEngine(
    private val btcDataManager: PayloadDataManager,
    private val sendDataManager: SendDataManager,
    private val feeManager: FeeDataManager,
    walletPreferences: WalletStatus,
    requireSecondPassword: Boolean
) : OnChainTxEngineBase(
    requireSecondPassword,
    walletPreferences
), LunuClientEngine, KoinComponent {

    override fun assertInputsValid() {
        check(sourceAccount is BtcCryptoWalletAccount)
        check(txTarget is CryptoAddress)
        check((txTarget as CryptoAddress).asset == CryptoCurrency.BTC)
        check(sourceAsset == CryptoCurrency.BTC)
    }

    private val btcTarget: CryptoAddress
        get() = txTarget as CryptoAddress

    private val btcSource: BtcCryptoWalletAccount by unsafeLazy {
        sourceAccount as BtcCryptoWalletAccount
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
                    selectedLevel = mapSavedFeeToFeeLevel(fetchDefaultFeeLevel(sourceAsset)),
                    availableLevels = AVAILABLE_FEE_OPTIONS,
                    asset = sourceAsset
                ),
                selectedFiat = userFiat
            )
        )

    override fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx> =
        Single.zip(
            sourceAccount.accountBalance.map { it as CryptoValue },
            getDynamicFeePerKb(pendingTx),
            getUnspentApiResponse(btcSource.xpubs)
        ) { total, optionsAndFeePerKb, coins ->
            updatePendingTxFromAmount(
                amount as CryptoValue,
                total,
                pendingTx,
                optionsAndFeePerKb.second,
                optionsAndFeePerKb.first,
                coins
            )
        }.onErrorReturnItem(
            pendingTx.copy(
                validationState = ValidationState.INSUFFICIENT_FUNDS
            )
        )

    private fun getUnspentApiResponse(xpubs: XPubs): Single<List<Utxo>> {
        val balance = btcDataManager.getAddressBalance(xpubs)
        return if (balance.isPositive) {
            sendDataManager.getUnspentBtcOutputs(xpubs)
                // If we get here, we should have balance...
                // but if we have no UTXOs then we have a problem:
                .map { utxo ->
                    if (utxo.isEmpty()) {
                        throw fatalError(IllegalStateException("No BTC UTXOs found for non-zero balance"))
                    } else {
                        utxo
                    }
                }
        } else {
            Single.error(Throwable("No BTC funds"))
        }
    }

    private fun getDynamicFeePerKb(pendingTx: PendingTx): Single<Pair<FeeOptions, CryptoValue>> =
        feeManager.btcFeeOptions
            .map { feeOptions ->
                when (pendingTx.feeSelection.selectedLevel) {
                    FeeLevel.None -> Pair(feeOptions, CryptoValue.zero(sourceAsset))
                    FeeLevel.Regular -> Pair(feeOptions, feeToCrypto(feeOptions.regularFee))
                    FeeLevel.Priority -> Pair(feeOptions, feeToCrypto(feeOptions.priorityFee))
                    FeeLevel.Custom -> Pair(feeOptions, feeToCrypto(pendingTx.feeSelection.customAmount))
                }
            }.singleOrError()

    private fun feeToCrypto(feePerKb: Long): CryptoValue =
        CryptoValue.fromMinor(sourceAsset, (feePerKb * 1000).toBigInteger())

    private fun updatePendingTxFromAmount(
        amount: CryptoValue,
        balance: CryptoValue,
        pendingTx: PendingTx,
        feePerKb: CryptoValue,
        feeOptions: FeeOptions,
        coins: List<Utxo>
    ): PendingTx {
        val targetOutputType = btcDataManager.getAddressOutputType(btcTarget.address)
        val changeOutputType = btcDataManager.getXpubFormatOutputType(btcSource.xpubs.default.derivation)

        val available = sendDataManager.getMaximumAvailable(
            asset = sourceAsset,
            targetOutputType = targetOutputType,
            unspentCoins = coins,
            feePerKb = feePerKb
        ) // This is total balance, with fees deducted

        val utxoBundle = sendDataManager.getSpendableCoins(
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
            feeAmount = CryptoValue.fromMinor(sourceAsset, utxoBundle.absoluteFee),
            feeSelection = pendingTx.feeSelection.copy(
                customLevelRates = feeOptions.toLevelRates()
            ),
            engineState = pendingTx.engineState
                .copyAndPut(STATE_UTXO, utxoBundle)
                .copyAndPut(FEE_OPTIONS, feeOptions)
        ).let {
            it.copy(
                feeSelection = it.feeSelection.copy(
                    feeState = getFeeState(it, it.feeOptions)
                )
            )
        }
    }

    private fun FeeOptions.toLevelRates(): FeeLevelRates =
        FeeLevelRates(
            regularFee = regularFee,
            priorityFee = priorityFee
        )

    override fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx> =
        validateAmounts(pendingTx)
            .then { validateSufficientFunds(pendingTx) }
            .updateTxValidity(pendingTx)

    override fun doBuildConfirmations(pendingTx: PendingTx): Single<PendingTx> =
        Single.just(
            buildConfirmations(pendingTx)
        )

    private fun buildConfirmations(pendingTx: PendingTx): PendingTx =
        pendingTx.copy(
            confirmations = listOfNotNull(
                TxConfirmationValue.From(sourceAccount, sourceAsset),
                TxConfirmationValue.To(txTarget, AssetAction.Send, sourceAccount),
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
                ),
                TxConfirmationValue.Description(),
                if (isLargeTransaction(pendingTx)) {
                    TxConfirmationValue.TxBooleanConfirmation<Unit>(
                        TxConfirmation.LARGE_TRANSACTION_WARNING
                    )
                } else null
            )
        )

    // Returns true if bitcoin transaction is large by checking against 3 criteria:
    //  * If the fee > $0.50 AND
    //  * the Tx size is over 1kB AND
    //  * the ratio of fee/amount is over 1%
    private fun isLargeTransaction(pendingTx: PendingTx): Boolean {
        val fiatValue = pendingTx.feeAmount.toFiat(LARGE_TX_FIAT, exchangeRates)

        val outputs = listOf(
            btcDataManager.getAddressOutputType(btcTarget.address),
            btcDataManager.getXpubFormatOutputType(btcSource.xpubs.default.derivation)
        )

        val txSize = sendDataManager.estimateSize(
            inputs = pendingTx.utxoBundle.spendableOutputs,
            outputs = outputs // assumes change required
        )

        val relativeFee =
            BigDecimal(100) * (pendingTx.feeAmount.toBigDecimal() / pendingTx.amount.toBigDecimal())
        return fiatValue.toBigDecimal() > BigDecimal(LARGE_TX_FEE) &&
            txSize > LARGE_TX_SIZE &&
            relativeFee > LARGE_TX_PERCENTAGE
    }

    override fun doValidateAll(pendingTx: PendingTx): Single<PendingTx> =
        validateAddress()
            .then { validateAmounts(pendingTx) }
            .then { validateSufficientFunds(pendingTx) }
            .then { validateOptions(pendingTx) }
            .logValidityFailure()
            .updateTxValidity(pendingTx)

    private fun validateAddress(): Completable =
        Completable.fromCallable {
            if (!FormatsUtil.isValidBitcoinAddress(btcTarget.address)) {
                throw TxValidationFailure(ValidationState.INVALID_ADDRESS)
            }
        }

    private fun validateAmounts(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            val amount = pendingTx.amount.toBigInteger()
            if (amount < Payment.DUST) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }

            if (amount > MAX_BTC_AMOUNT) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }

            if (amount <= BigInteger.ZERO) {
                throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
            }
        }

    private fun validateSufficientFunds(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            if (pendingTx.availableBalance < pendingTx.amount) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }

            if (pendingTx.utxoBundle.spendableOutputs.isEmpty()) {
                throw TxValidationFailure(ValidationState.INSUFFICIENT_FUNDS)
            }
        }

    private fun validateOptions(pendingTx: PendingTx): Completable =
        Completable.fromCallable {
            // If the large_fee warning is present, make sure it's ack'd.
            // If it's not, then there's nothing to do
            if (pendingTx.getOption<TxConfirmationValue.TxBooleanConfirmation<Unit>>(
                    TxConfirmation.LARGE_TRANSACTION_WARNING
                )?.value == false
            ) {
                throw TxValidationFailure(ValidationState.OPTION_INVALID)
            }

            if (pendingTx.feeSelection.selectedLevel == FeeLevel.Custom) {
                when {
                    pendingTx.feeSelection.customAmount == -1L ->
                        throw TxValidationFailure(ValidationState.INVALID_AMOUNT)
                    pendingTx.feeSelection.customAmount < MINIMUM_CUSTOM_FEE ->
                        throw TxValidationFailure(ValidationState.UNDER_MIN_LIMIT)
                }
            }
        }

    override fun doExecute(pendingTx: PendingTx, secondPassword: String): Single<TxResult> =
        doPrepareTransaction(pendingTx)
            .flatMap { (tx, _) ->
                doSignTransaction(tx, pendingTx, secondPassword)
            }.flatMap { engineTx ->
                val btcTx = engineTx as LunuBtcPreparedTx
                sendDataManager.submitBtcPayment(btcTx.btcTx)
            }.doOnSuccess {
                doOnTransactionSuccess(pendingTx)
            }.doOnError { e ->
                doOnTransactionFailed(pendingTx, e)
            }.onErrorResumeNext {
                Single.error(TransactionError.ExecutionFailed)
            }.map {
                TxResult.HashedTxResult(it, pendingTx.amount)
            }

    // Logic to decide on sending change to bech32 xpub change or receive chain.
    // When moving from legacy to segwit, we should send change to at least one receive address before we start
    // using change address. The BE cannot determine balances held in change address on a derivation chain without
    // at least one receive address having a value.
    // A better way of doing this is to see if we have a +ve change address index, but the routing to have that
    // information available here is complex, and this will work. We should re-visit this when BTC downstack refactoring
    // makes it more sensible.
    private fun selectAddressForChange(
        inputs: SpendableUnspentOutputs,
        changeAddress: String,
        receiveAddress: String
    ): String =
        changeAddress.takeIf { inputs.spendableOutputs.firstOrNull { it.isSegwit } != null } ?: receiveAddress

    override fun doSignTransaction(
        tx: Transaction,
        pendingTx: PendingTx,
        secondPassword: String
    ): Single<EngineTransaction> =
        btcSource.getSigningKeys(
            pendingTx.utxoBundle,
            secondPassword
        )
            .map { keys ->
                LunuBtcPreparedTx(
                    sendDataManager.getSignedBtcTransaction(
                        tx,
                        keys
                    )
                )
            }

    override fun doPrepareTransaction(
        pendingTx: PendingTx
    ): Single<Pair<Transaction, DustInput?>> =
        Singles.zip(
            btcSource.getChangeAddress(),
            btcSource.receiveAddress
        ).map { (changeAddress, receiveAddress) ->
            sendDataManager.getBtcTransaction(
                pendingTx.utxoBundle,
                btcTarget.address,
                selectAddressForChange(pendingTx.utxoBundle, changeAddress, receiveAddress.address),
                pendingTx.feeAmount.toBigInteger(),
                pendingTx.amount.toBigInteger()
            ) to null
        }

    override fun doOnTransactionSuccess(pendingTx: PendingTx) {
        btcSource.incrementReceiveAddress()
        updateInternalBtcBalances(pendingTx)
    }

    override fun doOnTransactionFailed(pendingTx: PendingTx, e: Throwable) {
        Timber.e("BTC Send failed: $e")
        crashLogger.logException(e)
    }

    // Update balance immediately after spend - until refresh from server
    private fun updateInternalBtcBalances(pendingTx: PendingTx) {
        try {
            val totalSent = pendingTx.totalSent.toBigInteger()
            val address = btcSource.xpubAddress
            btcDataManager.subtractAmountFromAddressBalance(
                address,
                totalSent.toLong()
            )
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    override fun doPostExecute(pendingTx: PendingTx, txResult: TxResult): Completable =
        super.doPostExecute(pendingTx, txResult)
            .doOnComplete { btcSource.forceRefresh() }

    companion object {
        const val LARGE_TX_FIAT = "USD"
        const val LARGE_TX_FEE = 0.5
        const val LARGE_TX_SIZE = 1024
        val LARGE_TX_PERCENTAGE = BigDecimal(1.0)

        private val AVAILABLE_FEE_OPTIONS = setOf(FeeLevel.Regular, FeeLevel.Priority, FeeLevel.Custom)
        private val MAX_BTC_AMOUNT = 2_100_000_000_000_000L.toBigInteger()
    }

    // TEMP diagnostics - TODO Remove this once we're stable
    private val crashLogger: CrashLogger by inject()

    private fun Completable.logValidityFailure(): Completable =
        this.doOnError { crashLogger.logException(it) }

    private fun fatalError(e: Throwable): Throwable {
        crashLogger.logException(e)
        return e
    }
}

private val PendingTx.totalSent: Money
    get() = amount + feeAmount

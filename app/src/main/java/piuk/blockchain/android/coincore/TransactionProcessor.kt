package piuk.blockchain.android.coincore

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import info.blockchain.balance.Money
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.subjects.BehaviorSubject
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager

open class TransferError(msg: String) : Exception(msg)

enum class ValidationState {
    CAN_EXECUTE,
    UNINITIALISED,
    HAS_TX_IN_FLIGHT,
    INVALID_AMOUNT,
    INSUFFICIENT_FUNDS,
    INSUFFICIENT_GAS,
    INVALID_ADDRESS,
    ADDRESS_IS_CONTRACT,
    OPTION_INVALID,
    UNDER_MIN_LIMIT,
    OVER_MAX_LIMIT
}

class TxValidationFailure(val state: ValidationState) : TransferError("Invalid Send Tx: $state")

enum class FeeLevel {
    None,
    Regular,
    Priority,
    Custom
}

data class PendingTx(
    val amount: Money,
    val available: Money,
    val fees: Money,
    val feeLevel: FeeLevel = FeeLevel.Regular,
    val options: Set<TxOptionValue> = emptySet(),
    val minLimit: Money? = null,
    val maxLimit: Money? = null,
    val validationState: ValidationState = ValidationState.UNINITIALISED
) {
    fun hasOption(option: TxOption): Boolean =
        options.find { it.option == option } != null

    inline fun <reified T : TxOptionValue> getOption(option: TxOption): T? =
        options.find { it.option == option } as? T
}

enum class TxOption {
    DESCRIPTION,
    AGREEMENT_INTEREST_T_AND_C,
    AGREEMENT_INTEREST_TRANSFER,
}

sealed class TxOptionValue {
    abstract val option: TxOption

    data class TxTextOption(
        override val option: TxOption,
        val text: String = ""
    ) : TxOptionValue()

    data class TxBooleanOption(
        override val option: TxOption,
        val value: Boolean = false
    ) : TxOptionValue()
}

abstract class TransactionProcessor(
    protected val sendingAccount: CryptoAccount,
    protected val sendTarget: SendTarget,
    protected val exchangeRates: ExchangeRateDataManager
) {

    // This may be moved into options at some point in the near future.
    abstract val feeOptions: Set<FeeLevel>

    protected val asset: CryptoCurrency
        get() = sendingAccount.asset

    open val requireSecondPassword: Boolean = false
    open val canTransactFiat: Boolean = false

    private val txObservable: BehaviorSubject<PendingTx> = BehaviorSubject.create()

    private fun updatePendingTx(pendingTx: PendingTx) {
        txObservable.onNext(pendingTx)
    }

    private fun getPendingTx(): PendingTx =
        txObservable.value ?: throw IllegalStateException("TransactionProcessor not initialised")

    // Initialise the tx as required.
    // This will start propagating the pendingTx to the client code.
    fun initialiseTx(): Observable<PendingTx> =
        doInitialiseTx()
            .doOnSuccess {
                updatePendingTx(it)
            }.flatMapObservable {
                txObservable
            }

    // Set the option to the passed option value. If the option is not supported, it will not be
    // in the original list when the pendingTx is created. And if it is not supported, then trying to
    // update it will cause an error.
    fun setOption(newOption: TxOptionValue): Completable {
        val pendingTx = getPendingTx()
        if (!pendingTx.hasOption(newOption.option)) {
            throw IllegalArgumentException("Unsupported TxOption: ${newOption.option}")
        }
        val opts = pendingTx.options.toMutableSet()
        val old = opts.find { it.option == newOption.option }
        opts.remove(old)
        opts.add(newOption)

        return doValidateAll(pendingTx.copy(options = opts))
            .doOnSuccess { updatePendingTx(it) }
            .ignoreElement()
    }

    fun updateAmount(amount: Money): Completable {
        val pendingTx = getPendingTx()
        if (!canTransactFiat && amount is FiatValue)
            throw IllegalArgumentException("The processor does not support fiat values")

        return doUpdateAmount(amount, pendingTx)
            .flatMap { doValidateAmount(it) }
            .doOnSuccess { updatePendingTx(it) }
            .ignoreElement()
    }

    // Return a stream of the exchange rate between the source asset and the user's selected
    // fiat currency. This should always return at least once, but can safely either complete
    // or keep sending updated rates, depending on what is useful for Transaction context
    open fun userExchangeRate(userFiat: String): Observable<ExchangeRate> =
        Observable.just(
            exchangeRates.getLastPrice(sendingAccount.asset, userFiat)
        ).map { rate ->
            ExchangeRate.CryptoToFiat(
                sendingAccount.asset,
                userFiat,
                rate.toBigDecimal()
            )
        }

    // Check the validity of a pending transactions.
    fun validateAll(): Completable {
        val pendingTx = getPendingTx()
        return doValidateAll(pendingTx)
            .doOnSuccess { updatePendingTx(it) }
            .ignoreElement()
    }

    // Execute the transaction.
    // Ideally, I'd like to return the Tx id/hash. But we get nothing back from the
    // custodial APIs (and are not likely to, since the tx is batched and not executed immediately)
    fun execute(secondPassword: String = ""): Completable {
        if (requireSecondPassword && secondPassword.isEmpty())
            throw IllegalArgumentException("Second password not supplied")

        val pendingTx = getPendingTx()
        return doValidateAll(pendingTx)
            .doOnSuccess {
                if (it.validationState != ValidationState.CAN_EXECUTE)
                    throw IllegalStateException("PendingTx is not executable")
            }.doOnSuccess {
                doExecute(it, secondPassword)
            }.ignoreElement()
        }

    // If the source and target assets are not the same this MAY return a stream of the exchange rates
    // between them. Or it may simply complete. This is not used yet in the UI, but it may be when
    // sell and or swap are fully integrated into this flow
    open fun targetExchangeRate(): Observable<ExchangeRate> =
        Observable.empty()

    // Implementation interface:
    // Call this first to initialise the processor. Construct and initialise a pendingTx object.
    protected abstract fun doInitialiseTx(): Single<PendingTx>

    // Update the transaction with a new amount. This method should check balances, calculate fees and
    // Return a new PendingTx with the state updated for the UI to update. The pending Tx will
    // be passed to validate after this call.
    protected abstract fun doUpdateAmount(amount: Money, pendingTx: PendingTx): Single<PendingTx>

    // Check the tx is complete, well formed and possible. If it is, set pendingTx to CAN_EXECUTE
    // Else set it to the appropriate error, and then return the updated PendingTx
    protected abstract fun doValidateAmount(pendingTx: PendingTx): Single<PendingTx>

    // Check the tx is complete, well formed and possible. If it is, set pendingTx to CAN_EXECUTE
    // Else set it to the appropriate error, and then return the updated PendingTx
    protected abstract fun doValidateAll(pendingTx: PendingTx): Single<PendingTx>

    // Execute the transaction, it will have been validated before this is called, so the expectation
    // is that it will succeed.
    protected abstract fun doExecute(pendingTx: PendingTx, secondPassword: String): Completable
}

fun Completable.updateTxValidity(pendingTx: PendingTx): Single<PendingTx> =
    this.toSingle {
        pendingTx.copy(validationState = ValidationState.CAN_EXECUTE)
    }
    .onErrorReturn {
        if (it is TxValidationFailure) {
            pendingTx.copy(validationState = it.state)
        } else {
            throw it
        }
    }
package piuk.blockchain.android.ui.shapeshift.detail

import com.blockchain.morph.CoinPair
import com.blockchain.morph.to
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.shapeshift.models.TradeDetailUiState
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ShapeShiftDetailPresenter @Inject constructor(
    private val shapeShiftDataManager: ShapeShiftDataManager,
    private val stringUtils: StringUtils
) : BasePresenter<ShapeShiftDetailView>() {

    private val decimalFormat by unsafeLazy {
        DecimalFormat.getNumberInstance(view.locale).apply {
            minimumIntegerDigits = 1
            minimumFractionDigits = 1
            maximumFractionDigits = 8
        }
    }

    override fun onViewReady() {
        // Find trade first in list
        shapeShiftDataManager.findTrade(view.depositAddress)
            .applySchedulers()
            .addToCompositeDisposable(this)
            .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
            .doOnError {
                view.showToast(R.string.shapeshift_trade_not_found, ToastCustom.TYPE_ERROR)
                view.finishPage()
            }
            // Display information that we have stored
            .doOnSuccess {
                updateUiAmounts(it)
                handleTrade(it)
            }
            // Get trade info from ShapeShift only if necessary
            .flatMapObservable {
                if (requiresMoreInfoForUi(it)) {
                    shapeShiftDataManager.getTradeStatus(view.depositAddress)
                        .doOnNext { handleTradeResponse(it) }
                } else {
                    Observable.just(it)
                }
            }
            .doOnTerminate { view.dismissProgressDialog() }
            // Start polling for results anyway
            .flatMap {
                Observable.interval(10, TimeUnit.SECONDS, Schedulers.io())
                    .flatMap { shapeShiftDataManager.getTradeStatus(view.depositAddress) }
                    .applySchedulers()
                    .addToCompositeDisposable(this)
                    .doOnNext { handleTradeResponse(it) }
                    .takeUntil { isInFinalState(it.status) }
            }
            .subscribe(
                {
                    // Doesn't particularly matter if completion is interrupted here
                    with(it) {
                        updateMetadata(address, transaction, status)
                    }
                },
                {
                    Timber.e(it)
                }
            )
    }

    private fun handleTradeResponse(tradeStatusResponse: TradeStatusResponse) {
        with(tradeStatusResponse) {
            val fromCoin: CryptoCurrency = CryptoCurrency.fromSymbol(incomingType ?: "btc")!!
            val toCoin: CryptoCurrency = CryptoCurrency.fromSymbol(outgoingType ?: "eth")!!
            val fromAmount: BigDecimal? = incomingCoin
            val toAmount: BigDecimal? = outgoingCoin
            val pair = fromCoin to toCoin

            fromAmount?.let { updateDeposit(pair.from, it) }
            toAmount?.let { updateReceive(pair.to, it) }

            if (pair.sameInputOutput) {
                onRefunded()
                return
            }
        }

        handleState(tradeStatusResponse.status)
    }

    private fun requiresMoreInfoForUi(trade: Trade): Boolean =
    // Web isn't currently storing the deposit amount for some reason
        trade.quote.depositAmount == null ||
            trade.quote.pair.isNullOrEmpty() ||
            trade.quote.pair == "_"

    private fun updateUiAmounts(trade: Trade) {
        with(trade) {
            updateOrderId(quote.orderId)
            // Web don't store everything, but we do. Check here and make an assumption
            if (quote.pair.isNullOrEmpty() || quote.pair == "_") {
                quote.pair = "btc_eth"
            }
            val pair = CoinPair.fromPairCode(quote.pair)

            updateDeposit(pair.from, quote.depositAmount ?: BigDecimal.ZERO)
            updateReceive(pair.to, quote.withdrawalAmount ?: BigDecimal.ZERO)
            updateExchangeRate(quote.quotedRate ?: BigDecimal.ZERO, pair)
            updateTransactionFee(pair.to, quote.minerFee ?: BigDecimal.ZERO)
        }
    }

    // region View Updates
    private fun updateDeposit(fromCurrency: CryptoCurrency, depositAmount: BigDecimal) {
        val label =
            stringUtils.getFormattedString(R.string.shapeshift_deposit_title, fromCurrency.unit)
        val amount = "${depositAmount.toLocalisedString()} ${fromCurrency.symbol.toUpperCase()}"

        view.updateDeposit(label, amount)
    }

    private fun updateReceive(toCurrency: CryptoCurrency, receiveAmount: BigDecimal) {
        val label =
            stringUtils.getFormattedString(R.string.shapeshift_receive_title, toCurrency.unit)
        val amount = "${receiveAmount.toLocalisedString()} ${toCurrency.symbol.toUpperCase()}"

        view.updateReceive(label, amount)
    }

    private fun updateExchangeRate(
        exchangeRate: BigDecimal,
        pair: CoinPair
    ) {
        val formattedExchangeRate = exchangeRate.setScale(8, RoundingMode.HALF_DOWN)
            .toLocalisedString()
        val formattedString = stringUtils.getFormattedString(
            R.string.shapeshift_exchange_rate_formatted,
            pair.from.symbol,
            formattedExchangeRate,
            pair.to.symbol
        )

        view.updateExchangeRate(formattedString)
    }

    private fun updateTransactionFee(fromCurrency: CryptoCurrency, transactionFee: BigDecimal) {
        val displayString = "${transactionFee.toLocalisedString()} ${fromCurrency.symbol}"

        view.updateTransactionFee(displayString)
    }

    private fun updateOrderId(displayString: String) {
        view.updateOrderId(displayString)
    }
    // endregion

    private fun updateMetadata(address: String, hashOut: String?, status: Trade.STATUS) {
        shapeShiftDataManager.findTrade(address)
            .map {
                it.apply {
                    this.status = status
                    this.hashOut = hashOut
                }
            }
            .flatMapCompletable { shapeShiftDataManager.updateTrade(it) }
            .addToCompositeDisposable(this)
            .subscribe(
                { Timber.d("Update metadata entry complete") },
                { Timber.e(it) }
            )
    }

    // region UI State
    private fun handleTrade(trade: Trade) {
        val pair = CoinPair.fromPairCode(trade.quote.pair)
        if (pair.sameInputOutput) {
            onRefunded()
        } else {
            handleState(trade.status)
        }
    }

    private fun handleState(status: Trade.STATUS) = when (status) {
        Trade.STATUS.NO_DEPOSITS -> onNoDeposit()
        Trade.STATUS.RECEIVED -> onReceived()
        Trade.STATUS.COMPLETE -> onComplete()
        Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> onFailed()
    }

    private fun onNoDeposit() {
        val state = TradeDetailUiState(
            R.string.shapeshift_sending_title,
            R.string.shapeshift_sending_title,
            stringUtils.getFormattedString(R.string.shapeshift_step_number, 1),
            R.drawable.shapeshift_progress_airplane,
            R.color.black
        )
        view.updateUi(state)
    }

    private fun onReceived() {
        val state = TradeDetailUiState(
            R.string.shapeshift_in_progress_title,
            R.string.shapeshift_in_progress_summary,
            stringUtils.getFormattedString(R.string.shapeshift_step_number, 2),
            R.drawable.shapeshift_progress_exchange,
            R.color.black
        )
        view.updateUi(state)
    }

    private fun onComplete() {
        val state = TradeDetailUiState(
            R.string.shapeshift_complete_title,
            R.string.shapeshift_complete_title,
            stringUtils.getFormattedString(R.string.shapeshift_step_number, 3),
            R.drawable.shapeshift_progress_complete,
            R.color.black
        )
        view.updateUi(state)
    }

    private fun onFailed() {
        val state = TradeDetailUiState(
            R.string.shapeshift_failed_title,
            R.string.shapeshift_failed_summary,
            stringUtils.getString(R.string.shapeshift_failed_explanation),
            R.drawable.shapeshift_progress_failed,
            R.color.product_gray_hint
        )
        view.updateUi(state)
    }

    private fun onRefunded() {
        val state = TradeDetailUiState(
            R.string.shapeshift_refunded_title,
            R.string.shapeshift_refunded_summary,
            stringUtils.getString(R.string.shapeshift_refunded_explanation),
            R.drawable.shapeshift_progress_failed,
            R.color.product_gray_hint
        )
        view.updateUi(state)
    }
    // endregion

    private fun isInFinalState(status: Trade.STATUS) = when (status) {
        Trade.STATUS.NO_DEPOSITS, Trade.STATUS.RECEIVED -> false
        Trade.STATUS.COMPLETE, Trade.STATUS.FAILED, Trade.STATUS.RESOLVED -> true
    }

    private fun BigDecimal.toLocalisedString(): String = decimalFormat.format(this)
}
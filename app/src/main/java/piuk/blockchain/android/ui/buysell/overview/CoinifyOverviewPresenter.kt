package piuk.blockchain.android.ui.buysell.overview

import android.support.annotation.StringRes
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.buysell.details.models.AwaitingFundsModel
import piuk.blockchain.android.ui.buysell.details.models.BuySellDetailsModel
import piuk.blockchain.android.ui.buysell.details.models.RecurringTradeDisplayModel
import piuk.blockchain.android.ui.buysell.overview.models.BuySellButtons
import piuk.blockchain.android.ui.buysell.overview.models.BuySellDisplayable
import piuk.blockchain.android.ui.buysell.overview.models.BuySellTransaction
import piuk.blockchain.android.ui.buysell.overview.models.EmptyTransactionList
import piuk.blockchain.android.ui.buysell.overview.models.KycStatus
import piuk.blockchain.android.ui.buysell.overview.models.RecurringBuyOrder
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.android.util.extensions.toFormattedString
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.TradeData
import piuk.blockchain.androidbuysell.models.coinify.BankDetails
import piuk.blockchain.androidbuysell.models.coinify.BuyFrequency
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.Medium
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.models.coinify.Subscription
import piuk.blockchain.androidbuysell.models.coinify.TradeState
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidbuysell.utils.fromIso8601
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import piuk.blockchain.androidcore.utils.extensions.toSerialisedString
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import timber.log.Timber
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class CoinifyOverviewPresenter @Inject constructor(
    private val exchangeService: ExchangeService,
    private val coinifyDataManager: CoinifyDataManager,
    private val metadataManager: MetadataManager,
    private val stringUtils: StringUtils
) : BasePresenter<CoinifyOverviewView>() {

    // Display States
    private val buttons = BuySellButtons()
    private val empty = EmptyTransactionList()
    // Display List
    private val displayList: MutableList<BuySellDisplayable> = mutableListOf(buttons)
    // Observables
    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .map {
                it.coinify?.token ?: throw IllegalStateException("Coinify offline token is null")
            }
            .firstOrError()
            .cache()

    private val tradesSingle: Observable<CoinifyTrade>
        get() = tokenSingle
            .flatMapObservable { coinifyDataManager.getTrades(it) }

    private val kycReviewsSingle: Single<List<KycResponse>> by unsafeLazy {
        tokenSingle
            .flatMap { coinifyDataManager.getKycReviews(it) }
    }

    private val traderSellLimitSingle: Single<Pair<Double, String>> by unsafeLazy {
        tokenSingle
            .flatMap { coinifyDataManager.getTrader(it) }
            .map { it.level.limits.bank.outLimits.daily to it.defaultCurrency }
    }

    private val recurringBuySingle: Single<List<Subscription>> by unsafeLazy {
        tokenSingle
            .flatMapObservable { coinifyDataManager.getSubscriptions(it) }
            .filter { it.isActive }
            .toList()
            .cache()
    }

    override fun onViewReady() {
        renderTrades(emptyList())
        view.renderViewState(OverViewState.Loading)
        checkKycStatus()
        checkSubscriptionStatus()
    }

    internal fun refreshTransactionList() {
        tradesSingle
            .toList()
            .doOnSuccess { updateMetadataAsNeeded(it) }
            .toObservable()
            .flatMapIterable { it }
            .map { mapTradeToDisplayObject(it) }
            .toList()
            .doOnError { Timber.e(it) }
            .subscribeBy(
                onSuccess = { renderTrades(it) },
                onError = {
                    view.renderViewState(OverViewState.Failure(R.string.buy_sell_overview_error_loading_transactions))
                }
            )
    }

    internal fun onBuySelected() {
        kycReviewsSingle
            .doOnSubscribe { view.displayProgressDialog() }
            .doAfterTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onSuccess = {
                    if (it.kycUnverified()) {
                        view.launchCardBuyFlow()
                    } else {
                        view.launchBuyPaymentSelectionFlow()
                    }
                },
                onError = {
                    view.renderViewState(OverViewState.Failure(R.string.unexpected_error))
                }
            )
    }

    internal fun onTransactionSelected(transactionId: Int) {
        tradesSingle
            .doOnSubscribe { view.displayProgressDialog() }
            .filter { it.id == transactionId }
            .firstOrError()
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .subscribeBy(
                onSuccess = {
                    if (it.isAwaitingTransferIn()) {
                        view.launchAwaitingTransferPage(getAwaitingFundsModel(it))
                    } else {
                        view.launchDetailsPage(getBuySellDetailsModel(it))
                    }
                },
                onError = {
                    view.renderViewState(OverViewState.Failure(R.string.buy_sell_overview_error_loading_transactions))
                }
            )
    }

    internal fun onCompleteKycSelected() {
        tokenSingle
            .flatMap { token ->
                coinifyDataManager.getTrader(token)
                    .flatMap { coinifyDataManager.getKycReviews(token) }
            }
            .map {
                it.firstOrNull { it.state == ReviewState.Pending || it.state == ReviewState.DocumentsRequested }
                    ?: throw IllegalStateException("No pending KYC found")
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .addToCompositeDisposable(this)
            .subscribeBy(
                onSuccess = { view.onStartVerifyIdentification(it.redirectUrl, it.externalId) },
                onError = {
                    Timber.e(it)
                    view.showAlertDialog(R.string.buy_sell_overview_pending_kyc_not_found)
                }
            )
    }

    internal fun onRestartKycSelected() {
        tokenSingle
            .flatMap { coinifyDataManager.startKycReview(it) }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .addToCompositeDisposable(this)
            .subscribeBy(
                onSuccess = { view.onStartVerifyIdentification(it.redirectUrl, it.externalId) },
                onError = {
                    Timber.e(it)
                    view.showAlertDialog(R.string.buy_sell_overview_could_not_start_kyc)
                }
            )
    }

    internal fun onSubscriptionClicked(subscriptionId: Int) {
        Single.zip(
            recurringBuySingle,
            tradesSingle
                .filter { it.tradeSubscriptionId == subscriptionId }
                .firstOrError(),
            BiFunction { subscriptions: List<Subscription>, trade: CoinifyTrade ->
                return@BiFunction subscriptions.first { it.id == subscriptionId } to trade
            }
        ).doOnSubscribe { view.displayProgressDialog() }
            .doAfterTerminate { view.dismissProgressDialog() }
            .subscribeBy(
                onSuccess = {
                    val subscription = it.first
                    val trade = it.second

                    val calendar = Calendar.getInstance()
                        .apply { time = trade.createTime.fromIso8601()!! }
                    val dayOfWeek = calendar.getDisplayName(
                        Calendar.DAY_OF_WEEK,
                        Calendar.LONG,
                        Locale.getDefault()
                    )
                    val dayOfMonth = calendar.getDisplayName(
                        Calendar.DAY_OF_WEEK_IN_MONTH,
                        Calendar.LONG,
                        Locale.getDefault()
                    )
                    val frequencyString = when (subscription.frequency) {
                        BuyFrequency.Daily -> stringUtils.getString(R.string.buy_sell_recurring_frequency_daily)
                        BuyFrequency.Weekly -> stringUtils.getFormattedString(
                            R.string.buy_sell_recurring_frequency_weekly,
                            dayOfWeek
                        )
                        BuyFrequency.Monthly -> stringUtils.getFormattedString(
                            R.string.buy_sell_recurring_frequency_monthly,
                            dayOfMonth
                        )
                    }

                    val amount = trade.inAmount
                    val fee = trade.transferIn.getFee().toBigDecimal()
                        .setScale(2, RoundingMode.UP).toPlainString()
                    val currency = trade.transferIn.currency.toUpperCase()

                    var dateString =
                        subscription.endTime?.fromIso8601()?.toFormattedString(
                            view.locale
                        )
                    if (dateString != null) {
                        dateString += "${stringUtils.getString(R.string.buy_sell_recurring_order_duration_until)} " +
                            "$dateString"
                    }
                    val displayModel = RecurringTradeDisplayModel(
                        amountString = stringUtils.getFormattedString(
                            R.string.buy_sell_recurring_order_amount,
                            amount,
                            currency,
                            currency,
                            fee
                        ),
                        frequencyString = frequencyString,
                        durationStringToFormat = stringUtils.getString(
                            R.string.buy_sell_recurring_order_duration_until_cancelled
                        ),
                        duration = dateString
                            ?: stringUtils.getString(R.string.buy_sell_recurring_order_duration_you_cancelled)
                    )

                    view.launchRecurringTradeDetail(displayModel)
                },
                onError = { Timber.e(it) }
            )
    }

    private fun getAwaitingFundsModel(coinifyTrade: CoinifyTrade): AwaitingFundsModel {
        val (referenceText, account, bank, holder, _, _) = coinifyTrade.transferIn.details as BankDetails
        val formattedAmount = formatFiatWithSymbol(
            coinifyTrade.transferIn.sendAmount,
            coinifyTrade.transferIn.currency,
            view.locale
        )

        return AwaitingFundsModel(
            coinifyTrade.id,
            formattedAmount,
            referenceText,
            holder.name,
            holder.address.getFormattedAddressString(),
            account.number,
            account.bic,
            "${bank.name}, ${bank.address.getFormattedAddressString()}"
        )
    }

    private fun checkKycStatus() {
        Single.zip(
            kycReviewsSingle,
            traderSellLimitSingle,
            BiFunction { kycReviews: List<KycResponse>, sellLimit: Pair<Double, String> -> kycReviews to sellLimit }
        ).subscribeBy(
            onSuccess = { (kycReviews, sellLimits) ->
                if (kycReviews.kycUnverified()) {
                    val limitString =
                        formatFiatWithSymbol(sellLimits.first, sellLimits.second, view.locale)
                    val statusCard: KycStatus? = when {
                    // Unlikely to see this result - after supplying docs status will be pending
                    // otherwise we will go straight to overview
                        kycReviews.any { it.state == ReviewState.Reviewing } -> KycStatus.InReview(limitString)
                        kycReviews.any { it.state == ReviewState.Pending } ||
                            kycReviews.any { it.state == ReviewState.DocumentsRequested } ->
                            KycStatus.NotYetCompleted(limitString)
                        kycReviews.any { it.state == ReviewState.Failed } ||
                            kycReviews.any { it.state == ReviewState.Rejected } ||
                            kycReviews.any { it.state == ReviewState.Expired } -> KycStatus.Denied(
                            limitString
                        )
                        else -> null
                    }

                    statusCard?.let {
                        displayList.add(0, it)
                        view.renderViewState(OverViewState.Data(displayList.toList()))
                    }
                }
            },
            onError = { Timber.e(it) }
        )
    }

    private fun checkSubscriptionStatus() {
        Single.zip(
            recurringBuySingle,
            tradesSingle
                .filter { it.tradeSubscriptionId != null }
                .toList(),
            // Returns a pair of subscription objects with the first trade created by the subscription
            // This is so that we can calculate the start date
            BiFunction { subscriptions: List<Subscription>, trades: List<CoinifyTrade> ->
                val list = mutableListOf<Pair<Subscription, CoinifyTrade>>()
                for (sub in subscriptions) {
                    val coinifyTrade = trades.firstOrNull { sub.id == it.tradeSubscriptionId }
                    if (coinifyTrade != null) {
                        list.add(sub to coinifyTrade)
                    }
                }

                return@BiFunction list.toList()
            }
        ).subscribeBy(
            onSuccess = {
                if (!it.isEmpty()) {
                    it.map { (subscription, trade) ->

                        val calendar = Calendar.getInstance()
                            .apply { time = trade.createTime.fromIso8601()!! }
                        val dayOfWeek = calendar.getDisplayName(
                            Calendar.DAY_OF_WEEK,
                            Calendar.LONG,
                            Locale.getDefault()
                        )
                        val dayOfMonth = calendar.getDisplayName(
                            Calendar.DAY_OF_WEEK_IN_MONTH,
                            Calendar.LONG,
                            Locale.getDefault()
                        )
                        val displayString = when (subscription.frequency) {
                            BuyFrequency.Daily -> stringUtils.getString(R.string.buy_sell_overview_subscription_daily)
                            BuyFrequency.Weekly -> stringUtils.getFormattedString(
                                R.string.buy_sell_overview_subscription_weekly,
                                dayOfWeek
                            )
                            BuyFrequency.Monthly -> stringUtils.getFormattedString(
                                R.string.buy_sell_overview_subscription_monthly,
                                dayOfMonth
                            )
                        }

                        return@map RecurringBuyOrder(displayString, subscription.id)
                    }.run { displayList.addAll(1, this) }
                    view.renderViewState(OverViewState.Data(displayList.toList()))
                }
            },
            onError = { Timber.e(it) }
        )
    }

    private fun renderTrades(trades: List<BuySellTransaction>) {
        displayList.removeAll { it is BuySellTransaction || it is EmptyTransactionList }
        displayList.apply { addAll(trades) }
            .apply {
                if (trades.isEmpty()) {
                    add(empty)
                } else {
                    removeAll { it is EmptyTransactionList }
                }
            }
        view.renderViewState(OverViewState.Data(displayList.toList()))
    }

    @StringRes
    private fun tradeStateToStringRes(state: TradeState): Int = when (state) {
        TradeState.AwaitingTransferIn -> R.string.buy_sell_state_awaiting_funds
        TradeState.Completed, TradeState.CompletedTest -> R.string.buy_sell_state_completed
        TradeState.Cancelled -> R.string.buy_sell_state_cancelled
        TradeState.Rejected -> R.string.buy_sell_state_rejected
        TradeState.Expired -> R.string.buy_sell_state_expired
        TradeState.Processing, TradeState.Reviewing -> R.string.buy_sell_state_processing
    }

    private fun updateMetadataAsNeeded(trades: List<CoinifyTrade>) {
        exchangeService.getExchangeMetaData()
            .map {
                val list = it.coinify!!.trades ?: mutableListOf()
                for (tradeData in list) {
                    val coinifyTrade = trades.firstOrNull { it.id == tradeData.id }
                    // Here we update the stored metadata state if necessary
                    if (coinifyTrade != null && tradeData.state != coinifyTrade.state.toString()) {
                        tradeData.state = coinifyTrade.state.toString()
                    }
                }
                // Here we remove any transactions that are failed from metadata, as we aren't interested in them
                list.removeAll { it.isFailureState() }
                it.coinify!!.trades = list
                return@map it
            }
            .flatMapCompletable {
                metadataManager.saveToMetadata(
                    it.toSerialisedString(),
                    ExchangeService.METADATA_TYPE_EXCHANGE
                )
            }
            .subscribeBy(onError = { Timber.e(it) })
    }

    // region Model helper functions
    private fun mapTradeToDisplayObject(coinifyTrade: CoinifyTrade): BuySellTransaction {
        val displayString = if (coinifyTrade.isSellTransaction()) {
            "-${coinifyTrade.inAmount} ${coinifyTrade.inCurrency.capitalize()}"
        } else {
            val amount = coinifyTrade.transferOut.receiveAmount
            "+$amount ${coinifyTrade.outCurrency.capitalize()}"
        }

        return BuySellTransaction(
            transactionId = coinifyTrade.id,
            time = coinifyTrade.createTime.fromIso8601()!!,
            displayAmount = displayString,
            tradeStateString = if (coinifyTrade.isAwaitingCardPayment()) {
                R.string.buy_sell_state_pending_buy
            } else {
                tradeStateToStringRes(coinifyTrade.state)
            },
            tradeState = coinifyTrade.state,
            isSellTransaction = coinifyTrade.isSellTransaction()
        )
    }

    private fun getBuySellDetailsModel(coinifyTrade: CoinifyTrade): BuySellDetailsModel {
        // Title
        val stateString = stringUtils.getString(tradeStateToStringRes(coinifyTrade.state))
        val titleStringRes =
            if (coinifyTrade.isSellTransaction()) {
                R.string.buy_sell_detail_title_sell
            } else {
                R.string.buy_sell_detail_title_buy
            }
        val titleString = if (coinifyTrade.isAwaitingCardPayment()) {
            stringUtils.getString(R.string.buy_sell_state_pending_buy)
        } else {
            stringUtils.getFormattedString(titleStringRes, stateString)
        }
        // Date
        val time = coinifyTrade.updateTime.fromIso8601() ?: Date()
        val calendar = Calendar.getInstance()
        val timeZone = calendar.timeZone
        val offset = timeZone.getOffset(time.time)
        val offsetTime = time.time + offset
        val dateString = Date(offsetTime).toFormattedString(view.locale)
        // Amounts
        val sent = coinifyTrade.transferIn.receiveAmount
        val sentWithFee = coinifyTrade.transferIn.sendAmount
        val received = coinifyTrade.transferOut.sendAmount
        val receivedWithFee = coinifyTrade.transferOut.receiveAmount
        val sellPaymentFee = coinifyTrade.transferOut.getFee()
        val paymentFee = coinifyTrade.transferIn.getFee().toBigDecimal()
            .setScale(8, RoundingMode.HALF_UP)
            .abs()
            .stripTrailingZeros()
        // Currency
        val receiveCurrency = coinifyTrade.transferOut.currency.capitalize()
        val sendCurrency = coinifyTrade.transferIn.currency.capitalize()
        val isEndState = coinifyTrade.state.isEndState()
        // Model Strings
        val headlineAmount: String
        val detailAmount: String
        val paymentFeeString: String
        val receiveTitleString: String
        val amountString: String
        val totalString: String

        if (!coinifyTrade.isSellTransaction()) {
            // Crypto out (from Coinify's perspective)
            headlineAmount = "$receivedWithFee $receiveCurrency"
            detailAmount = "$receivedWithFee $receiveCurrency"
            // Fiat in
            amountString = formatFiatWithSymbol(sent, sendCurrency, view.locale)
            paymentFeeString =
                formatFiatWithSymbol(paymentFee.toDouble(), sendCurrency, view.locale)
            totalString = formatFiatWithSymbol(sentWithFee, sendCurrency, view.locale)
            // Received/Sold title
            receiveTitleString = getReceiveTitleString(
                isEndState,
                R.string.buy_sell_detail_currency_to_be_received,
                R.string.buy_sell_detail_currency_received,
                receiveCurrency
            )
        } else {
            // Fiat out (from Coinify's perspective)
            headlineAmount =
                formatFiatWithSymbol(received - sellPaymentFee, receiveCurrency, view.locale)
            detailAmount = "$sent $sendCurrency"
            // Crypto in
            paymentFeeString = "Not rendered"
            totalString = "Not rendered"
            amountString = "Not rendered"
            // Received/Sold title
            receiveTitleString = getReceiveTitleString(
                isEndState,
                R.string.buy_sell_detail_currency_to_be_sold,
                R.string.buy_sell_detail_currency_sold,
                sendCurrency
            )
        }

        return BuySellDetailsModel(
            coinifyTrade.isSellTransaction(),
            coinifyTrade.isAwaitingCardPayment(),
            titleString,
            headlineAmount,
            detailAmount,
            dateString,
            "#${coinifyTrade.id}",
            coinifyTrade.id,
            receiveTitleString,
            amountString,
            paymentFeeString,
            totalString
        )
    }
    // endregion

    // region Formatting helpers
    private fun formatFiatWithSymbol(
        fiatValue: Double,
        currencyCode: String,
        locale: Locale
    ): String {
        val numberFormat = NumberFormat.getCurrencyInstance(locale)
        val decimalFormatSymbols = (numberFormat as DecimalFormat).decimalFormatSymbols
        numberFormat.decimalFormatSymbols = decimalFormatSymbols.apply {
            this.currencySymbol = Currency.getInstance(currencyCode).getSymbol(locale)
        }
        return numberFormat.format(fiatValue)
    }

    private fun getReceiveTitleString(
        isEndState: Boolean,
        @StringRes pendingString: Int,
        @StringRes completeString: Int,
        currencyCode: String
    ): String = stringUtils.getFormattedString(
        if (isEndState) completeString else pendingString,
        currencyCode.capitalize()
    )
    // endregion

    // region Extension functions
    private fun List<KycResponse>.kycUnverified(): Boolean =
        this.none { it.state == ReviewState.Completed }

    private fun CoinifyTrade.isAwaitingTransferIn(): Boolean =
        (!this.isSellTransaction() &&
            this.state == TradeState.AwaitingTransferIn &&
            this.transferIn.medium == Medium.Bank)

    private fun CoinifyTrade.isAwaitingCardPayment(): Boolean =
        (!this.isSellTransaction() &&
            this.state == TradeState.AwaitingTransferIn &&
            this.transferIn.medium == Medium.Card)

    /**
     * See https://github.com/blockchain/bitcoin-exchange-client/blob/master/src/trade.js#L318
     */
    private fun TradeData.isFailureState(): Boolean =
        this.state == "cancelled" || this.state == "rejected" || this.state == "expired"
    // endregion
}

sealed class OverViewState {

    object Loading : OverViewState()
    class Failure(@StringRes val message: Int) : OverViewState()
    class Data(val items: List<BuySellDisplayable>) : OverViewState()
}
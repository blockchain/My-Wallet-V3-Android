package piuk.blockchain.android.ui.buysell.createorder

import com.crashlytics.android.answers.AddToCartEvent
import com.crashlytics.android.answers.StartCheckoutEvent
import info.blockchain.api.data.UnspentOutputs
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.FiatValue
import info.blockchain.utils.parseBigDecimal
import info.blockchain.utils.sanitiseEmptyNumber
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.payload.data.Account
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.datamanagers.FeeDataManager
import piuk.blockchain.androidcore.data.payments.SendDataManager
import piuk.blockchain.android.ui.buysell.createorder.models.BuyConfirmationDisplayModel
import piuk.blockchain.android.ui.buysell.createorder.models.OrderType
import piuk.blockchain.android.ui.buysell.createorder.models.ParcelableQuote
import piuk.blockchain.android.ui.buysell.createorder.models.SellConfirmationDisplayModel
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.coinify.ForcedDelay
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.LimitInAmounts
import piuk.blockchain.androidbuysell.models.coinify.LimitsExceeded
import piuk.blockchain.androidbuysell.models.coinify.Medium
import piuk.blockchain.androidbuysell.models.coinify.PaymentMethod
import piuk.blockchain.androidbuysell.models.coinify.Quote
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.models.coinify.TradeInProgress
import piuk.blockchain.androidbuysell.models.coinify.Trader
import piuk.blockchain.androidbuysell.services.ExchangeService
import com.blockchain.nabu.extensions.fromIso8601
import piuk.blockchain.androidcore.data.currency.BTCDenomination
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.absoluteValue
import kotlin.properties.Delegates

class BuySellBuildOrderPresenter @Inject constructor(
    private val coinifyDataManager: CoinifyDataManager,
    private val sendDataManager: SendDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val exchangeService: ExchangeService,
    private val currencyFormatManager: CurrencyFormatManager,
    private val feeDataManager: FeeDataManager,
    private val dynamicFeeCache: DynamicFeeCache,
    private val exchangeRateDataManager: ExchangeRateDataManager,
    private val stringUtils: StringUtils
) : BasePresenter<BuySellBuildOrderView>() {

    val receiveSubject: PublishSubject<String> = PublishSubject.create<String>()
    val sendSubject: PublishSubject<String> = PublishSubject.create<String>()
    var account by Delegates.observable(payloadDataManager.defaultAccount) { _, old, new ->
        if (old != new) {
            view.updateAccountSelector(new.label)
            if (isSell) loadMax(new)
        }
    }
    var selectedCurrency: String by Delegates.observable("EUR") { _, old, new ->
        if (old != new) initialiseUi(); subscribeToSubjects()
    }
    private var latestQuote: Quote? = null
    private var latestLoadedLimits: LimitInAmounts? = null
    private var feeOptions: FeeOptions? = null
    private var maximumInAmounts: Double = 0.0
    private var minimumInAmount: Double = 0.0
    // This value is in the user's default currency
    private var maximumInCardAmount: Double = 0.0
    // The user's max spendable bitcoin
    private var maxBitcoinAmount: BigDecimal = BigDecimal.ZERO
    // The inbound fee - this varies depending on InMedium being bank, card or blockchain
    private var inPercentageFee: Double = 0.0
    // The outbound fee - this is applicable only for sell, as Coinify charge a little for the bank transfer
    private var outPercentageFee: Double = 0.0
    // The outbound fee - ie for Buying, this is the BTC cost of the transaction. This will be
    // zero if Selling, as fee is on our side, not Coinify's.
    private var outFixedFee: Double = 0.0
    private var defaultCurrency: String = "EUR"
    private var initialLoad = true
    // For comparison to avoid double logging
    private var lastLog: LogItem? = null

    private val fiatFormat by unsafeLazy {
        (NumberFormat.getInstance(view.locale) as DecimalFormat).apply {
            maximumFractionDigits = 2
            minimumFractionDigits = 2
        }
    }

    private val emptyQuote
        get() = Quote(null, selectedCurrency, "BTC", 0.0, 0.0, "", "")

    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .addToCompositeDisposable(this)
            .subscribeOn(Schedulers.io())
            .singleOrError()
            .doOnError { view.onFatalError() }
            .map { it.coinify!!.token }

    private val inMediumSingle: Single<Medium>
        get() = when (view.orderType) {
            OrderType.Sell -> Single.just(Medium.Blockchain)
            OrderType.BuyCard -> Single.just(Medium.Card)
            OrderType.BuyBank -> Single.just(Medium.Bank)
            OrderType.Buy -> tokenSingle
                .flatMap { coinifyDataManager.getKycReviews(it) }
                // Here we assume Bank payment as it has higher limits unless KYC pending
                .map { if (it.hasPendingKyc()) Medium.Card else Medium.Bank }
        }

    private val isSell: Boolean
        get() = view.orderType == OrderType.Sell

    override fun onViewReady() {
        // Display Accounts selector if necessary
        if (payloadDataManager.accounts.size > 1) {
            view.displayAccountSelector(account.label)
        }

        initialiseUi()
        subscribeToSubjects()
        loadMax(account)
    }

    internal fun onMaxClicked() {
        if (isSell) {
            val maxAmount = when {
                maxBitcoinAmount < maximumInAmounts.toBigDecimal() -> maxBitcoinAmount
                else -> maximumInAmounts.toBigDecimal()
            }
            view.updateReceiveAmount(maxAmount.toString())
        } else {
            view.updateSendAmount(maximumInAmounts.toString())
        }
    }

    internal fun onMinClicked() {
        val updateAmount = minimumInAmount.toString()
        if (isSell) {
            view.requestReceiveFocus()
            view.updateReceiveAmount(updateAmount)
        } else {
            view.requestSendFocus()
            view.updateSendAmount(updateAmount)
        }
    }

    internal fun onConfirmClicked() {
        require(latestQuote != null) { "Latest quote is null" }
        val lastQuote = latestQuote!!

        val currencyToSend = when {
            lastQuote.baseAmount < 0 -> lastQuote.baseCurrency
            else -> lastQuote.quoteCurrency
        }
        val currencyToReceive = when {
            lastQuote.quoteAmount < 0 -> lastQuote.baseCurrency
            else -> lastQuote.quoteCurrency
        }
        val amountToSend = when {
            lastQuote.baseAmount < 0 -> lastQuote.baseAmount
            else -> lastQuote.quoteAmount
        }.absoluteValue
        val amountToReceive = when {
            lastQuote.baseAmount < 0 -> lastQuote.quoteAmount
            else -> lastQuote.baseAmount
        }.absoluteValue
        val paymentFeeBuy = (amountToSend * (inPercentageFee / 100)).toBigDecimal()
        val paymentFeeSell = (amountToReceive * (outPercentageFee / 100)).toBigDecimal()

        Logging.logStartCheckout(
            StartCheckoutEvent()
                .putCustomAttribute("currency", currencyToSend.toUpperCase())
                .putTotalPrice(amountToSend.absoluteValue.toBigDecimal())
                .putItemCount(1)
        )

        if (!isSell) {
            getBuyDetails(
                lastQuote,
                currencyToSend,
                currencyToReceive,
                amountToSend,
                amountToReceive,
                paymentFeeBuy
            )
        } else {
            getSellDetails(
                amountToSend,
                currencyToSend,
                currencyToReceive,
                amountToReceive,
                lastQuote,
                paymentFeeSell
            )
        }
    }

    private fun getBuyDetails(
        lastQuote: Quote,
        currencyToSend: String,
        currencyToReceive: String,
        amountToSend: Double,
        amountToReceive: Double,
        paymentFeeBuy: BigDecimal
    ) {
        val quote = BuyConfirmationDisplayModel(
            currencyToSend = currencyToSend,
            currencyToReceive = currencyToReceive,
            amountToSend = currencyFormatManager.getFormattedFiatValueWithSymbol(
                amountToSend,
                currencyToSend,
                view.locale
            ),
            amountToReceive = amountToReceive,
            orderFee = outFixedFee.unaryMinus()
                .toBigDecimal()
                .setScale(8, RoundingMode.UP)
                .sanitise(),
            paymentFee = currencyFormatManager.getFormattedFiatValueWithSymbol(
                paymentFeeBuy.toDouble(),
                currencyToSend,
                view.locale
            ),
            totalAmountToReceiveFormatted =
            (amountToReceive.toBigDecimal() - outFixedFee.absoluteValue.toBigDecimal()).sanitise(),
            totalCostFormatted = currencyFormatManager.getFormattedFiatValueWithSymbol(
                (amountToSend.toBigDecimal() + paymentFeeBuy).toDouble(),
                currencyToSend,
                view.locale
            ),
            // Include the original quote to avoid converting directions back again
            originalQuote = ParcelableQuote.fromQuote(lastQuote),
            isHigherThanCardLimit = amountToSend.toBigDecimal() > getLocalisedCardLimit(),
            localisedCardLimit = getLocalisedCardLimitString(),
            cardLimit = getLocalisedCardLimit().toDouble(),
            accountIndex = payloadDataManager.accounts.indexOf(account)
        )

        view.startOrderConfirmation(view.orderType, quote)
    }

    private fun getSellDetails(
        amountToSend: Double,
        currencyToSend: String,
        currencyToReceive: String,
        amountToReceive: Double,
        lastQuote: Quote,
        paymentFeeSell: BigDecimal
    ) {
        val satoshis = BigDecimal.valueOf(amountToSend)
            .multiply(BigDecimal.valueOf(1e8))
            .toBigInteger()

        val xPub = account.xpub

        tokenSingle
            .applySchedulers()
            .addToCompositeDisposable(this)
            .flatMap {
                coinifyDataManager.getBankAccounts(it)
                    .flatMap { accounts ->
                        getFeeForTransaction(
                            xPub,
                            satoshis,
                            feeOptions!!.regularFee.toBigInteger()
                        ).map { (accounts.isEmpty()) to it }
                    }
            }
            .doOnSubscribe { view.showProgressDialog() }
            .doOnEvent { _, _ -> view.dismissProgressDialog() }
            .subscribeBy(
                onSuccess = {
                    val noAccounts = it.first
                    val fee = it.second.toBigDecimal().divide(1e8.toBigDecimal())
                        .setScale(8, RoundingMode.UP)
                    val totalCost = amountToSend.toBigDecimal().plus(fee)
                        .setScale(8, RoundingMode.UP)
                        .sanitise()

                    val displayModel = SellConfirmationDisplayModel(
                        currencyToSend = currencyToSend,
                        currencyToReceive = currencyToReceive,
                        amountToSend = amountToSend,
                        amountToReceive = amountToReceive,
                        networkFee = fee.sanitise(),
                        accountIndex = payloadDataManager.accounts.indexOf(account),
                        originalQuote = ParcelableQuote.fromQuote(lastQuote),
                        totalAmountToReceiveFormatted = currencyFormatManager.getFormattedFiatValueWithSymbol(
                            amountToReceive - paymentFeeSell.toDouble(),
                            currencyToReceive,
                            view.locale
                        ),
                        totalCostFormatted = totalCost,
                        amountInSatoshis = satoshis,
                        feePerKb = feeOptions!!.regularFee.toBigInteger(),
                        absoluteFeeInSatoshis = it.second,
                        paymentFee = currencyFormatManager.getFormattedFiatValueWithSymbol(
                            paymentFeeSell.toDouble(),
                            currencyToReceive,
                            view.locale
                        )
                    )

                    if (noAccounts) {
                        view.launchAddNewBankAccount(displayModel)
                    } else {
                        view.launchBankAccountSelection(displayModel)
                    }
                },
                onError = {
                    Timber.e(it)
                    view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                }
            )
    }

    private fun subscribeToSubjects() {
        sendSubject.applyDefaults()
            .flatMapSingle { amount ->
                tokenSingle.flatMap {
                    coinifyDataManager.getQuote(
                        it,
                        if (isSell) amount else amount.unaryMinus(),
                        selectedCurrency,
                        "BTC"
                    ).doOnSuccess { latestQuote = it }
                        .onErrorReturn { emptyQuote }
                        .doAfterSuccess { view.showQuoteInProgress(false) }
                }
            }
            .doOnNext { updateExchangeRate(it) }
            .doOnNext { updateReceiveAmount(it.quoteAmount.absoluteValue) }
            .doOnNext { updateSendAmount(it.baseAmount.absoluteValue) }
            .doOnNext { compareToLimits(it) }
            .doOnNext {
                val currency = if (isSell) it.quoteCurrency else it.baseCurrency
                val amount = if (isSell) it.quoteAmount else it.baseAmount
                val itemName = if (isSell) it.baseCurrency else it.quoteCurrency
                val itemType = if (isSell) Logging.ITEM_TYPE_FIAT else Logging.ITEM_TYPE_CRYPTO
                logAddToCart(currency, amount, itemName, itemType)
            }
            .subscribeBy(onError = { setUnknownErrorState(it) })

        receiveSubject.applyDefaults()
            .flatMapSingle { amount ->
                tokenSingle.flatMap {
                    coinifyDataManager.getQuote(
                        it,
                        if (isSell) amount.unaryMinus() else amount,
                        "BTC",
                        selectedCurrency
                    ).doOnSuccess { latestQuote = it }
                        .onErrorReturn { emptyQuote }
                        .doAfterSuccess { view.showQuoteInProgress(false) }
                }
            }
            .doOnNext { updateExchangeRate(it) }
            .doOnNext { updateSendAmount(it.quoteAmount.absoluteValue) }
            .doOnNext { updateReceiveAmount(it.baseAmount.absoluteValue) }
            .doOnNext { compareToLimits(it) }
            .doOnNext {
                val currency = if (isSell) it.baseCurrency else it.quoteCurrency
                val amount = if (isSell) it.baseAmount else it.quoteAmount
                val itemName = if (isSell) it.quoteCurrency else it.baseCurrency
                val itemType = if (isSell) Logging.ITEM_TYPE_FIAT else Logging.ITEM_TYPE_CRYPTO
                logAddToCart(currency, amount, itemName, itemType)
            }
            .subscribeBy(onError = { setUnknownErrorState(it) })
    }

    private fun updateExchangeRate(quote: Quote) {
        val currency = if (quote.isBtcBase()) quote.quoteCurrency else quote.baseCurrency
        val numerator = if (quote.isBtcBase()) quote.quoteAmount.absoluteValue else quote.baseAmount.absoluteValue
        val denominator = if (quote.isBtcBase()) quote.baseAmount.absoluteValue else quote.quoteAmount.absoluteValue
        currencyFormatManager.getFormattedFiatValueWithSymbol(
            numerator / denominator,
            currency,
            view.locale
        ).run { view.renderExchangeRate(ExchangeRateStatus.Data("@ $this")) }
    }

    private fun compareToLimits(quote: Quote) {
        val amountToSend = (if (quote.baseAmount >= 0) quote.quoteAmount else quote.baseAmount)
            .absoluteValue.toBigDecimal()

        val orderType = view.orderType
        // Attempting to sell more bitcoin than you have
        if (orderType == OrderType.Sell && amountToSend > maxBitcoinAmount) {
            view.setButtonEnabled(false)
            view.renderLimitStatus(
                LimitStatus.ErrorTooHigh(
                    R.string.buy_sell_not_enough_bitcoin,
                    amountToSend.toPlainString()
                )
            )
            // Attempting to buy less than is allowed
        } else if (!isSell && amountToSend < minimumInAmount.toBigDecimal()) {
            view.setButtonEnabled(false)
            view.renderLimitStatus(
                LimitStatus.ErrorTooLow(
                    R.string.buy_sell_amount_too_low,
                    "${fiatFormat.format(minimumInAmount)} $selectedCurrency"
                )
            )
            // Attempting to buy more than allowed via Bank
        } else if (orderType == OrderType.Buy && amountToSend > maximumInAmounts.toBigDecimal()) {
            view.setButtonEnabled(false)
            view.renderLimitStatus(
                LimitStatus.ErrorTooHigh(
                    R.string.buy_sell_remaining_buy_limit,
                    "${fiatFormat.format(maximumInAmounts)} $selectedCurrency"
                )
            )
            // Attempting to buy more than allowed via Card
        } else if (orderType == OrderType.BuyCard && amountToSend > maximumInAmounts.toBigDecimal()) {
            view.setButtonEnabled(false)
            view.renderLimitStatus(
                LimitStatus.ErrorTooHigh(
                    R.string.buy_sell_remaining_buy_limit,
                    "${fiatFormat.format(maximumInAmounts)} $selectedCurrency"
                )
            )
            // Attempting to sell more than allowed
        } else if (isSell && amountToSend > maximumInAmounts.toBigDecimal()) {
            view.setButtonEnabled(false)
            view.renderLimitStatus(
                LimitStatus.ErrorTooHigh(
                    R.string.buy_sell_remaining_sell_limit,
                    "${fiatFormat.format(maximumInAmounts)} $selectedCurrency"
                )
            )
            // Attempting to sell less than allowed
        } else if (isSell && amountToSend < minimumInAmount.toBigDecimal()) {
            view.setButtonEnabled(false)
            view.renderLimitStatus(
                LimitStatus.ErrorTooLow(
                    R.string.buy_sell_remaining_sell_minimum_limit,
                    "$minimumInAmount BTC"
                )
            )
            // All good, reload previously stated limits
        } else {
            view.setButtonEnabled(true)
            renderLimits(latestLoadedLimits!!)
        }
    }

    private fun loadMax(account: Account) {
        fetchFeesObservable()
            .flatMap { getBtcMaxObservable(account) }
            .doOnError { Timber.e(it) }
            .subscribeBy(
                onNext = { maxBitcoinAmount = it },
                onError = {
                    view.showToast(
                        R.string.buy_sell_error_fetching_limit,
                        ToastCustom.TYPE_ERROR
                    )
                }
            )
    }

    private fun initialiseUi() {
        // Get quote for value of 1 BTC for UI using default currency
        tokenSingle
            .doOnSubscribe { view.renderSpinnerStatus(SpinnerStatus.Loading) }
            .flatMapObservable { token ->
                Observable.zip(
                    coinifyDataManager.getTrader(token)
                        .toObservable(),
                    inMediumSingle.toObservable(),
                    BiFunction<Trader, Medium, Pair<Trader, Medium>> { trader, inMedium ->
                        return@BiFunction trader to inMedium
                    }
                ).flatMap { (trader, inMedium) ->
                    val currency =
                        if (initialLoad) getDefaultCurrency(trader.defaultCurrency) else selectedCurrency

                    getExchangeRate(token, if (isSell) -1.0 else 1.0, currency)
                        .toObservable()
                        .doOnNext {
                            maximumInCardAmount = trader.level.limits.card.inLimits.daily
                        }
                        .flatMap { getPaymentMethods(token, inMedium).toObservable() }
                        .doOnNext { defaultCurrency = getDefaultCurrency(trader.defaultCurrency) }
                        .doOnNext {
                            if (initialLoad) {
                                selectCurrencies(it, inMedium, defaultCurrency)
                                initialLoad = false
                            }

                            inPercentageFee = it.inPercentageFee
                            outPercentageFee = it.outPercentageFee
                            outFixedFee = it.outFixedFees.btc

                            minimumInAmount = if (view.orderType == OrderType.Sell) {
                                it.minimumInAmounts.getLimitsForCurrency("btc")
                            } else {
                                it.minimumInAmounts.getLimitsForCurrency(selectedCurrency)
                            }
                            maximumInAmounts = if (view.orderType == OrderType.Sell) {
                                it.limitInAmounts.getLimitsForCurrency("btc")
                            } else {
                                it.limitInAmounts.getLimitsForCurrency(selectedCurrency)
                            }
                        }
                        .doOnNext { renderLimits(it.limitInAmounts) }
                        .doOnNext { checkIfCanTrade(it) }
                }
            }
            .subscribeBy(
                onError = {
                    Timber.e(it)
                    view.onFatalError()
                }
            )
    }

    private fun getDefaultCurrency(userDefaultCurrency: String): String = if (!isSell) {
        userDefaultCurrency
    } else {
        if (userDefaultCurrency.equals("usd", ignoreCase = true)) {
            defaultCurrency
        } else {
            userDefaultCurrency
        }
    }

    private fun updateReceiveAmount(quoteAmount: Double) {
        val formatted = currencyFormatManager
            .getFormattedBchValue(BigDecimal.valueOf(quoteAmount), BTCDenomination.BTC)
        view.updateReceiveAmount(formatted)
    }

    private fun updateSendAmount(quoteAmount: Double) {
        val formatted = FiatValue.fromMajor(selectedCurrency, quoteAmount.toBigDecimal())
            .toStringWithSymbol(view.locale)
        view.updateSendAmount(formatted)
    }

    private fun setUnknownErrorState(throwable: Throwable) {
        Timber.e(throwable)
        view.clearEditTexts()
        view.setButtonEnabled(false)
        view.showToast(R.string.buy_sell_error_fetching_quote, ToastCustom.TYPE_ERROR)
    }

    private fun getPaymentMethods(token: String, inMedium: Medium): Single<PaymentMethod> =
        coinifyDataManager.getPaymentMethods(token)
            .filter { it.inMedium == inMedium }
            .firstOrError()

    private fun selectCurrencies(
        paymentMethod: PaymentMethod,
        inMedium: Medium,
        userCurrency: String
    ) {
        val currencies = when (inMedium) {
            Medium.Blockchain -> paymentMethod.outCurrencies.toMutableList() // Sell
            else -> paymentMethod.inCurrencies.toMutableList() // Buy
        }

        // Selling USD is not allowed
        if (isSell) currencies.remove("USD")

        selectedCurrency = if (currencies.contains(userCurrency)) {
            val index = currencies.indexOf(userCurrency)
            currencies.removeAt(index)
            currencies.add(0, userCurrency)
            userCurrency
        } else {
            currencies[0]
        }

        view.renderSpinnerStatus(SpinnerStatus.Data(currencies))
    }

    private fun renderLimits(limits: LimitInAmounts) {
        latestLoadedLimits = limits

        val limitAmount = when (view.orderType) {
            OrderType.Sell -> {
                val max = if (maxBitcoinAmount < limits.btc!!.toBigDecimal()) {
                    maxBitcoinAmount
                } else {
                    limits.btc!!.toBigDecimal()
                }
                "$max BTC"
            }
            OrderType.BuyCard, OrderType.BuyBank, OrderType.Buy -> "${fiatFormat.format(
                maximumInAmounts
            )} $selectedCurrency"
        }

        val descriptionString = when (view.orderType) {
            OrderType.Buy, OrderType.BuyCard, OrderType.BuyBank -> R.string.buy_sell_remaining_buy_limit
            OrderType.Sell -> R.string.buy_sell_sell_bitcoin_max
        }

        view.renderLimitStatus(LimitStatus.Data(descriptionString, limitAmount))
    }

    private fun checkIfCanTrade(paymentMethod: PaymentMethod) {
        if (!paymentMethod.canTrade) {
            val reason = paymentMethod.cannotTradeReasons!!.first()
            when (reason) {
                is ForcedDelay -> renderWaitTime(reason.delayEnd)
                is TradeInProgress ->
                    view.displayFatalErrorDialog(stringUtils.getString(R.string.buy_sell_error_trade_in_progress))
                is LimitsExceeded ->
                    view.displayFatalErrorDialog(stringUtils.getString(R.string.buy_sell_error_limits_exceeded))
            }
            view.setButtonEnabled(false)
        }
    }

    private fun renderWaitTime(delayEnd: String) {
        val expiryDateUtc = delayEnd.fromIso8601()
        val calendar = Calendar.getInstance()
        val timeZone = calendar.timeZone
        val offset = timeZone.getOffset(expiryDateUtc!!.time)

        val endTimeLong = expiryDateUtc.time + offset
        val remaining = (endTimeLong - System.currentTimeMillis()) / 1000
        var hours = TimeUnit.SECONDS.toHours(remaining)
        if (hours == 0L) hours = 1L

        val readableTime = String.format("%2d", hours)
        val formattedString =
            stringUtils.getFormattedString(R.string.buy_sell_error_forced_delay, readableTime)

        view.displayFatalErrorDialog(formattedString)
    }

    private fun getLocalisedCardLimitString(): String {
        val limit = getLocalisedCardLimit()
        return "$limit $selectedCurrency"
    }

    private fun getLocalisedCardLimit(): BigDecimal {
        val exchangeRateSelected = getExchangeRate(selectedCurrency)
        val exchangeRateDefault = getExchangeRate(defaultCurrency)
        val rate = exchangeRateSelected.div(exchangeRateDefault)
        return rate.multiply(maximumInCardAmount.toBigDecimal()).setScale(2, RoundingMode.DOWN)
    }

    private fun getExchangeRate(currencyCode: String) =
        exchangeRateDataManager.getLastPrice(CryptoCurrency.BTC, currencyCode).toBigDecimal()

    // region Observables
    private fun getExchangeRate(token: String, amount: Double, currency: String): Single<Quote> =
        coinifyDataManager.getQuote(token, amount, "BTC", currency)
            .doOnSuccess {
                val valueWithSymbol =
                    currencyFormatManager.getFormattedFiatValueWithSymbol(
                        it.quoteAmount.absoluteValue,
                        it.quoteCurrency,
                        view.locale
                    )

                view.renderExchangeRate(ExchangeRateStatus.Data("@ $valueWithSymbol"))
            }
            .doOnError { view.renderExchangeRate(ExchangeRateStatus.Failed) }

    private fun PublishSubject<String>.applyDefaults(): Observable<Double> = this.doOnNext {
        view.setButtonEnabled(false)
        view.showQuoteInProgress(true)
    }.debounce(2000, TimeUnit.MILLISECONDS)
        // Here we kill any quotes in flight already, as they take up to ten seconds to fulfill
        .doOnNext { compositeDisposable.clear() }
        // Strip out localised information for predictable formatting
        .map { it.sanitiseEmptyNumber().parseBigDecimal(view.locale) }
        // Logging
        .doOnError(Timber::wtf)
        // Return zero if empty or some other error
        .onErrorReturn { BigDecimal.ZERO }
        // Scheduling for UI updates if necessary
        .observeOn(AndroidSchedulers.mainThread())
        // If zero, clear all EditTexts and reset UI state
        .doOnNext {
            if (it <= BigDecimal.ZERO) {
                view.clearEditTexts()
                view.setButtonEnabled(false)
                view.showQuoteInProgress(false)
            }
        }
        // Don't pass zero events to the API as they're invalid
        .filter { it > BigDecimal.ZERO }
        // To double, as API requires it
        .map { it.toDouble() }
        // Prevents focus issues
        .distinctUntilChanged()
    // endregion

    // region Extension Functions
    private fun List<KycResponse>.hasPendingKyc(): Boolean = this.any { it.state.isProcessing() } &&
        this.none { it.state == ReviewState.Completed }

    private fun BigDecimal.sanitise() = this.stripTrailingZeros().toPlainString()

    private fun Quote.isBtcBase() = baseCurrency.equals("btc", ignoreCase = true)
    // endregion

    // region Bitcoin helpers
    private fun getFeeForTransaction(
        xPub: String,
        amountToSend: BigInteger,
        feePerKb: BigInteger
    ): Single<BigInteger> =
        getUnspentApiResponseBtc(xPub)
            .map { getSuggestedAbsoluteFee(it, amountToSend, feePerKb) }
            .singleOrError()

    private fun getSuggestedAbsoluteFee(
        coins: UnspentOutputs,
        amountToSend: BigInteger,
        feePerKb: BigInteger
    ): BigInteger {
        val spendableCoins = sendDataManager.getSpendableCoins(coins, amountToSend, feePerKb)
        return spendableCoins.absoluteFee
    }

    private fun getBtcMaxObservable(account: Account): Observable<BigDecimal> =
        getUnspentApiResponseBtc(account.xpub)
            .addToCompositeDisposable(this)
            .map { unspentOutputs ->
                val sweepBundle = sendDataManager.getMaximumAvailable(
                    unspentOutputs,
                    BigInteger.valueOf(feeOptions!!.regularFee * 1000)
                )
                val sweepableAmount =
                    BigDecimal(sweepBundle.left).divide(BigDecimal.valueOf(1e8))
                return@map sweepableAmount to BigDecimal(sweepBundle.right)
                    .divide(BigDecimal.valueOf(1e8))
            }
            .flatMap { Observable.just(it.first) }
            .onErrorReturn { BigDecimal.ZERO }

    private fun fetchFeesObservable(): Observable<FeeOptions> = feeDataManager.btcFeeOptions
        .doOnSubscribe { feeOptions = dynamicFeeCache.btcFeeOptions!! }
        .doOnNext { dynamicFeeCache.btcFeeOptions = it }

    private fun getUnspentApiResponseBtc(address: String): Observable<UnspentOutputs> {
        return if (payloadDataManager.getAddressBalance(address).toLong() > 0) {
            sendDataManager.getUnspentOutputs(address)
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }
    // endregion

    private fun logAddToCart(
        currency: String,
        amount: Double,
        itemName: String,
        itemType: String
    ) {
        val newLogItem = LogItem(currency, amount, itemName, itemType)
        if (lastLog != newLogItem) {
            // Prevents double logging, as both Observables will be triggered by new data and call this function
            lastLog = newLogItem
            Logging.logAddToCart(
                AddToCartEvent()
                    .putCustomAttribute("currency", currency.toUpperCase())
                    .putItemPrice(amount.absoluteValue.toBigDecimal())
                    .putItemName(itemName.toUpperCase())
                    .putItemType(itemType)
            )
        }
    }

    private data class LogItem(
        val currency: String,
        val amount: Double,
        val itemName: String,
        val itemType: String
    )

    sealed class ExchangeRateStatus {
        object Loading : ExchangeRateStatus()
        data class Data(val formattedQuote: String) : ExchangeRateStatus()
        object Failed : ExchangeRateStatus()
    }

    sealed class SpinnerStatus {
        object Loading : SpinnerStatus()
        data class Data(val currencies: List<String>) : SpinnerStatus()
        object Failure : SpinnerStatus()
    }

    sealed class LimitStatus {
        object Loading : LimitStatus()
        data class Data(val textResourceId: Int, val limit: String) : LimitStatus()
        data class ErrorTooLow(val textResourceId: Int, val limit: String) : LimitStatus()
        data class ErrorTooHigh(val textResourceId: Int, val limit: String) : LimitStatus()
        object Failure : LimitStatus()
    }
}
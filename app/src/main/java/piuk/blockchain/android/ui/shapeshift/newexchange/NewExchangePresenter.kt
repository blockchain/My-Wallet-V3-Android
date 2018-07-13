package piuk.blockchain.android.ui.shapeshift.newexchange

import info.blockchain.api.data.UnspentOutputs
import info.blockchain.wallet.api.data.FeeOptions
import info.blockchain.wallet.coin.GenericMetadataAccount
import info.blockchain.wallet.payload.data.Account
import info.blockchain.wallet.shapeshift.data.MarketInfo
import info.blockchain.wallet.shapeshift.data.Quote
import info.blockchain.wallet.shapeshift.data.QuoteRequest
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.cache.DynamicFeeCache
import piuk.blockchain.android.data.datamanagers.FeeDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.payments.SendDataManager
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.shapeshift.models.ShapeShiftData
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.currency.CryptoCurrency
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.settings.SettingsDataManager
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.data.shapeshift.models.CoinPairings
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsDataManager
import piuk.blockchain.androidcore.utils.Either
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.ParseException
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class NewExchangePresenter @Inject constructor(
    private val payloadDataManager: PayloadDataManager,
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val sendDataManager: SendDataManager,
    private val dynamicFeeCache: DynamicFeeCache,
    private val feeDataManager: FeeDataManager,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val shapeShiftDataManager: ShapeShiftDataManager,
    private val stringUtils: StringUtils,
    private val settingsDataManager: SettingsDataManager,
    private val buyDataManager: BuyDataManager,
    private val walletOptionsDataManager: WalletOptionsDataManager,
    private val currencyFormatManager: CurrencyFormatManager,
    walletAccountHelper: WalletAccountHelper
) : BasePresenter<NewExchangeView>() {

    internal val toCryptoSubject: PublishSubject<String> = PublishSubject.create<String>()
    internal val fromCryptoSubject: PublishSubject<String> = PublishSubject.create<String>()
    internal val toFiatSubject: PublishSubject<String> = PublishSubject.create<String>()
    internal val fromFiatSubject: PublishSubject<String> = PublishSubject.create<String>()

    private val btcAccounts = walletAccountHelper.getHdAccounts()
    private val bchAccounts = walletAccountHelper.getHdBchAccounts()
    private val cryptoFormat by unsafeLazy {
        (NumberFormat.getInstance(view.locale) as DecimalFormat).apply {
            minimumFractionDigits = 1
            maximumFractionDigits = 8
        }
    }
    private var marketInfo: MarketInfo? = null
    private var shapeShiftData: ShapeShiftData? = null
    private var latestQuote: Quote? = null
    private var account: Account? = null
    private var bchAccount: GenericMetadataAccount? = null
    private var feeOptions: FeeOptions? = null
    private var fromCurrency = CryptoCurrency.BTC
    private var toCurrency = CryptoCurrency.ETHER

    override fun onViewReady() {
        view.updateUi(
            fromCurrency,
            toCurrency,
            getCurrencyLabel(fromCurrency),
            getCurrencyLabel(toCurrency),
            currencyFormatManager.getFormattedFiatValueWithSymbol(0.0)
        )

        val shapeShiftObservable = getMarketInfoObservable(fromCurrency, toCurrency)
        val feesObservable = fetchFeesObservable(fromCurrency)

        shapeShiftObservable
            .doOnNext { marketInfo = it }
            .flatMap { feesObservable }
            .doOnSubscribe { view.showProgressDialog(R.string.shapeshift_getting_information) }
            .doOnTerminate { view.dismissProgressDialog() }
            .addToCompositeDisposable(this)
            .doOnError { Timber.e(it) }
            .subscribeBy(
                onComplete = {
                    // Only set account the first time
                    if (account == null) account = payloadDataManager.defaultAccount
                    if (bchAccount == null) bchAccount =
                        bchDataManager.getDefaultGenericMetadataAccount()
                    checkForEmptyBalances()
                },
                onError = {
                    view.showToast(
                        R.string.shapeshift_getting_information_failed,
                        ToastCustom.TYPE_ERROR
                    )
                    view.finishPage()
                }
            )

        subscribeToEditTexts()
    }

    internal fun onSwitchCurrencyClicked() {
        val currentFromCurrencyState = fromCurrency
        val currentToCurrencyState = toCurrency
        toCurrency = currentFromCurrencyState
        fromCurrency = currentToCurrencyState
        compositeDisposable.clear()
            .run { view.clearEditTexts() }
            .run { view.clearError() }
            .run {
                // This is a bit hacky and should be abstracted out when more currencies are
                // available, hence the null checks in onViewReady().
                onViewReady()
            }
    }

    internal fun onContinuePressed() {
        // State check
        if (shapeShiftData == null) {
            view.showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR)
            return
        }

        // Check user isn't submitting an empty page
        if (shapeShiftData?.withdrawalAmount?.compareTo(BigDecimal.ZERO) == 0) {
            view.showToast(R.string.invalid_amount, ToastCustom.TYPE_ERROR)
            return
        }

        // It's possible that the fee observable can return zero but not kill the chain with an
        // error, hence checking here
        if (shapeShiftData?.networkFee?.compareTo(BigDecimal.ZERO) == 0) {
            view.showToast(R.string.shapeshift_getting_fees_failed, ToastCustom.TYPE_ERROR)
            return
        }

        getMaxCurrencyObservable()
            .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
            .doOnNext {
                val amount = it.setScale(8, RoundingMode.HALF_DOWN)
                if (amount < shapeShiftData!!.depositAmount) {
                    view.dismissProgressDialog()
                    // Show warning, inform user
                    view.showAmountError(stringUtils.getString(R.string.insufficient_funds))

                    Timber.d("Attempted to send ${shapeShiftData!!.depositAmount} but max available was $amount")
                } else {
                    sendFinalRequest(fromCurrency, toCurrency)
                }
            }
            .subscribeBy(
                onError = { setUnknownErrorState(it) }
            )
    }

    internal fun onMaxPressed() {
        view.removeAllFocus()
        view.showQuoteInProgress(true)
        getMaxCurrencyObservable().subscribeBy(
            onNext = {
                // 'it' can be zero here if amounts insufficient
                if (getMinimum() > it) {
                    view.showAmountError(
                        stringUtils.getFormattedString(
                            R.string.shapeshift_amount_to_low,
                            getMinimum(),
                            fromCurrency.symbol.toUpperCase()
                        )
                    )
                    view.showQuoteInProgress(false)
                } else {
                    fromCryptoSubject.onNext(cryptoFormat.format(it))
                    // This is a bit of a hack to bypass focus issues
                    view.updateFromCryptoText(cryptoFormat.format(it))
                }
            },
            onError = { setUnknownErrorState(it) }
        )
    }

    internal fun onMinPressed() {
        view.removeAllFocus()
        view.showQuoteInProgress(true)

        getMaxCurrencyObservable()
            .subscribeBy(
                onNext = {
                    if (getMinimum() > it) {
                        view.showAmountError(
                            stringUtils.getFormattedString(
                                R.string.shapeshift_amount_to_low,
                                getMinimum(),
                                fromCurrency.symbol.toUpperCase()
                            )
                        )
                        view.showQuoteInProgress(false)
                    } else {
                        with(getMinimum()) {
                            fromCryptoSubject.onNext(cryptoFormat.format(this))
                            // This is a bit of a hack to bypass focus issues
                            view.updateFromCryptoText(cryptoFormat.format(this))
                        }
                    }
                },
                onError = { setUnknownErrorState(it) }
            )
    }

    internal fun onFromChooserClicked() = view.launchAccountChooserActivityFrom()

    internal fun onToChooserClicked() = view.launchAccountChooserActivityTo()

    internal fun onFromEthSelected() {
        fromCurrency = CryptoCurrency.ETHER
        // Here we prevent users selecting to and from as the same currency. Default to BTC
        if (fromCurrency == toCurrency) toCurrency = CryptoCurrency.BTC
        view.clearEditTexts()
        onViewReady()
    }

    internal fun onToEthSelected() {
        toCurrency = CryptoCurrency.ETHER
        // Here we prevent users selecting to and from as the same currency. Default to BTC
        if (fromCurrency == toCurrency) fromCurrency = CryptoCurrency.BTC
        view.clearEditTexts()
        onViewReady()
    }

    internal fun onFromAccountChanged(account: Account) {
        fromCurrency = CryptoCurrency.BTC
        // Here we prevent users selecting to and from as the same currency. Default to ETH
        if (fromCurrency == toCurrency) toCurrency = CryptoCurrency.ETHER
        this.account = account
        view.clearEditTexts()
        onViewReady()
    }

    internal fun onToAccountChanged(account: Account) {
        toCurrency = CryptoCurrency.BTC
        // Here we prevent users selecting to and from as the same currency. Default to ETH
        if (fromCurrency == toCurrency) fromCurrency = CryptoCurrency.ETHER
        this.account = account
        view.clearEditTexts()
        onViewReady()
    }

    fun onFromBchAccountChanged(account: GenericMetadataAccount) {
        fromCurrency = CryptoCurrency.BCH
        // Here we prevent users selecting to and from as the same currency. Default to BTC
        if (fromCurrency == toCurrency) toCurrency = CryptoCurrency.BTC
        this.bchAccount = account
        view.clearEditTexts()
        onViewReady()
    }

    fun onToBchAccountChanged(account: GenericMetadataAccount) {
        toCurrency = CryptoCurrency.BCH
        // Here we prevent users selecting to and from as the same currency. Default to BTC
        if (fromCurrency == toCurrency) fromCurrency = CryptoCurrency.BTC
        this.bchAccount = account
        view.clearEditTexts()
        onViewReady()
    }

    private fun checkForEmptyBalances() {
        hasEmptyBalances()
            .addToCompositeDisposable(this)
            .applySchedulers()
            .doOnSubscribe { view.showProgressDialog(R.string.please_wait) }
            .doOnTerminate { view.dismissProgressDialog() }
            .flatMap { empty ->
                if (empty) buyDataManager.canBuy else Observable.empty<Boolean>()
            }
            .subscribeBy(
                onNext = { canBuy -> view.showNoFunds(canBuy && view.isBuyPermitted) },
                onError = { Timber.e(it) }
            )
    }

    private fun subscribeToEditTexts() {
        fromCryptoSubject.applyDefaults()
            // Update to Fiat as it's not dependent on web results
            .doOnNext { updateFromFiat(it) }
            // Update results dependent on Shapeshift
            .flatMap { amount ->
                getQuoteFromRequest(amount, fromCurrency, toCurrency)
                    .doOnNext { updateToFields(it.withdrawalAmount) }
            }
            .subscribeBy(
                onError = { setUnknownErrorState(it) }
            )

        fromFiatSubject.applyDefaults()
            // Convert to fromCrypto amount
            .map {
                val (_, toExchangeRate) = getExchangeRates(
                    currencyFormatManager.fiatCountryCode,
                    toCurrency,
                    fromCurrency
                )
                return@map it.divide(toExchangeRate, 18, RoundingMode.HALF_UP)
            }
            // Update from amount view
            .doOnNext { view.updateFromCryptoText(cryptoFormat.format(it)) }
            // Update results dependent on Shapeshift
            .flatMap { amount ->
                getQuoteFromRequest(amount, fromCurrency, toCurrency)
                    .doOnNext { updateToFields(it.withdrawalAmount) }
            }
            .subscribeBy(
                onError = { setUnknownErrorState(it) }
            )
        toCryptoSubject.applyDefaults()
            // Update to Fiat as it's not dependent on web results
            .doOnNext { updateToFiat(it) }
            // Update results dependent on Shapeshift
            .flatMap { amount ->
                getQuoteToRequest(amount, fromCurrency, toCurrency)
                    .doOnNext { updateFromFields(it.depositAmount) }
            }
            .subscribeBy(
                onError = { setUnknownErrorState(it) }
            )

        toFiatSubject.applyDefaults()
            // Convert to toCrypto amount
            .map {
                val (fromExchangeRate, _) = getExchangeRates(
                    currencyFormatManager.fiatCountryCode,
                    toCurrency,
                    fromCurrency
                )
                return@map it.divide(fromExchangeRate, 18, RoundingMode.HALF_UP)
            }
            // Update from amount view
            .doOnNext { view.updateToCryptoText(cryptoFormat.format(it)) }
            // Update results dependent on Shapeshift
            .flatMap { amount ->
                getQuoteToRequest(amount, fromCurrency, toCurrency)
                    .doOnNext { updateFromFields(it.depositAmount) }
            }
            .subscribeBy(
                onError = { setUnknownErrorState(it) }
            )
    }

    private fun setUnknownErrorState(throwable: Throwable) {
        Timber.e(throwable)
        view.clearEditTexts()
        view.setButtonEnabled(false)
        view.showToast(R.string.shapeshift_getting_information_failed, ToastCustom.TYPE_ERROR)
    }

    private fun getExchangeRates(
        currencyCode: String,
        toCurrency: CryptoCurrency,
        fromCurrency: CryptoCurrency
    ): ExchangeRates = ExchangeRates(
        getExchangeRate(toCurrency, currencyCode),
        getExchangeRate(fromCurrency, currencyCode)
    )

    private fun getExchangeRate(
        cryptoCurrency: CryptoCurrency,
        currencyCode: String
    ): BigDecimal {
        val price = when (cryptoCurrency) {
            CryptoCurrency.BTC -> exchangeRateFactory.getLastBtcPrice(currencyCode)
            CryptoCurrency.ETHER -> exchangeRateFactory.getLastEthPrice(currencyCode)
            CryptoCurrency.BCH -> exchangeRateFactory.getLastBchPrice(currencyCode)
        }

        return BigDecimal.valueOf(price)
    }

    private fun getUnspentApiResponseBtc(address: String): Observable<UnspentOutputs> {
        return if (payloadDataManager.getAddressBalance(address).toLong() > 0) {
            sendDataManager.getUnspentOutputs(address)
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    private fun getUnspentApiResponseBch(address: String): Observable<UnspentOutputs> {
        return if (bchDataManager.getAddressBalance(address).toLong() > 0) {
            sendDataManager.getUnspentBchOutputs(address)
        } else {
            Observable.error(Throwable("No funds - skipping call to unspent API"))
        }
    }

    private fun getBtcLabel() = if (btcAccounts.size > 1) {
        if (account != null) {
            account!!.label
        } else {
            payloadDataManager.defaultAccount.label
        }
    } else {
        stringUtils.getString(R.string.shapeshift_btc)
    }

    private fun getEthLabel() = if (btcAccounts.size > 1) {
        stringUtils.getString(R.string.eth_default_account_label)
    } else {
        stringUtils.getString(R.string.shapeshift_eth)
    }

    private fun getBchLabel() = if (bchAccounts.size > 1) {
        if (bchAccount != null) {
            bchAccount!!.label
        } else {
            bchDataManager.getDefaultGenericMetadataAccount()!!.label
        }
    } else {
        stringUtils.getString(R.string.shapeshift_bch)
    }

    private fun getCurrencyLabel(currency: CryptoCurrency) = when (currency) {
        CryptoCurrency.BTC -> getBtcLabel()
        CryptoCurrency.ETHER -> getEthLabel()
        CryptoCurrency.BCH -> getBchLabel()
    }

    private fun getShapeShiftPair(fromCurrency: CryptoCurrency, toCurrency: CryptoCurrency) =
        CoinPairings.getPair(fromCurrency, toCurrency)

    private fun getMaximum() = marketInfo?.maxLimit ?: BigDecimal.ZERO

    private fun getMinimum() = marketInfo?.minimum ?: BigDecimal.ZERO

    // region Field Updates
    private fun updateFromFiat(amount: BigDecimal) {
        view.updateFromFiatText(
            currencyFormatManager.getFormattedFiatValueWithSymbol(
                amount.multiply(
                    getExchangeRates(
                        currencyFormatManager.fiatCountryCode,
                        toCurrency,
                        fromCurrency
                    ).fromRate
                ).toDouble()
            )
        )
    }

    private fun updateToFiat(amount: BigDecimal) {
        view.updateToFiatText(
            currencyFormatManager.getFormattedFiatValueWithSymbol(
                amount.multiply(
                    getExchangeRates(
                        currencyFormatManager.fiatCountryCode,
                        toCurrency,
                        fromCurrency
                    ).toRate
                ).toDouble()
            )
        )
    }

    private fun updateToFields(toAmount: BigDecimal) {
        val amount = toAmount.max(BigDecimal.ZERO)
        view.updateToCryptoText(cryptoFormat.format(amount))
        updateToFiat(amount)
    }

    private fun updateFromFields(fromAmount: BigDecimal) {
        val amount = fromAmount.max(BigDecimal.ZERO)
        view.updateFromCryptoText(cryptoFormat.format(amount))
        updateFromFiat(amount)
    }
    // endregion

    // region Observables
    /**
     * Sends a complete [QuoteRequest] object to ShapeShift and sends all of the required fields
     * serialized to the next Activity.
     */
    private fun sendFinalRequest(fromCurrency: CryptoCurrency, toCurrency: CryptoCurrency) {
        val quoteRequest = QuoteRequest().apply {
            with(shapeShiftData!!) {
                this@apply.depositAmount = depositAmount
                this@apply.withdrawalAmount = withdrawalAmount
                this@apply.withdrawal = withdrawalAddress
                this@apply.pair = getShapeShiftPair(fromCurrency, toCurrency).pairCode
                this@apply.returnAddress = shapeShiftData!!.returnAddress
                this@apply.apiKey = view.shapeShiftApiKey
            }
        }
        // Update quote with final data
        getQuoteObservable(quoteRequest, fromCurrency, toCurrency)
            .doOnTerminate { view.dismissProgressDialog() }
            .addToCompositeDisposable(this)
            .subscribeBy(
                onNext = { view.launchConfirmationPage(shapeShiftData!!) },
                onError = {
                    view.showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR)
                }
            )
    }

    private fun getQuoteFromRequest(
        fromAmount: BigDecimal,
        fromCurrency: CryptoCurrency,
        toCurrency: CryptoCurrency
    ): Observable<Quote> {
        val quoteRequest = QuoteRequest().apply {
            depositAmount = fromAmount.setScale(8, RoundingMode.HALF_DOWN)
            pair = getShapeShiftPair(fromCurrency, toCurrency).pairCode
            apiKey = view.shapeShiftApiKey
        }

        return getQuoteObservable(quoteRequest, fromCurrency, toCurrency)
    }

    private fun getQuoteToRequest(
        toAmount: BigDecimal,
        fromCurrency: CryptoCurrency,
        toCurrency: CryptoCurrency
    ): Observable<Quote> {
        val quoteRequest = QuoteRequest().apply {
            withdrawalAmount = toAmount.setScale(8, RoundingMode.HALF_DOWN)
            pair = getShapeShiftPair(fromCurrency, toCurrency).pairCode
            apiKey = view.shapeShiftApiKey
        }

        return getQuoteObservable(quoteRequest, fromCurrency, toCurrency)
    }

    private fun fetchFeesObservable(selectedCurrency: CryptoCurrency) = when (selectedCurrency) {
        CryptoCurrency.BTC -> feeDataManager.btcFeeOptions
            .doOnSubscribe { feeOptions = dynamicFeeCache.btcFeeOptions!! }
            .doOnNext { dynamicFeeCache.btcFeeOptions = it }

        CryptoCurrency.ETHER -> feeDataManager.ethFeeOptions
            .doOnSubscribe { feeOptions = dynamicFeeCache.ethFeeOptions!! }
            .doOnNext { dynamicFeeCache.ethFeeOptions = it }

        CryptoCurrency.BCH -> feeDataManager.bchFeeOptions
            .doOnSubscribe { feeOptions = dynamicFeeCache.bchFeeOptions!! }
            .doOnNext { dynamicFeeCache.bchFeeOptions = it }
    }

    private fun getMarketInfoObservable(
        fromCurrency: CryptoCurrency,
        toCurrency: CryptoCurrency
    ): Observable<MarketInfo> =
        shapeShiftDataManager.getRate(CoinPairings.getPair(fromCurrency, toCurrency))

    private fun getQuoteObservable(
        quoteRequest: QuoteRequest,
        fromCurrency: CryptoCurrency,
        toCurrency: CryptoCurrency
    ): Observable<Quote> =
    // Get quote for Quote Request
        shapeShiftDataManager.getQuote(quoteRequest)
            .addToCompositeDisposable(this)
            .map {
                when (it) {
                    is Either.Right<Quote> -> return@map it.value
                    is Either.Left<String> -> {
                        // Show error in UI, fallback to initial quote rates
                        view.showAmountError(it.value)
                        return@map Quote().apply {
                            orderId = ""
                            quotedRate = marketInfo?.rate ?: BigDecimal.ONE
                            minerFee = marketInfo?.minerFee ?: BigDecimal.ZERO
                            withdrawalAmount = quoteRequest.withdrawalAmount ?: BigDecimal.ZERO
                            depositAmount = quoteRequest.depositAmount ?: BigDecimal.ZERO
                            expiration = 0L
                        }
                    }
                }
            }
            .flatMap { quote ->
                // Get fee for the proposed payment amount
                getFeeForPayment(quote.depositAmount, fromCurrency)
                    .flatMap { fee ->
                        // Get receive/change address pair
                        getAddressPair(fromCurrency, toCurrency)
                            .map { addresses ->
                                latestQuote = quote
                                // Update ShapeShift Data
                                shapeShiftData = ShapeShiftData(
                                    orderId = quote.orderId,
                                    fromCurrency = fromCurrency,
                                    toCurrency = toCurrency,
                                    depositAmount = quote.depositAmount
                                        ?: BigDecimal.ZERO,
                                    changeAddress = addresses.changeAddress,
                                    depositAddress = quote.deposit ?: "",
                                    withdrawalAmount = quote.withdrawalAmount
                                        ?: BigDecimal.ZERO,
                                    withdrawalAddress = addresses.withdrawalAddress,
                                    exchangeRate = quote.quotedRate,
                                    transactionFee = fee,
                                    networkFee = quote.minerFee,
                                    returnAddress = addresses.returnAddress,
                                    xPub = account!!.xpub,
                                    expiration = quote.expiration,
                                    gasLimit = BigInteger.valueOf(
                                        feeOptions?.gasLimit ?: 0L
                                    ),
                                    gasPrice = BigInteger.valueOf(
                                        feeOptions?.regularFee ?: 0L
                                    ),
                                    feePerKb = BigInteger.valueOf(
                                        when (fromCurrency) {
                                            CryptoCurrency.BTC, CryptoCurrency.BCH -> feeOptions?.priorityFee
                                                ?: 0 * 1000L
                                            CryptoCurrency.ETHER -> 0L
                                        }
                                    )
                                )

                                return@map quote
                            }
                    }
                    .doOnError { setUnknownErrorState(it) }
            }
            .doAfterNext {
                view.showQuoteInProgress(false)
                view.setButtonEnabled(true)
            }
            .doOnError { setUnknownErrorState(it) }
            .doOnError { Timber.e(it) }

    private fun hasEmptyBalances(): Observable<Boolean> =
        Observable.zip(
            getBtcMaxObservable(),
            getEthMaxObservable(),
            getBchMaxObservable(),
            Function3 { btc, eth, bch ->
                btc == BigDecimal.ZERO &&
                    eth == BigDecimal.ZERO &&
                    bch == BigDecimal.ZERO
            }
        )

    // region Fees Observables
    private fun getFeeForPayment(
        amountToSend: BigDecimal,
        selectedCurrency: CryptoCurrency
    ): Observable<BigInteger> = when (selectedCurrency) {
        CryptoCurrency.BTC -> getFeeForBtcPaymentObservable(
            amountToSend,
            BigInteger.valueOf(feeOptions!!.priorityFee * 1000)
        )
        CryptoCurrency.ETHER -> getFeeForEthPaymentObservable()
        CryptoCurrency.BCH -> getFeeForBchPaymentObservable(
            amountToSend,
            BigInteger.valueOf(feeOptions!!.priorityFee * 1000)
        )
    }.doOnError { view.showToast(R.string.confirm_payment_fee_sync_error, ToastCustom.TYPE_ERROR) }

    /**
     * Returns the ETH fee in Wei
     */
    private fun getFeeForEthPaymentObservable(): Observable<BigInteger> {
        val gwei = BigDecimal.valueOf(feeOptions!!.gasLimit * feeOptions!!.regularFee)
        val feeInGwei = Convert.toWei(gwei, Convert.Unit.GWEI)
        return Observable.just(feeInGwei.toBigInteger())
    }

    /**
     * Returns the BTC fee in Satoshis
     */
    private fun getFeeForBtcPaymentObservable(
        amountToSend: BigDecimal,
        feePerKb: BigInteger
    ): Observable<BigInteger> = getUnspentApiResponseBtc(account!!.xpub)
        .addToCompositeDisposable(this)
        .map {
            val satoshis = amountToSend.multiply(BigDecimal.valueOf(100000000))
            return@map sendDataManager.getSpendableCoins(
                it,
                satoshis.toBigInteger(),
                feePerKb
            ).absoluteFee
        }

    private fun getFeeForBchPaymentObservable(
        amountToSend: BigDecimal,
        feePerKb: BigInteger
    ): Observable<BigInteger> = getUnspentApiResponseBch(bchAccount!!.xpub)
        .addToCompositeDisposable(this)
        .map {
            val satoshis = amountToSend.multiply(BigDecimal.valueOf(100000000))
            return@map sendDataManager.getSpendableCoins(
                it,
                satoshis.toBigInteger(),
                feePerKb
            ).absoluteFee
        }
    // endregion

    // region Address Pair Observables
    private fun getAddressPair(
        fromCurrency: CryptoCurrency,
        toCurrency: CryptoCurrency
    ): Observable<Addresses> = Observable.zip(
        getReceiveAddress(fromCurrency),
        getWithdrawalAddress(toCurrency),
        getChangeAddress(fromCurrency),
        Function3<String, String, String, Addresses> { receive, withdrawal, change ->
            Addresses(withdrawal, receive, change)
        })
        .doOnError {
            view.showToast(
                R.string.shapeshift_deriving_address_failed,
                ToastCustom.TYPE_ERROR
            )
        }

    private fun getReceiveAddress(cryptoCurrency: CryptoCurrency): Observable<String> =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> getBtcReceiveAddress()
            CryptoCurrency.ETHER -> getEthAddress()
            CryptoCurrency.BCH -> getBchReceiveAddress()
        }

    private fun getChangeAddress(cryptoCurrency: CryptoCurrency): Observable<String> =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> getBtcChangeAddress()
            CryptoCurrency.ETHER -> getEthAddress()
            CryptoCurrency.BCH -> getBchChangeAddress()
        }

    private fun getWithdrawalAddress(cryptoCurrency: CryptoCurrency): Observable<String> =
        when (cryptoCurrency) {
            CryptoCurrency.BTC -> getBtcReceiveAddress()
            CryptoCurrency.ETHER -> getEthAddress()
            CryptoCurrency.BCH -> getBchReceiveAddress()
        }

    private fun getEthAddress(): Observable<String> =
        Observable.just(ethDataManager.getEthWallet()!!.account.address)

    private fun getBtcReceiveAddress(): Observable<String> =
        payloadDataManager.getNextReceiveAddress(account!!)

    private fun getBtcChangeAddress(): Observable<String> =
        payloadDataManager.getNextChangeAddress(account!!)

    private fun getBchReceiveAddress(): Observable<String> {
        val position =
            bchDataManager.getActiveAccounts().indexOfFirst { it.xpub == bchAccount!!.xpub }
        return bchDataManager.getNextReceiveAddress(position)
    }

    private fun getBchChangeAddress(): Observable<String> {
        val position =
            bchDataManager.getActiveAccounts().indexOfFirst { it.xpub == bchAccount!!.xpub }
        return bchDataManager.getNextChangeAddress(position)
    }
    // endregion

    // region Max Amounts Observables
    private fun getMaxCurrencyObservable(): Observable<BigDecimal> =
        when (fromCurrency) {
            CryptoCurrency.BTC -> getBtcMaxObservable()
            CryptoCurrency.ETHER -> getEthMaxObservable()
            CryptoCurrency.BCH -> getBchMaxObservable()
        }.doOnError { Timber.e(it) }

    private fun getEthMaxObservable(): Observable<BigDecimal> = ethDataManager.fetchEthAddress()
        .addToCompositeDisposable(this)
        .map {
            val gwei = BigDecimal.valueOf(feeOptions!!.gasLimit * feeOptions!!.regularFee)
            val wei = Convert.toWei(gwei, Convert.Unit.GWEI)

            val addressResponse = it.getAddressResponse()
            val maxAvailable =
                addressResponse!!.balance!!.minus(wei.toBigInteger()).max(BigInteger.ZERO)

            val availableEth = Convert.fromWei(maxAvailable.toString(), Convert.Unit.ETHER)
            val amount = if (availableEth > getMaximum()) getMaximum() else availableEth
            return@map amount to Convert.fromWei(wei, Convert.Unit.ETHER)
        }
        .flatMap { (amount, fee) -> getRegionalMaxAmount(fee, amount) }
        .onErrorReturn { BigDecimal.ZERO }

    private fun getBtcMaxObservable(): Observable<BigDecimal> =
        getUnspentApiResponseBtc(account!!.xpub)
            .addToCompositeDisposable(this)
            .map { unspentOutputs ->
                val sweepBundle = sendDataManager.getMaximumAvailable(
                    unspentOutputs,
                    BigInteger.valueOf(feeOptions!!.priorityFee * 1000)
                )
                val sweepableAmount =
                    BigDecimal(sweepBundle.left).divide(BigDecimal.valueOf(1e8))
                val amount =
                    if (sweepableAmount > getMaximum()) getMaximum() else sweepableAmount
                return@map amount to BigDecimal(sweepBundle.right).divide(
                    BigDecimal.valueOf(1e8)
                )
            }
            .flatMap { (amount, fee) -> getRegionalMaxAmount(fee, amount) }
            .onErrorReturn { BigDecimal.ZERO }

    private fun getBchMaxObservable(): Observable<BigDecimal> =
        getUnspentApiResponseBch(bchAccount!!.xpub)
            .addToCompositeDisposable(this)
            .map { unspentOutputs ->
                val sweepBundle = sendDataManager.getMaximumAvailable(
                    unspentOutputs,
                    BigInteger.valueOf(feeOptions!!.priorityFee * 1000)
                )
                val sweepableAmount =
                    BigDecimal(sweepBundle.left).divide(BigDecimal.valueOf(1e8))
                val amount =
                    if (sweepableAmount > getMaximum()) getMaximum() else sweepableAmount
                return@map amount to BigDecimal(sweepBundle.right).divide(
                    BigDecimal.valueOf(1e8)
                )
            }
            .flatMap { (amount, fee) -> getRegionalMaxAmount(fee, amount) }
            .onErrorReturn { BigDecimal.ZERO }

    /**
     * If the amount passed to this function is greater than $500 or €500 (region dependent),
     * the function returns the amount of crypto equal to $500 or €500, minus the fee for sending
     * it to ShapeShift (therefore the total amount sent is equal to $500 or €500). If the amount
     * passed to the function is less than the limit, the amount is returned.
     *
     * @param fee The fee in Ether or BTC (no sub-units)
     * @param amount The amount to be checked against in Ether or BTC (no sub-units)
     * @return An [Observable] wrapping a [BigDecimal]
     */
    private fun getRegionalMaxAmount(fee: BigDecimal, amount: BigDecimal): Observable<BigDecimal> {
        return settingsDataManager.getSettings().map {
            val rate = when {
                it.countryCode == "US" -> getExchangeRates("USD", toCurrency, fromCurrency).fromRate
                else -> getExchangeRates("EUR", toCurrency, fromCurrency).fromRate
            }

            val limit = walletOptionsDataManager.getShapeShiftLimit().toBigDecimal()
            // Multiply to get fiat amount
            val fiatAmount = amount.multiply(rate)
            if (fiatAmount >= limit) {
                // Get crypto amount equal to 500 $ or €, then subtract fee
                return@map (limit.divide(rate, 8, RoundingMode.HALF_DOWN)).minus(fee)
            } else {
                return@map amount
            }
        }
    }
    // endregion

    private fun PublishSubject<String>.applyDefaults(): Observable<BigDecimal> = this.doOnNext {
        view.clearError()
        view.setButtonEnabled(false)
        view.showQuoteInProgress(true)
    }.debounce(1000, TimeUnit.MILLISECONDS)
        // Here we kill any quotes in flight already, as they take up to ten seconds to fulfill
        .doOnNext { compositeDisposable.clear() }
        // Strip out localised information for predictable formatting
        .map { it.sanitise().parse(view.locale) }
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
    // endregion

    // region Extension Functions
    private fun String.sanitise() = if (isNotEmpty()) this else "0"

    @Throws(ParseException::class)
    private fun String.parse(locale: Locale): BigDecimal {
        val format = NumberFormat.getNumberInstance(locale)
        if (format is DecimalFormat) {
            format.isParseBigDecimal = true
        }
        return format.parse(this.replace("[^\\d.,]".toRegex(), "")) as BigDecimal
    }
    // endregion

    private data class ExchangeRates(val toRate: BigDecimal, val fromRate: BigDecimal)

    private data class Addresses(
        val withdrawalAddress: String,
        val returnAddress: String,
        val changeAddress: String
    )
}
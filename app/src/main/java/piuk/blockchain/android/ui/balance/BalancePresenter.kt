package piuk.blockchain.android.ui.balance

import android.annotation.SuppressLint
import android.support.annotation.VisibleForTesting
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.ethereum.data.EthAddressResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.notifications.models.NotificationPayload
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.coinify.BlockchainDetails
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.currency.BTCDenomination
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.currency.ETHDenomination
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.ui.base.UiState
import timber.log.Timber
import java.math.BigDecimal
import javax.inject.Inject

class BalancePresenter @Inject constructor(
    private val exchangeRateDataManager: ExchangeRateDataManager,
    private val transactionListDataManager: TransactionListDataManager,
    private val ethDataManager: EthDataManager,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    internal val payloadDataManager: PayloadDataManager,
    private val buyDataManager: BuyDataManager,
    private val stringUtils: StringUtils,
    private val prefsUtil: PrefsUtil,
    private val rxBus: RxBus,
    private val currencyState: CurrencyState,
    private val shapeShiftDataManager: ShapeShiftDataManager,
    private val bchDataManager: BchDataManager,
    private val walletAccountHelper: WalletAccountHelper,
    private val environmentSettings: EnvironmentConfig,
    private val currencyFormatManager: CurrencyFormatManager,
    private val exchangeService: ExchangeService,
    private val coinifyDataManager: CoinifyDataManager
) : BasePresenter<BalanceView>() {

    @VisibleForTesting
    var notificationObservable: Observable<NotificationPayload>? = null
    @VisibleForTesting
    var authEventObservable: Observable<AuthEvent>? = null

    private var shortcutsGenerated = false
    private val tokenSingle: Single<String>
        get() = exchangeService.getExchangeMetaData()
            .cache()
            .addToCompositeDisposable(this)
            .applySchedulers()
            .singleOrError()
            .map { it.coinify!!.token }

    // region Life cycle
    override fun onViewReady() {
        onAccountsAdapterSetup()
        onTxFeedAdapterSetup()
        subscribeToEvents()
        if (environmentSettings.environment == Environment.TESTNET) {
            currencyState.cryptoCurrency = CryptoCurrency.BTC
            view.disableCurrencyHeader()
        }
    }

    override fun onViewDestroyed() {
        notificationObservable?.let { rxBus.unregister(NotificationPayload::class.java, it) }
        authEventObservable?.let { rxBus.unregister(AuthEvent::class.java, it) }
        super.onViewDestroyed()
    }

    private fun subscribeToEvents() {
        authEventObservable = rxBus.register(AuthEvent::class.java).apply {
            subscribe {
                // Clear tx feed
                view.updateTransactionDataSet(
                    currencyState.isDisplayingCryptoCurrency,
                    mutableListOf()
                )
                transactionListDataManager.clearTransactionList()
            }
        }

        notificationObservable = rxBus.register(NotificationPayload::class.java).apply {
            subscribe {
                // no-op
            }
        }
    }
    // endregion

    // region API calls
    /**
     * Do all API calls to reload page
     */
    @SuppressLint("VisibleForTests")
    private fun refreshAllCompletable(account: ItemAccount): Completable {
        return getUpdateTickerCompletable()
            .andThen(updateEthAddress())
//                .andThen(updateBchWallet())
            .andThen(updateTransactionsListCompletable(account))
            .andThen(updateBalancesCompletable().doOnComplete { refreshBalanceHeader(account) })
            .doOnError { view.setUiState(UiState.FAILURE) }
            .doOnSubscribe { view.setUiState(UiState.LOADING) }
            .doOnSubscribe { view.setDropdownVisibility(getAccounts().size > 1) }
            .doOnComplete {
                refreshAccountDataSet()
                if (!shortcutsGenerated) {
                    shortcutsGenerated = true
                    refreshLauncherShortcuts()
                }
                setViewType(currencyState.isDisplayingCryptoCurrency)
            }
    }

    /*
    onResume and Swipe down force refresh
     */
    internal fun onRefreshRequested() {
        refreshAllCompletable(getCurrentAccount())
            .doOnError { Timber.e(it) }
            .addToCompositeDisposable(this)
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )
    }

    @VisibleForTesting
    internal fun getUpdateTickerCompletable(): Completable {
        return exchangeRateDataManager.updateTickers()
    }

    /**
     * API call - Update eth address
     */

    @VisibleForTesting
    internal fun updateEthAddress() =
        Completable.fromObservable(ethDataManager.fetchEthAddress()
            .onExceptionResumeNext { Observable.empty<EthAddressResponse>() })

    /**
     * API call - Update bitcoincash wallet
     */

    @VisibleForTesting
    internal fun updateBchWallet() = bchDataManager.refreshMetadataCompletable()
        .doOnError { Timber.e(it) }

    /**
     * API call - Fetches latest balance for selected currency and updates UI balance
     */
    @VisibleForTesting
    internal fun updateBalancesCompletable() =
        when (currencyState.cryptoCurrency) {
            CryptoCurrency.BTC -> payloadDataManager.updateAllBalances()
            CryptoCurrency.ETHER -> ethDataManager.fetchEthAddressCompletable()
            CryptoCurrency.BCH -> bchDataManager.updateAllBalances()
            else -> throw IllegalArgumentException("${currencyState.cryptoCurrency.unit} is not currently supported")
        }

    /**
     * API call - Fetches latest transactions for selected currency and account, and updates UI tx list
     */
    @VisibleForTesting
    internal fun updateTransactionsListCompletable(account: ItemAccount): Completable {
        return Completable.fromObservable(
            transactionListDataManager.fetchTransactions(account, 50, 0)
                .doAfterTerminate(this::storeSwipeReceiveAddresses)
                .map { txs ->
                    Observable.zip(
                        getShapeShiftTxNotesObservable(),
                        getCoinifyTxNotesObservable(),
                        mergeReduce()
                    ).addToCompositeDisposable(this)
                        .subscribe(
                            { txNotesMap ->
                                for (tx in txs) {
                                    // Add shapeShift notes
                                    txNotesMap[tx.hash]?.let { tx.note = it }

                                    when (currencyState.cryptoCurrency) {
                                        CryptoCurrency.BTC -> {
                                            tx.totalDisplayableCrypto =
                                                getBtcBalanceString(
                                                    true,
                                                    tx.total.toLong()
                                                )
                                            tx.totalDisplayableFiat =
                                                getBtcBalanceString(
                                                    false,
                                                    tx.total.toLong()
                                                )
                                        }
                                        CryptoCurrency.ETHER -> {
                                            tx.totalDisplayableCrypto =
                                                getEthBalanceString(
                                                    true,
                                                    BigDecimal(tx.total)
                                                )
                                            tx.totalDisplayableFiat =
                                                getEthBalanceString(
                                                    false,
                                                    BigDecimal(tx.total)
                                                )
                                        }
                                        CryptoCurrency.BCH -> {
                                            tx.totalDisplayableCrypto =
                                                getBchBalanceString(
                                                    true,
                                                    tx.total.toLong()
                                                )
                                            tx.totalDisplayableFiat =
                                                getBchBalanceString(
                                                    false,
                                                    tx.total.toLong()
                                                )
                                        }
                                        else -> throw IllegalArgumentException(
                                            "${currencyState.cryptoCurrency.unit} is not currently supported"
                                        )
                                    }
                                }

                                when {
                                    txs.isEmpty() -> view.setUiState(UiState.EMPTY)
                                    else -> view.setUiState(UiState.CONTENT)
                                }

                                view.updateTransactionDataSet(
                                    currencyState.isDisplayingCryptoCurrency,
                                    txs
                                )
                            },
                            { Timber.e(it) })
                })
    }
    // endregion

    // region Incoming UI events
    /*
    Currency selected from dropdown
     */
    internal fun onCurrencySelected(cryptoCurrency: CryptoCurrency) {
        // Set new currency state
        currencyState.cryptoCurrency = cryptoCurrency

        // Select default account for this currency
        val accounts = getAccounts()
        val account = accounts[0]

        updateTransactionsListCompletable(account)
            .doOnSubscribe { view.setDropdownVisibility(accounts.size > 1) }
            .doOnSubscribe { view.setUiState(UiState.LOADING) }
            .doOnSubscribe { refreshBalanceHeader(account) }
            .doOnSubscribe { refreshAccountDataSet() }
            .addToCompositeDisposable(this)
            .doOnError { Timber.e(it) }
            .doOnComplete { view.selectDefaultAccount() }
            .subscribe(
                { /* No-op */ },
                { view.setUiState(UiState.FAILURE) })
    }

    internal fun onGetBitcoinClicked() {
        buyDataManager.canBuy
            .addToCompositeDisposable(this)
            .subscribe({
                if (it && view.shouldShowBuy()) {
                    view.startBuyActivity()
                } else {
                    view.startReceiveFragmentBtc()
                }
            }, { Timber.e(it) })
    }

    /*
    Fetch all active accounts for initial selected currency and set up account adapter
     */
    private fun onAccountsAdapterSetup() {
        view.setupAccountsAdapter(getAccounts())
    }

    internal fun onAccountSelected(position: Int) {
        val account = getAccounts()[position]

        updateTransactionsListCompletable(account)
            .doOnSubscribe { view.setUiState(UiState.LOADING) }
            .addToCompositeDisposable(this)
            .doOnError { Timber.e(it) }
            .doOnComplete {
                refreshBalanceHeader(account)
                refreshAccountDataSet()
            }
            .subscribe(
                { /* No-op */ },
                { view.setUiState(UiState.FAILURE) })
    }

    /*
    Set fiat or crypto currency state
     */
    internal fun setViewType(showCrypto: Boolean) {
        // Set new currency state
        currencyState.isDisplayingCryptoCurrency = showCrypto

        // Update balance header
        refreshBalanceHeader(getCurrentAccount())

        // Update tx list balances
        view.updateTransactionValueType(showCrypto)

        // Update accounts data set
        refreshAccountDataSet()
    }

    /*
    Toggle between fiat - crypto currency
     */
    internal fun onBalanceClick() = setViewType(!currencyState.isDisplayingCryptoCurrency)
    // endregion

    // region Update UI
    internal fun refreshBalanceHeader(account: ItemAccount) {
        view.updateSelectedCurrency(currencyState.cryptoCurrency)
        view.updateBalanceHeader(account.displayBalance ?: "")
    }

    internal fun refreshAccountDataSet() {
        val accountList = getAccounts()
        view.updateAccountsDataSet(accountList)
    }

    private fun refreshLauncherShortcuts() {
        view.generateLauncherShortcuts()
    }
    // endregion

    // region Adapter data
    private fun onTxFeedAdapterSetup() {
        view.setupTxFeedAdapter(currencyState.isDisplayingCryptoCurrency)
    }

    /**
     * Get accounts based on selected currency. Mutable list necessary for adapter. This needs fixing.
     */
    private fun getAccounts() = walletAccountHelper.getAccountItemsForOverview().toMutableList()

    private fun getCurrentAccount(): ItemAccount {
        return getAccountAt(view.getCurrentAccountPosition() ?: 0)
    }

    private fun getAccountAt(position: Int): ItemAccount {
        val accounts = getAccounts()
        return accounts[if (position < 0 || position >= accounts.size) 0 else position]
    }

    private fun getShapeShiftTxNotesObservable() =
        shapeShiftDataManager.getTradesList()
            .addToCompositeDisposable(this)
            .map {
                val mutableMap: MutableMap<String, String> = mutableMapOf()

                for (trade in it) {
                    trade.hashIn?.let {
                        mutableMap.put(it, stringUtils.getString(R.string.morph_deposit_to))
                    }
                    trade.hashOut?.let {
                        mutableMap.put(it, stringUtils.getString(R.string.morph_deposit_from))
                    }
                }
                return@map mutableMap.toMap()
            }
            .doOnError { Timber.e(it) }
            .onErrorReturn { mutableMapOf() }

    private fun getCoinifyTxNotesObservable() =
        tokenSingle.flatMap { coinifyDataManager.getTrades(it).toList() }
            .addToCompositeDisposable(this)
            .map {
                val mutableMap: MutableMap<String, String> = mutableMapOf()
                for (trade in it) {
                    val transfer = if (trade.isSellTransaction()) {
                        trade.transferIn.details as BlockchainDetails
                    } else {
                        trade.transferOut.details as BlockchainDetails
                    }
                    transfer.eventData?.txId?.let {
                        mutableMap.put(
                            it,
                            stringUtils.getFormattedString(
                                R.string.buy_sell_transaction_list_label,
                                trade.id
                            )
                        )
                    }
                }
                return@map mutableMap.toMap()
            }
            .toObservable()
            .doOnError { Timber.e(it) }
            .onErrorReturn { mutableMapOf() }

    private fun storeSwipeReceiveAddresses() {
        // Defer to background thread as deriving addresses is quite processor intensive
        Completable.fromCallable {
            swipeToReceiveHelper.updateAndStoreBitcoinAddresses()
            swipeToReceiveHelper.updateAndStoreBitcoinCashAddresses()
            Void.TYPE
        }.subscribeOn(Schedulers.computation())
            .addToCompositeDisposable(this)
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) })
    }
    // endregion

    // region Helper methods
    private fun getBtcBalanceString(showCrypto: Boolean, btcBalance: Long): String {
        return if (showCrypto) {
            currencyFormatManager.getFormattedBtcValueWithUnit(
                btcBalance.toBigDecimal(),
                BTCDenomination.SATOSHI
            )
        } else {
            currencyFormatManager.getFormattedFiatValueFromSelectedCoinValueWithSymbol(
                coinValue = btcBalance.toBigDecimal(),
                convertBtcDenomination = BTCDenomination.SATOSHI
            )
        }
    }

    private fun getEthBalanceString(showCrypto: Boolean, ethBalance: BigDecimal): String {
        return if (showCrypto) {
            currencyFormatManager.getFormattedEthShortValueWithUnit(ethBalance, ETHDenomination.WEI)
        } else {
            currencyFormatManager.getFormattedFiatValueFromSelectedCoinValueWithSymbol(
                coinValue = ethBalance,
                convertEthDenomination = ETHDenomination.WEI
            )
        }
    }

    private fun getBchBalanceString(showCrypto: Boolean, bchBalance: Long): String {
        return if (showCrypto) {
            currencyFormatManager.getFormattedBchValueWithUnit(
                bchBalance.toBigDecimal(),
                BTCDenomination.SATOSHI
            )
        } else {
            currencyFormatManager.getFormattedFiatValueFromSelectedCoinValueWithSymbol(bchBalance.toBigDecimal())
        }
    }

    private fun <T, R> mergeReduce(): BiFunction<Map<T, R>, Map<T, R>, Map<T, R>> =
        BiFunction { firstMap, secondMap ->
            mutableMapOf<T, R>().apply {
                putAll(firstMap)
                putAll(secondMap)
            }.toMap()
        }

    internal fun areLauncherShortcutsEnabled() =
        prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)

    internal fun getCurrentCurrency() = currencyState.cryptoCurrency
    // endregion
}
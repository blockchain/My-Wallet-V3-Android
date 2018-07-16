package piuk.blockchain.android.ui.dashboard

import android.support.annotation.DrawableRes
import android.support.annotation.VisibleForTesting
import info.blockchain.balance.AccountKey
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.Schedulers
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.ui.balance.AnnouncementData
import piuk.blockchain.android.ui.dashboard.models.OnboardingModel
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.onboarding.OnboardingPagerContent
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.currency.BTCDenomination
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.ETHDenomination
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.utils.logging.BalanceLoadedEvent
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
    private val prefsUtil: PrefsUtil,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val ethDataManager: EthDataManager,
    private val bchDataManager: BchDataManager,
    private val payloadDataManager: PayloadDataManager,
    private val transactionListDataManager: TransactionListDataManager,
    private val stringUtils: StringUtils,
    private val accessState: AccessState,
    private val buyDataManager: BuyDataManager,
    private val rxBus: RxBus,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    private val currencyFormatManager: CurrencyFormatManager
) : BasePresenter<DashboardView>() {

    private val displayList by unsafeLazy {
        mutableListOf<Any>(
            stringUtils.getString(R.string.dashboard_balances),
            PieChartsState.Loading,
            stringUtils.getString(R.string.dashboard_price_charts),
            AssetPriceCardState.Loading(CryptoCurrency.BTC),
            AssetPriceCardState.Loading(CryptoCurrency.ETHER),
            AssetPriceCardState.Loading(CryptoCurrency.BCH)
        )
    }
    private val metadataObservable by unsafeLazy {
        rxBus.register(
            MetadataEvent::class.java
        )
    }
    private var btcBalance = CryptoValue.ZeroBtc
    private var bchBalance = CryptoValue.ZeroBch
    private var ethBalance: BigInteger = BigInteger.ZERO

    override fun onViewReady() {
        with(view) {
            notifyItemAdded(displayList, 0)
            scrollToTop()
        }
        updatePrices()

        val observable = when (firstRun) {
            true -> metadataObservable
            false -> Observable.just(MetadataEvent.SETUP_COMPLETE)
                .applySchedulers()
                // If data is present, update with cached data
                // Data updates run anyway but this makes the UI nicer to look at whilst loading
                .doOnNext {
                    cachedData?.run { view.updatePieChartState(this) }
                }
        }

        firstRun = false

        // Triggers various updates to the page once all metadata is loaded
        observable.flatMap { getOnboardingStatusObservable() }
            // Clears subscription after single event
            .firstOrError()
            .doOnSuccess { updateAllBalances() }
            .doOnSuccess { checkLatestAnnouncements() }
            .doOnSuccess { swipeToReceiveHelper.storeEthAddress() }
            .addToCompositeDisposable(this)
            .subscribe(
                { /* No-op */ },
                { Timber.e(it) }
            )
    }

    fun updateBalances() {

        with(view) {
            scrollToTop()
        }

        updatePrices()
        updateAllBalances()
    }

    override fun onViewDestroyed() {
        rxBus.unregister(MetadataEvent::class.java, metadataObservable)
        super.onViewDestroyed()
    }

    private fun updatePrices() {
        exchangeRateFactory.updateTickers()
            .addToCompositeDisposable(this)
            .doOnError { Timber.e(it) }
            .subscribe(
                {
                    val list = listOf(
                        AssetPriceCardState.Data(
                            getBtcPriceString(),
                            CryptoCurrency.BTC,
                            R.drawable.vector_bitcoin
                        ),
                        AssetPriceCardState.Data(
                            getEthPriceString(),
                            CryptoCurrency.ETHER,
                            R.drawable.vector_eth
                        ),
                        AssetPriceCardState.Data(
                            getBchPriceString(),
                            CryptoCurrency.BCH,
                            R.drawable.vector_bitcoin_cash
                        )
                    )

                    handleAssetPriceUpdate(list)
                },
                {
                    val list = listOf(
                        AssetPriceCardState.Error(CryptoCurrency.BTC),
                        AssetPriceCardState.Error(CryptoCurrency.ETHER),
                        AssetPriceCardState.Error(CryptoCurrency.BCH)
                    )

                    handleAssetPriceUpdate(list)
                }
            )
    }

    private fun handleAssetPriceUpdate(list: List<AssetPriceCardState>) {
        displayList.removeAll { it is AssetPriceCardState }
        displayList.addAll(list)

        val firstPosition = displayList.indexOfFirst { it is AssetPriceCardState }

        val positions = listOf(
            firstPosition,
            firstPosition + 1,
            firstPosition + 2
        )

        view.notifyItemUpdated(displayList, positions)
    }

    private fun updateAllBalances() {
        ethDataManager.fetchEthAddress()
            .flatMapCompletable { ethAddressResponse ->
                payloadDataManager.updateAllBalances()
                    .andThen(
                        Completable.merge(
                            listOf(
                                payloadDataManager.updateAllTransactions(),
                                bchDataManager.updateAllBalances()
                            )
                        ).doOnError { Timber.e(it) }
                            .onErrorComplete()
                    )
                    .doOnComplete {
                        btcBalance = transactionListDataManager.balance(AccountKey.EntireWallet(CryptoCurrency.BTC))
                        bchBalance = transactionListDataManager.balance(AccountKey.EntireWallet(CryptoCurrency.BCH))
                        ethBalance = ethAddressResponse.getTotalBalance()

                        val btcFiat =
                            exchangeRateFactory.getLastBtcPrice(getFiatCurrency()) * btcBalance.toMajorUnitDouble()
                        val bchFiat =
                            exchangeRateFactory.getLastBchPrice(getFiatCurrency()) * bchBalance.toMajorUnitDouble()
                        val ethFiat =
                            BigDecimal(
                                exchangeRateFactory.getLastEthPrice(
                                    getFiatCurrency()
                                )
                            ).multiply(
                                Convert.fromWei(
                                    BigDecimal(ethBalance),
                                    Convert.Unit.ETHER
                                )
                            )

                        val totalDouble = btcFiat.plus(ethFiat.toDouble()).plus(bchFiat)
                        val totalString = getFormattedCurrencyString(totalDouble)

                        Logging.logCustom(
                            BalanceLoadedEvent(
                                btcBalance.isPositive(),
                                bchBalance.isPositive(),
                                ethBalance.signum() == 1
                            )
                        )

                        cachedData = PieChartsState.Data(
                            fiatSymbol = getCurrencySymbol(),
                            bitcoin = PieChartsState.DataPoint(
                                fiatValue = BigDecimal.valueOf(btcFiat),
                                fiatValueString = getBtcFiatString(btcBalance.amount),
                                cryptoValueString = getBtcBalanceString(btcBalance.amount)
                            ),
                            bitcoinCash = PieChartsState.DataPoint(
                                fiatValue = BigDecimal.valueOf(bchFiat),
                                fiatValueString = getBchFiatString(bchBalance.amount),
                                cryptoValueString = getBchBalanceString(bchBalance.amount)
                            ),
                            ether = PieChartsState.DataPoint(
                                fiatValue = ethFiat,
                                fiatValueString = getEthFiatString(ethBalance),
                                cryptoValueString = getEthBalanceString(ethBalance)
                            ),
                            totalValueString = totalString
                        ).also { view.updatePieChartState(it) }
                    }
            }
            .addToCompositeDisposable(this)
            .subscribe(
                { storeSwipeToReceiveAddresses() },
                { Timber.e(it) }
            )
    }

    private fun showAnnouncement(index: Int, announcementData: AnnouncementData) {
        displayList.add(index, announcementData)
        with(view) {
            notifyItemAdded(displayList, index)
            scrollToTop()
        }
    }

    private fun dismissAnnouncement(prefKey: String) {
        displayList.filterIsInstance<AnnouncementData>()
            .forEachIndexed { index, any ->
                if (any.prefsKey == prefKey) {
                    displayList.remove(any)
                    with(view) {
                        notifyItemRemoved(displayList, index)
                        scrollToTop()
                    }
                }
            }
    }

    private fun getOnboardingStatusObservable(): Observable<Boolean> = if (isOnboardingComplete()) {
        Observable.just(false)
    } else {
        buyDataManager.canBuy
            .addToCompositeDisposable(this)
            .doOnNext { displayList.removeAll { it is OnboardingModel } }
            .doOnNext { displayList.add(0, getOnboardingPages(it)) }
            .doOnNext { view.notifyItemAdded(displayList, 0) }
            .doOnNext { view.scrollToTop() }
            .doOnError { Timber.e(it) }
    }

    private fun checkLatestAnnouncements() {
        // If user hasn't completed onboarding, ignore announcements
        if (isOnboardingComplete()) {
            displayList.removeAll { it is AnnouncementData }

            val bchPrefKey = BITCOIN_CASH_ANNOUNCEMENT_DISMISSED
            if (!prefsUtil.getValue(bchPrefKey, false)) {
                prefsUtil.setValue(bchPrefKey, true)

                val announcementData = AnnouncementData(
                    title = R.string.bitcoin_cash,
                    description = R.string.onboarding_bitcoin_cash_description,
                    link = R.string.onboarding_cta,
                    image = R.drawable.vector_bch_onboarding,
                    emoji = "\uD83C\uDF89",
                    closeFunction = { dismissAnnouncement(bchPrefKey) },
                    linkFunction = { view.startBitcoinCashReceive() },
                    prefsKey = bchPrefKey
                )
                showAnnouncement(0, announcementData)
            }

            val buyPrefKey = SFOX_ANNOUNCEMENT_DISMISSED
            buyDataManager.isSfoxAllowed
                .addToCompositeDisposable(this)
                .subscribe(
                    {
                        if (it && !prefsUtil.getValue(buyPrefKey, false)) {
                            prefsUtil.setValue(buyPrefKey, true)

                            val announcementData = AnnouncementData(
                                title = R.string.announcement_trading_cta,
                                description = R.string.announcement_trading_description,
                                link = R.string.announcement_trading_link,
                                image = R.drawable.vector_buy_onboarding,
                                emoji = null,
                                closeFunction = { dismissAnnouncement(buyPrefKey) },
                                linkFunction = { view.startBuyActivity() },
                                prefsKey = buyPrefKey
                            )
                            showAnnouncement(0, announcementData)
                        }
                    }, { Timber.e(it) }
                )
        }
    }

    private fun getOnboardingPages(isBuyAllowed: Boolean): OnboardingModel {
        val pages = mutableListOf<OnboardingPagerContent>()

        if (isBuyAllowed) {
            // Buy bitcoin prompt
            pages.add(
                OnboardingPagerContent(
                    stringUtils.getString(R.string.onboarding_current_price),
                    getFormattedPriceString(),
                    stringUtils.getString(R.string.onboarding_buy_content),
                    stringUtils.getString(R.string.onboarding_buy_bitcoin),
                    MainActivity.ACTION_BUY,
                    R.color.primary_blue_accent,
                    R.drawable.vector_buy_offset
                )
            )
        }
        // Receive bitcoin
        pages.add(
            OnboardingPagerContent(
                stringUtils.getString(R.string.onboarding_receive_bitcoin),
                "",
                stringUtils.getString(R.string.onboarding_receive_content),
                stringUtils.getString(R.string.receive_bitcoin),
                MainActivity.ACTION_RECEIVE,
                R.color.secondary_teal_medium,
                R.drawable.vector_receive_offset
            )
        )
        // QR Codes
        pages.add(
            OnboardingPagerContent(
                stringUtils.getString(R.string.onboarding_qr_codes),
                "",
                stringUtils.getString(R.string.onboarding_qr_codes_content),
                stringUtils.getString(R.string.onboarding_scan_address),
                MainActivity.ACTION_SEND,
                R.color.primary_navy_medium,
                R.drawable.vector_qr_offset
            )
        )

        return OnboardingModel(
            pages,
            // TODO: These are neat and clever, but make things pretty hard to test. Replace with callbacks.
            dismissOnboarding = {
                setOnboardingComplete(true)
                displayList.removeAll { it is OnboardingModel }
                view.notifyItemRemoved(displayList, 0)
                view.scrollToTop()
            },
            onboardingComplete = { setOnboardingComplete(true) },
            onboardingNotComplete = { setOnboardingComplete(false) }
        )
    }

    private fun isOnboardingComplete() =
    // If wallet isn't newly created, don't show onboarding
        prefsUtil.getValue(
            PrefsUtil.KEY_ONBOARDING_COMPLETE,
            false
        ) || !accessState.isNewlyCreated

    private fun setOnboardingComplete(completed: Boolean) {
        prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, completed)
    }

    private fun storeSwipeToReceiveAddresses() {
        bchDataManager.getWalletTransactions(50, 0)
            .flatMapCompletable { getSwipeToReceiveCompletable() }
            .addToCompositeDisposable(this)
            .subscribe(
                { view.startWebsocketService() },
                { Timber.e(it) }
            )
    }

    private fun getSwipeToReceiveCompletable(): Completable =
    // Defer to background thread as deriving addresses is quite processor intensive
        Completable.fromCallable {
            swipeToReceiveHelper.updateAndStoreBitcoinAddresses()
            swipeToReceiveHelper.updateAndStoreBitcoinCashAddresses()
        }.subscribeOn(Schedulers.computation())
            // Ignore failure
            .onErrorComplete()

    // /////////////////////////////////////////////////////////////////////////
    // Units
    // /////////////////////////////////////////////////////////////////////////

    private fun getFormattedPriceString(): String {
        val lastPrice = getLastBtcPrice(getFiatCurrency())
        val fiatSymbol = currencyFormatManager.getFiatSymbol(getFiatCurrency(), view.locale)
        val format = DecimalFormat().apply { minimumFractionDigits = 2 }

        return stringUtils.getFormattedString(
            R.string.current_price_btc,
            "$fiatSymbol${format.format(lastPrice)}"
        )
    }

    private fun getBtcBalanceString(btcBalance: BigInteger): String =
        currencyFormatManager.getFormattedBtcValueWithUnit(
            btcBalance.toBigDecimal(),
            BTCDenomination.SATOSHI
        )

    private fun getBtcFiatString(btcBalance: BigInteger): String =
        currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(
            btcBalance.toBigDecimal(),
            BTCDenomination.SATOSHI
        )

    private fun getBchBalanceString(bchBalance: BigInteger): String =
        currencyFormatManager.getFormattedBchValueWithUnit(
            bchBalance.toBigDecimal(),
            BTCDenomination.SATOSHI
        )

    private fun getBchFiatString(bchBalance: BigInteger): String =
        currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(
            bchBalance.toBigDecimal(),
            BTCDenomination.SATOSHI
        )

    private fun getEthBalanceString(ethBalance: BigInteger): String =
        currencyFormatManager.getFormattedEthShortValueWithUnit(
            ethBalance.toBigDecimal(),
            ETHDenomination.WEI
        )

    private fun getEthFiatString(ethBalance: BigInteger): String =
        currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(
            ethBalance.toBigDecimal(),
            ETHDenomination.WEI
        )

    private fun getBtcPriceString(): String =
        getLastBtcPrice(getFiatCurrency()).run { getFormattedCurrencyString(this) }

    private fun getEthPriceString(): String =
        getLastEthPrice(getFiatCurrency()).run { getFormattedCurrencyString(this) }

    private fun getBchPriceString(): String =
        getLastBchPrice(getFiatCurrency()).run { getFormattedCurrencyString(this) }

    private fun getFormattedCurrencyString(price: Double): String {
        return currencyFormatManager.getFormattedFiatValueWithSymbol(price)
    }

    private fun getCurrencySymbol() =
        currencyFormatManager.getFiatSymbol(getFiatCurrency(), view.locale)

    private fun getFiatCurrency() =
        prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getLastBtcPrice(fiat: String) = exchangeRateFactory.getLastBtcPrice(fiat)

    private fun getLastEthPrice(fiat: String) = exchangeRateFactory.getLastEthPrice(fiat)

    private fun getLastBchPrice(fiat: String) = exchangeRateFactory.getLastBchPrice(fiat)

    companion object {

        @VisibleForTesting
        const val BITCOIN_CASH_ANNOUNCEMENT_DISMISSED =
            "BITCOIN_CASH_ANNOUNCEMENT_DISMISSED"

        @VisibleForTesting
        const val SFOX_ANNOUNCEMENT_DISMISSED =
            "SFOX_ANNOUNCEMENT_DISMISSED"

        /**
         * This field stores whether or not the presenter has been run for the first time across
         * all instances. This allows the page to load without a metadata set-up event, which won't
         * be present if the the page is being returned to.
         */
        @VisibleForTesting
        var firstRun = true

        /**
         * This is intended to be a temporary solution to caching data on this page. In future,
         * I intend to organise the MainActivity fragment backstack so that the DashboardFragment
         * is never killed intentionally. However, this could introduce a lot of bugs so this will
         * do for now.
         */
        private var cachedData: PieChartsState.Data? = null

        @JvmStatic
        fun onLogout() {
            firstRun = true
            cachedData = null
        }
    }
}

sealed class PieChartsState {

    data class DataPoint(
        val fiatValue: BigDecimal,
        val fiatValueString: String,
        val cryptoValueString: String
    ) {
        val isZero: Boolean = fiatValue == BigDecimal.ZERO
    }

    data class Data(
        val fiatSymbol: String,
        val bitcoin: DataPoint,
        val ether: DataPoint,
        val bitcoinCash: DataPoint,
        val totalValueString: String
    ) : PieChartsState() {
        val isZero: Boolean = bitcoin.isZero && bitcoinCash.isZero && ether.isZero
    }

    object Loading : PieChartsState()
    object Error : PieChartsState()
}

sealed class AssetPriceCardState(val currency: CryptoCurrency) {

    data class Data(
        val priceString: String,
        val cryptoCurrency: CryptoCurrency,
        @DrawableRes val icon: Int
    ) : AssetPriceCardState(cryptoCurrency)

    class Loading(val cryptoCurrency: CryptoCurrency) : AssetPriceCardState(cryptoCurrency)
    class Error(val cryptoCurrency: CryptoCurrency) : AssetPriceCardState(cryptoCurrency)
}
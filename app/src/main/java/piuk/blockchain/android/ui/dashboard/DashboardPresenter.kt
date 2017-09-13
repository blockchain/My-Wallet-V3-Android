package piuk.blockchain.android.ui.dashboard

import info.blockchain.api.data.Point
import io.reactivex.Observable
import org.web3j.utils.Convert
import piuk.blockchain.android.R
import piuk.blockchain.android.data.charts.ChartsDataManager
import piuk.blockchain.android.data.charts.TimeSpan
import piuk.blockchain.android.data.currency.CryptoCurrencies
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.data.exchange.BuyDataManager
import piuk.blockchain.android.data.payload.PayloadDataManager
import piuk.blockchain.android.data.rxjava.RxBus
import piuk.blockchain.android.data.rxjava.RxUtil
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.balance.AnnouncementData
import piuk.blockchain.android.ui.base.BasePresenter
import piuk.blockchain.android.ui.customviews.ToastCustom
import piuk.blockchain.android.ui.dashboard.models.OnboardingModel
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.onboarding.OnboardingPagerContent
import piuk.blockchain.android.util.*
import piuk.blockchain.android.util.helperfunctions.unsafeLazy
import timber.log.Timber
import java.math.BigDecimal
import java.math.BigInteger
import java.text.DecimalFormat
import javax.inject.Inject

class DashboardPresenter @Inject constructor(
        private val chartsDataManager: ChartsDataManager,
        private val prefsUtil: PrefsUtil,
        private val exchangeRateFactory: ExchangeRateFactory,
        private val ethDataManager: EthDataManager,
        private val payloadDataManager: PayloadDataManager,
        private val transactionListDataManager: TransactionListDataManager,
        private val stringUtils: StringUtils,
        private val appUtil: AppUtil,
        private val buyDataManager: BuyDataManager,
        private val rxBus: RxBus
) : BasePresenter<DashboardView>() {

    private val monetaryUtil: MonetaryUtil by unsafeLazy { MonetaryUtil(getBtcUnitType()) }
    private var cryptoCurrency = CryptoCurrencies.BTC
    private val displayList = mutableListOf<Any>(ChartDisplayable())
    private val metadataObservable = rxBus.register(MetadataEvent::class.java)

    override fun onViewReady() {
        updateChartsData(TimeSpan.YEAR)
        updateAllBalances()
        view.notifyItemAdded(displayList, 0)

        metadataObservable.flatMap { getOnboardingStatusObservable() }
                .doOnNext { checkLatestAnnouncement() }
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) }
                )
    }

    override fun onViewDestroyed() {
        rxBus.unregister(MetadataEvent::class.java, metadataObservable)
        super.onViewDestroyed()
    }

    internal fun updateSelectedCurrency(cryptoCurrency: CryptoCurrencies) {
        this.cryptoCurrency = cryptoCurrency
        updatePrices()
        // TODO: Update graph data once ETH supported  
    }

    internal fun updatePrices() {
        exchangeRateFactory.updateTickers()
                .compose(RxUtil.addCompletableToCompositeDisposable(this))
                .subscribe(
                        {
                            view.updateCryptoCurrencyPrice(
                                    if (cryptoCurrency == CryptoCurrencies.BTC)
                                        getBtcString() else getEthString()
                            )
                        },
                        {
                            Timber.e(it)
                            view.showToast(R.string.dashboard_charts_price_error, ToastCustom.TYPE_ERROR)
                        }
                )
    }

    private fun updateChartsData(timeSpan: TimeSpan) {
        compositeDisposable.clear()

        view.updateChartState(ChartsState.TimeSpanUpdated(timeSpan))

        when (timeSpan) {
            TimeSpan.YEAR -> chartsDataManager.getYearPrice()
            TimeSpan.MONTH -> chartsDataManager.getMonthPrice()
            TimeSpan.WEEK -> chartsDataManager.getWeekPrice()
            else -> throw IllegalArgumentException("Day isn't currently supported")
        }.compose(RxUtil.addObservableToCompositeDisposable(this))
                .toList()
                .doOnSubscribe { view.updateChartState(ChartsState.Loading) }
                .doOnSuccess { view.updateChartState(getChartsData(it)) }
                .doOnError { view.updateChartState(ChartsState.Error) }
                .subscribe(
                        { /* No-op */ },
                        { Timber.e(it) }
                )
    }

    private fun getChartsData(list: List<Point>): ChartsState.Data {
        return ChartsState.Data(
                data = list,
                fiatSymbol = "$", // Dollar only supported currency right now
                getChartYear = { updateChartsData(TimeSpan.YEAR) },
                getChartMonth = { updateChartsData(TimeSpan.MONTH) },
                getChartWeek = { updateChartsData(TimeSpan.WEEK) }
        )
    }

    private fun updateAllBalances() {
        ethDataManager.getEthereumWallet(stringUtils.getString(R.string.eth_default_account_label))
                .flatMap { ethDataManager.fetchEthAddress() }
                .flatMapCompletable { ethAddressResponse ->
                    payloadDataManager.updateAllBalances()
                            .doOnComplete {
                                val btcBalance = transactionListDataManager.getBtcBalance(ItemAccount().apply {
                                    type = ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY
                                })
                                view.updateBtcBalance(getBtcBalanceString(btcBalance))
                                view.updateEthBalance(getEthBalanceString(ethAddressResponse.balance))

                                val btcFiat = exchangeRateFactory.getLastBtcPrice(getFiatCurrency()) * (btcBalance / 1e8)
                                val ethFiat = BigDecimal(exchangeRateFactory.getLastEthPrice(getFiatCurrency()))
                                        .multiply(Convert.fromWei(BigDecimal(ethAddressResponse.balance), Convert.Unit.ETHER))

                                val totalDouble = btcFiat.plus(ethFiat.toDouble())

                                val totalString = "${getCurrencySymbol()}${monetaryUtil.getFiatFormat(getFiatCurrency()).format(totalDouble)}"
                                view.updateTotalBalance(totalString)
                            }
                }.subscribe(
                { /* No-op*/ },
                {
                    Timber.e(it)
                    view.showToast(R.string.dashboard_charts_price_error, ToastCustom.TYPE_ERROR)
                }
        )
    }

    private fun showAnnouncement() {
        // Don't add the announcement if there already is one
        if (displayList.none { it is AnnouncementData }) {
            // In the future, the announcement data may be parsed from an endpoint. For now, here is fine
            val announcementData = AnnouncementData(
                    title = R.string.onboarding_ether_title,
                    description = R.string.onboarding_ether_description,
                    link = R.string.onboarding_ether_cta,
                    image = R.drawable.vector_eth_offset,
                    emoji = "",
                    closeFunction = { dismissAnnouncement() },
                    linkFunction = { view.startReceiveFragment() }
            )

            displayList.add(0, announcementData)
            view.notifyItemAdded(displayList, 0)
        }
    }

    private fun dismissAnnouncement() {
        prefsUtil.setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, true)
        if (displayList.any { it is AnnouncementData }) {
            displayList.removeAll { it is AnnouncementData }
            view.notifyItemRemoved(displayList, 0)
        }
    }

    private fun getOnboardingStatusObservable(): Observable<Boolean> {
        return if (isOnboardingComplete()) {
            Observable.just(false)
        } else
            buyDataManager.canBuy
                    .compose(RxUtil.addObservableToCompositeDisposable(this))
                    .doOnNext { displayList.add(0, getOnboardingPages(it)) }
                    .doOnNext { view.notifyItemAdded(displayList, 0) }
                    .doOnError { Timber.e(it) }
    }

    private fun checkLatestAnnouncement() {
        // If user hasn't completed onboarding, ignore announcements
        if (isOnboardingComplete()) {
            if (!prefsUtil.getValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_DISMISSED, false)) {
                prefsUtil.setValue(PrefsUtil.KEY_LATEST_ANNOUNCEMENT_SEEN, true)
                showAnnouncement()
            }
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
                dismissOnboarding = {
                    displayList.removeAll { it is OnboardingModel }
                    view.notifyItemRemoved(displayList, 0)
                },
                onboardingComplete = { setOnboardingComplete(true) },
                onboardingNotComplete = { setOnboardingComplete(false) }
        )
    }

    ///////////////////////////////////////////////////////////////////////////
    // Units
    ///////////////////////////////////////////////////////////////////////////

    private fun getFormattedPriceString(): String {
        val lastPrice = getLastBtcPrice(getFiatCurrency())
        val fiatSymbol = exchangeRateFactory.getSymbol(getFiatCurrency())
        val format = DecimalFormat().apply { minimumFractionDigits = 2 }

        return stringUtils.getFormattedString(
                R.string.current_price_btc,
                "$fiatSymbol${format.format(lastPrice)}"
        )
    }

    private fun getBtcBalanceString(btcBalance: Long): String =
            "${monetaryUtil.getDisplayAmountWithFormatting(btcBalance)} ${getBtcDisplayUnits()}"

    private fun getEthBalanceString(ethBalance: BigInteger): String {
        val number = DecimalFormat.getInstance().apply { maximumFractionDigits = 8 }
                .run { format(Convert.fromWei(BigDecimal(ethBalance), Convert.Unit.ETHER)) }

        return "$number ETH"
    }

    private fun getCurrencySymbol() = exchangeRateFactory.getSymbol(getFiatCurrency())

    // USD only supported currency for now
    private fun getBtcString(): String {
        val lastBtcPrice = getLastBtcPrice("USD")
        return "$${monetaryUtil.getFiatFormat(getFiatCurrency()).format(lastBtcPrice)}"
    }

    // USD only supported currency for now
    private fun getEthString(): String {
        val lastEthPrice = getLastEthPrice("USD")
        return "$${monetaryUtil.getFiatFormat(getFiatCurrency()).format(lastEthPrice)}"
    }

    private fun getBtcDisplayUnits() = monetaryUtil.getBtcUnits()[getBtcUnitType()]

    private fun getBtcUnitType() =
            prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)

    private fun getFiatCurrency() =
            prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getLastBtcPrice(fiat: String) = exchangeRateFactory.getLastBtcPrice(fiat)

    private fun getLastEthPrice(fiat: String) = exchangeRateFactory.getLastEthPrice(fiat)

    private fun isOnboardingComplete() =
            // If wallet isn't newly created, don't show onboarding
            prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false) || !appUtil.isNewlyCreated

    private fun setOnboardingComplete(completed: Boolean) {
        prefsUtil.setValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, completed)
    }

}

sealed class ChartsState {

    data class Data(
            val data: List<Point>,
            val fiatSymbol: String,
            val getChartYear: () -> Unit,
            val getChartMonth: () -> Unit,
            val getChartWeek: () -> Unit
    ) : ChartsState()

    object Error : ChartsState()
    object Loading : ChartsState()
    class TimeSpanUpdated(val timeSpan: TimeSpan) : ChartsState()

}

class ChartDisplayable
package piuk.blockchain.android.ui.dashboard

import android.support.annotation.DrawableRes
import android.support.annotation.VisibleForTesting
import com.blockchain.balance.drawableRes
import com.blockchain.kyc.status.KycTiersQueries
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.kycui.sunriver.SunriverCardType
import com.blockchain.lockbox.data.LockboxDataManager
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.Observables
import io.reactivex.rxkotlin.Singles
import io.reactivex.rxkotlin.plusAssign
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import piuk.blockchain.android.R
import piuk.blockchain.android.ui.balance.AnnouncementData
import piuk.blockchain.android.ui.balance.ImageLeftAnnouncementCard
import piuk.blockchain.android.ui.balance.ImageRightAnnouncementCard
import piuk.blockchain.android.ui.charts.models.ArbitraryPrecisionFiatValue
import piuk.blockchain.android.ui.charts.models.toStringWithSymbol
import piuk.blockchain.android.ui.dashboard.adapter.delegates.SunriverCard
import piuk.blockchain.android.ui.dashboard.models.OnboardingModel
import piuk.blockchain.android.ui.home.MainActivity
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.onboarding.OnboardingPagerContent
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.android.util.extensions.addToCompositeDisposable
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcore.utils.extensions.applySchedulers
import piuk.blockchain.androidcore.utils.helperfunctions.unsafeLazy
import piuk.blockchain.androidcoreui.ui.base.BasePresenter
import piuk.blockchain.androidcoreui.utils.logging.BalanceLoadedEvent
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber
import java.text.DecimalFormat

class DashboardPresenter(
    private val dashboardBalanceCalculator: DashboardData,
    private val prefsUtil: PrefsUtil,
    private val exchangeRateFactory: ExchangeRateDataManager,
    private val bchDataManager: BchDataManager,
    private val stringUtils: StringUtils,
    private val accessState: AccessState,
    private val buyDataManager: BuyDataManager,
    private val rxBus: RxBus,
    private val swipeToReceiveHelper: SwipeToReceiveHelper,
    private val currencyFormatManager: CurrencyFormatManager,
    private val kycTiersQueries: KycTiersQueries,
    private val lockboxDataManager: LockboxDataManager,
    private val sunriverCampaignHelper: SunriverCampaignHelper
) : BasePresenter<DashboardView>() {

    private val currencies = DashboardConfig.currencies

    private val displayList by unsafeLazy {
        (listOf(
            stringUtils.getString(R.string.dashboard_balances),
            PieChartsState.Loading,
            stringUtils.getString(R.string.dashboard_price_charts)
        ) + currencies.map {
            AssetPriceCardState.Loading(it)
        }).toMutableList()
    }

    private val metadataObservable by unsafeLazy {
        rxBus.register(
            MetadataEvent::class.java
        )
    }

    private val balanceUpdateDisposable = CompositeDisposable()

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

        compositeDisposable += balanceUpdateDisposable

        // Triggers various updates to the page once all metadata is loaded
        observable.flatMap { getOnboardingStatusObservable() }
            // Clears subscription after single event
            .firstOrError()
            .doOnSuccess { updateAllBalances() }
            .doOnSuccess { checkLatestAnnouncements() }
            .flatMapCompletable { swipeToReceiveHelper.storeEthAddress() }
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
            .observeOn(AndroidSchedulers.mainThread())
            .addToCompositeDisposable(this)
            .doOnError { Timber.e(it) }
            .subscribe(
                {
                    handleAssetPriceUpdate(
                        currencies.map {
                            AssetPriceCardState.Data(
                                getPriceString(it),
                                it,
                                it.drawableRes()
                            )
                        })
                },
                {
                    handleAssetPriceUpdate(
                        currencies.map {
                            AssetPriceCardState.Error(it)
                        }
                    )
                }
            )
    }

    private fun handleAssetPriceUpdate(list: List<AssetPriceCardState>) {
        displayList.removeAll { it is AssetPriceCardState }
        displayList.addAll(list)

        val firstPosition = displayList.indexOfFirst { it is AssetPriceCardState }

        val positions = (firstPosition until firstPosition + list.size).toList()

        view.notifyItemUpdated(displayList, positions)
    }

    private val balanceFilter = BehaviorSubject.create<BalanceFilter>().apply {
        onNext(BalanceFilter.Total)
    }

    fun setBalanceFilter(balanceFilter: BalanceFilter) {
        this.balanceFilter.onNext(balanceFilter)
    }

    private fun updateAllBalances() {
        balanceUpdateDisposable.clear()
        val data =
            dashboardBalanceCalculator.getPieChartData(balanceFilter.distinctUntilChanged())
                .observeOn(AndroidSchedulers.mainThread())
                .doOnNext {
                    Logging.logCustom(
                        BalanceLoadedEvent(
                            hasBtcBalance = !it.bitcoin.displayable.isZero,
                            hasBchBalance = !it.bitcoinCash.displayable.isZero,
                            hasEthBalance = !it.ether.displayable.isZero,
                            hasXlmBalance = !it.lumen.displayable.isZero
                        )
                    )
                    cachedData = it
                    storeSwipeToReceiveAddresses()
                }
        balanceUpdateDisposable += Observables.combineLatest(
            data,
            shouldDisplayLockboxMessage().cache().toObservable()
        ).map { (data, hasLockbox) ->
            data.copy(hasLockbox = hasLockbox)
        }.observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                view::updatePieChartState,
                Timber::e
            )
    }

    private fun shouldDisplayLockboxMessage(): Single<Boolean> = Single.zip(
        lockboxDataManager.isLockboxAvailable(),
        lockboxDataManager.hasLockbox(),
        BiFunction { available: Boolean, hasLockbox: Boolean -> available && hasLockbox }
    )

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
            // TODO: AND-1691 This is disabled temporarily for now until onboarding/announcements have been rethought.
//            checkNativeBuySellAnnouncement()
            compositeDisposable +=
                checkKycPrompt()
                    .subscribeBy(
                        onSuccess = { showedKycCard -> if (!showedKycCard) addSunriverPrompts() }
                    )
        }
    }

    internal fun addSunriverPrompts(): Single<Boolean> {
        val publish = BehaviorSubject.create<Boolean>()
        compositeDisposable +=
            sunriverCampaignHelper.getCampaignCardType()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeBy(
                    onSuccess = {
                        when (it) {
                            SunriverCardType.None -> publish.onNext(false)
                            SunriverCardType.JoinWaitList ->
                                SunriverCard.nowSupported(
                                    { removeSunriverCard() },
                                    { view.launchWaitlist() }
                                ).addIfNotDismissed()
                                    .also(publish::onNext)
                            SunriverCardType.FinishSignUp ->
                                SunriverCard.continueClaim(
                                    { removeSunriverCard() },
                                    { view.startKycFlow(CampaignType.Sunriver) }
                                ).addIfNotDismissed()
                                    .also(publish::onNext)
                            SunriverCardType.Complete ->
                                SunriverCard.onTheWay(
                                    { removeSunriverCard() },
                                    {}
                                ).addIfNotDismissed()
                                    .also(publish::onNext)
                        }
                    },
                    onError = Timber::e
                )
        return publish.first(false)
    }

    private fun SunriverCard.addIfNotDismissed(): Boolean {
        val add = !prefsUtil.getValue(prefsKey, false)
        if (add) {
            displayList.add(0, this)
            view.notifyItemAdded(displayList, 0)
            view.scrollToTop()
        }
        return add
    }

    private fun removeSunriverCard() {
        displayList.firstOrNull { it is SunriverCard }?.let {
            displayList.remove(it)
            prefsUtil.setValue((it as AnnouncementData).prefsKey, true)
            view.notifyItemRemoved(displayList, 0)
            view.scrollToTop()
        }
    }

    private fun checkNativeBuySellAnnouncement() {
        val buyPrefKey = NATIVE_BUY_SELL_DISMISSED
        buyDataManager.isCoinifyAllowed
            .addToCompositeDisposable(this)
            .subscribeBy(
                onNext = {
                    if (it && !prefsUtil.getValue(buyPrefKey, false)) {
                        prefsUtil.setValue(buyPrefKey, true)

                        val announcementData = ImageLeftAnnouncementCard(
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
                },
                onError = { Timber.e(it) }
            )
    }

    private fun checkKycPrompt(): Single<Boolean> {
        val displayed = BehaviorSubject.create<Boolean>()
        if (!prefsUtil.getValue(KYC_INCOMPLETE_DISMISSED, false)) {
            compositeDisposable +=
                Singles.zip(
                    kycTiersQueries.isKycInProgress(),
                    sunriverCampaignHelper.getCampaignCardType()
                )
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                        onSuccess = { (isKycInProgress, campaignCard) ->
                            if (isKycInProgress) {
                                val kycIncompleteData = ImageRightAnnouncementCard(
                                    title = R.string.buy_sell_verify_your_identity,
                                    description = R.string.kyc_drop_off_card_description,
                                    link = R.string.kyc_drop_off_card_button,
                                    image = R.drawable.vector_kyc_onboarding,
                                    closeFunction = {
                                        prefsUtil.setValue(KYC_INCOMPLETE_DISMISSED, true)
                                        dismissAnnouncement(KYC_INCOMPLETE_DISMISSED)
                                    },
                                    linkFunction = {
                                        view.startKycFlow(
                                            if (campaignCard == SunriverCardType.FinishSignUp) {
                                                CampaignType.Sunriver
                                            } else {
                                                CampaignType.Swap
                                            }
                                        )
                                    },
                                    prefsKey = KYC_INCOMPLETE_DISMISSED
                                )
                                showAnnouncement(0, kycIncompleteData)
                                displayed.onNext(true)
                            } else {
                                displayed.onNext(false)
                            }
                        },
                        onError = {
                            Timber.e(it)
                            displayed.onNext(false)
                        }
                    )
        } else {
            displayed.onNext(false)
        }
        return displayed.first(false)
    }

    private fun getOnboardingPages(isBuyAllowed: Boolean): OnboardingModel {
        val pages = mutableListOf<OnboardingPagerContent>()

        if (isBuyAllowed) {
            // Buy bitcoin prompt
            pages.add(
                OnboardingPagerContent(
                    stringUtils.getString(R.string.onboarding_current_price),
                    getFormattedPriceString(CryptoCurrency.BTC),
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
        swipeToReceiveHelper.updateAndStoreBitcoinAddresses()
            .andThen(swipeToReceiveHelper.updateAndStoreBitcoinCashAddresses())
            .subscribeOn(Schedulers.computation())
            // Ignore failure
            .onErrorComplete()

    // /////////////////////////////////////////////////////////////////////////
    // Units
    // /////////////////////////////////////////////////////////////////////////

    private fun getFormattedPriceString(cryptoCurrency: CryptoCurrency): String {
        val lastPrice = getLastPrice(cryptoCurrency, getFiatCurrency())
        val fiatSymbol = currencyFormatManager.getFiatSymbol(getFiatCurrency(), view.locale)
        val format = DecimalFormat().apply { minimumFractionDigits = 2 }

        return stringUtils.getFormattedString(
            R.string.current_price_btc,
            "$fiatSymbol${format.format(lastPrice)}"
        )
    }

    private fun getPriceString(cryptoCurrency: CryptoCurrency): String {
        val fiat = getFiatCurrency()
        return getLastPrice(cryptoCurrency, fiat).run {
            ArbitraryPrecisionFiatValue.fromMajor(fiat, this.toBigDecimal())
                .toStringWithSymbol()
        }
    }

    private fun getFiatCurrency() =
        prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)

    private fun getLastPrice(cryptoCurrency: CryptoCurrency, fiat: String) =
        exchangeRateFactory.getLastPrice(cryptoCurrency, fiat)

    companion object {

        @VisibleForTesting
        internal const val KYC_INCOMPLETE_DISMISSED = "KYC_INCOMPLETE_DISMISSED"

        @VisibleForTesting
        internal const val NATIVE_BUY_SELL_DISMISSED = "NATIVE_BUY_SELL_DISMISSED"

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

sealed class AssetPriceCardState(val currency: CryptoCurrency) {

    data class Data(
        val priceString: String,
        val cryptoCurrency: CryptoCurrency,
        @DrawableRes val icon: Int
    ) : AssetPriceCardState(cryptoCurrency)

    class Loading(val cryptoCurrency: CryptoCurrency) : AssetPriceCardState(cryptoCurrency)
    class Error(val cryptoCurrency: CryptoCurrency) : AssetPriceCardState(cryptoCurrency)
}
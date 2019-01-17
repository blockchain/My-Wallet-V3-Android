package piuk.blockchain.android.ui.dashboard

import com.blockchain.android.testutils.rxInit
import com.blockchain.balance.TotalBalance
import com.blockchain.kyc.status.KycTiersQueries
import com.blockchain.kycui.sunriver.SunriverCampaignHelper
import com.blockchain.kycui.sunriver.SunriverCardType
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.blockchain.testutils.lumens
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.verifyZeroInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.exchangerate.ratesFor
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PrefsUtil
import java.util.Locale

class DashboardPresenterTest {

    private lateinit var subject: DashboardPresenter
    private val prefsUtil: PrefsUtil = mock()
    private val exchangeRateFactory: ExchangeRateDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val payloadDataManager: PayloadDataManager = mock()
    private val transactionListDataManager: TransactionListDataManager = mock()
    private val stringUtils: StringUtils = mock()
    private val accessState: AccessState = mock()
    private val buyDataManager: BuyDataManager = mock()
    private val rxBus: RxBus = mock()
    private val swipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private val view: DashboardView = mock()
    private val currencyFormatManager: CurrencyFormatManager = mock()
    private val kycTiersQueries: KycTiersQueries = mock()
    private val lockboxDataManager: LockboxDataManager = mock()
    private val sunriverCampaignHelper: SunriverCampaignHelper = mock()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = DashboardPresenter(
            AsyncDashboardDataCalculator(
                exchangeRateFactory.ratesFor("USD"),
                BalanceUpdater(
                    bchDataManager,
                    payloadDataManager
                ),
                transactionListDataManager
            ),
            prefsUtil,
            exchangeRateFactory,
            bchDataManager,
            stringUtils,
            accessState,
            buyDataManager,
            rxBus,
            swipeToReceiveHelper,
            currencyFormatManager,
            kycTiersQueries,
            lockboxDataManager,
            sunriverCampaignHelper
        )

        subject.initView(view)

        whenever(view.locale).thenReturn(Locale.US)
        whenever(bchDataManager.getWalletTransactions(50, 0))
            .thenReturn(Observable.just(emptyList()))
    }

    @Test
    fun `onViewReady onboarding complete, no announcements`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn("USD")
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BTC),
                any()
            )
        ).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(
            4000.00
        )
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BCH),
                any()
            )
        ).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // Native Buy/Sell not available
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(false))
        // KYC already dismissed
        whenever(prefsUtil.getValue(DashboardPresenter.KYC_INCOMPLETE_DISMISSED, false)).thenReturn(
            true
        )
        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))
        // Ignore Sunriver
        whenever(sunriverCampaignHelper.getCampaignCardType()).thenReturn(Single.never())

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).scrollToTop()
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)

        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        // TODO: AND-1691 Temporarily disabled while onboarding/announcements are rethought.
//        verify(buyDataManager).isCoinifyAllowed

        verify(swipeToReceiveHelper).storeEthAddress()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady onboarding not complete`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn("USD")
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BTC),
                any()
            )
        ).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(
            4000.00
        )
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BCH),
                any()
            )
        ).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(false)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // No Native Buy/Sell announcement
        whenever(prefsUtil.getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false))
            .thenReturn(true)
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(false))
        // KYC already dismissed
        whenever(prefsUtil.getValue(DashboardPresenter.KYC_INCOMPLETE_DISMISSED, false)).thenReturn(
            true
        )
        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))
        // Ignore Sunriver
        whenever(sunriverCampaignHelper.getCampaignCardType()).thenReturn(Single.never())

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).scrollToTop()
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        // no announcements allowed while onboarding hasn't been completed

        verify(swipeToReceiveHelper).storeEthAddress()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady onboarding complete native buy sell announcement`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn("USD")
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BTC),
                any()
            )
        ).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(
            4000.00
        )
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BCH),
                any()
            )
        ).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // No Native Buy/Sell announcement
        whenever(prefsUtil.getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false))
            .thenReturn(false)
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(true))
        whenever(prefsUtil.getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false))
            .thenReturn(false)
        // KYC already dismissed
        whenever(prefsUtil.getValue(DashboardPresenter.KYC_INCOMPLETE_DISMISSED, false))
            .thenReturn(true)
        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))
        // Ignore Sunriver
        whenever(sunriverCampaignHelper.getCampaignCardType()).thenReturn(Single.never())

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        // Native Buy/Sell
        // TODO: AND-1691 Temporarily disabled while onboarding/announcements are rethought.
//        verify(buyDataManager).isCoinifyAllowed
//        verify(prefsUtil).getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false)
//        verify(prefsUtil, atLeastOnce()).setValue(
//            DashboardPresenter.NATIVE_BUY_SELL_DISMISSED,
//            true
//        )
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).scrollToTop()

        verify(swipeToReceiveHelper).storeEthAddress()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady onboarding complete kyc announcement`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn("USD")
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BTC), any()))
            .thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any()))
            .thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BCH), any()))
            .thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // No Native Buy/Sell announcement
        whenever(prefsUtil.getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false))
            .thenReturn(false)
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(true))
        whenever(prefsUtil.getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false))
            .thenReturn(false)
        // KYC already dismissed
        whenever(prefsUtil.getValue(DashboardPresenter.KYC_INCOMPLETE_DISMISSED, false))
            .thenReturn(false)
        whenever(kycTiersQueries.isKycInProgress()).thenReturn(Single.just(true))
        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))
        // Ignore Sunriver
        whenever(sunriverCampaignHelper.getCampaignCardType()).thenReturn(Single.never())

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        // Native Buy/Sell
        // TODO: AND-1691 Temporarily disabled while onboarding/announcements are rethought.
//        verify(buyDataManager).isCoinifyAllowed
//        verify(prefsUtil).getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false)
//        verify(prefsUtil, atLeastOnce()).setValue(
//            DashboardPresenter.NATIVE_BUY_SELL_DISMISSED,
//            true
//        )
        // KYC
        verify(kycTiersQueries).isKycInProgress()
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).scrollToTop()

        verify(swipeToReceiveHelper).storeEthAddress()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady onboarding complete with no native buy sell announcement`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn("USD")
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BTC),
                any()
            )
        ).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(
            4000.00
        )
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BCH),
                any()
            )
        ).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // No Native Buy/Sell announcement
        whenever(prefsUtil.getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false))
            .thenReturn(false)
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(false))
        // KYC Already dismissed
        whenever(prefsUtil.getValue(DashboardPresenter.KYC_INCOMPLETE_DISMISSED, false)).thenReturn(
            true
        )
        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))
        // Ignore Sunriver
        whenever(sunriverCampaignHelper.getCampaignCardType()).thenReturn(Single.never())

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).scrollToTop()
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        // Native Buy/Sell
        // TODO: AND-1691 Temporarily disabled while onboarding/announcements are rethought.
//        verify(buyDataManager).isCoinifyAllowed

        verify(swipeToReceiveHelper).storeEthAddress()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun updateBalances() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn("USD")
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BTC),
                any()
            )
        ).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(
            4000.00
        )
        whenever(
            exchangeRateFactory.getLastPrice(
                eq(CryptoCurrency.BCH),
                any()
            )
        ).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        givenBalance(210.bitcoin())
        givenBalance(200.bitcoinCash())
        givenBalance(220.ether())
        givenBalance(100.lumens())
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BTC, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.ETHER, "USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastPrice(CryptoCurrency.BCH, "USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedValueWithUnit(any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())
        // No Lockbox, not available
        whenever(lockboxDataManager.hasLockbox()).thenReturn(Single.just(false))
        whenever(lockboxDataManager.isLockboxAvailable()).thenReturn(Single.just(false))
        // Ignore Sunriver
        whenever(sunriverCampaignHelper.getCampaignCardType()).thenReturn(Single.never())

        // Act
        subject.updateBalances()

        // Assert
        verify(view, atLeastOnce()).scrollToTop()

        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.XLM), any())
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueries()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
    }

    private fun givenBalance(
        cryptoValue: CryptoValue
    ) {
        whenever(transactionListDataManager.totalBalance(cryptoValue.currency)).thenReturn(
            Single.just(
                TotalBalance.Balance(
                    spendable = cryptoValue,
                    watchOnly = cryptoValue.toZero(),
                    coldStorage = cryptoValue.toZero()
                )
            )
        )
    }

    private fun verifyBalanceQueries() {
        verify(transactionListDataManager).totalBalance(CryptoCurrency.BTC)
        verify(transactionListDataManager).totalBalance(CryptoCurrency.BCH)
        verify(transactionListDataManager).totalBalance(CryptoCurrency.ETHER)
        verify(transactionListDataManager).totalBalance(CryptoCurrency.XLM)
    }

    @Test
    fun onViewDestroyed() {
        // Arrange

        // Act
        subject.onViewDestroyed()
        // Assert
        verify(rxBus).unregister(eq(MetadataEvent::class.java), anyOrNull())
    }

    @Test
    fun `addSunriverPrompts type none`() {
        // Arrange
        whenever(sunriverCampaignHelper.getCampaignCardType()).thenReturn(Single.just(SunriverCardType.None))
        // Act
        subject.addSunriverPrompts()
        // Assert
        verifyZeroInteractions(view)
    }

    @Test
    fun `addSunriverPrompts type JoinWaitList`() {
        // Arrange
        whenever(sunriverCampaignHelper.getCampaignCardType())
            .thenReturn(Single.just(SunriverCardType.JoinWaitList))
        // Act
        subject.addSunriverPrompts()
        // Assert
        verify(view).notifyItemAdded(any(), eq(0))
        verify(view).scrollToTop()
    }

    @Test
    fun `addSunriverPrompts type FinishSignUp ignored as already dismissed`() {
        // Arrange
        whenever(sunriverCampaignHelper.getCampaignCardType())
            .thenReturn(Single.just(SunriverCardType.FinishSignUp))
        whenever(prefsUtil.getValue(SunriverCardType.FinishSignUp.javaClass.simpleName, false))
            .thenReturn(true)
        // Act
        subject.addSunriverPrompts()
        // Assert
        verifyZeroInteractions(view)
    }
}
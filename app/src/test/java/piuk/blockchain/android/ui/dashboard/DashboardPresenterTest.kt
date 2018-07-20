package piuk.blockchain.android.ui.dashboard

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.ui.home.models.MetadataEvent
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PrefsUtil
import java.math.BigInteger
import java.util.Locale

class DashboardPresenterTest : RxTest() {

    private lateinit var subject: DashboardPresenter
    private val prefsUtil: PrefsUtil = mock()
    private val exchangeRateFactory: ExchangeRateDataManager = mock()
    private val ethDataManager: EthDataManager = mock()
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

    @Before
    fun setUp() {
        subject = DashboardPresenter(
            prefsUtil,
            exchangeRateFactory,
            ethDataManager,
            bchDataManager,
            payloadDataManager,
            transactionListDataManager,
            stringUtils,
            accessState,
            buyDataManager,
            rxBus,
            swipeToReceiveHelper,
            currencyFormatManager
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
        whenever(exchangeRateFactory.getLastBtcPrice(any())).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastEthPrice(any())).thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastBchPrice(any())).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        val bchBalance = 20_000_000_000L
        whenever(transactionListDataManager.getBchBalance(any())).thenReturn(bchBalance)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastBchPrice("USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedBtcValueWithUnit(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedBchValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // Native Buy/Sell not available
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(false))

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).locale
        verify(view, atLeastOnce()).scrollToTop()
        verify(prefsUtil, atLeastOnce()).getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false)

        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastBtcPrice(any())
        verify(exchangeRateFactory, atLeastOnce()).getLastBchPrice(any())
        verify(exchangeRateFactory, atLeastOnce()).getLastEthPrice(any())
        verify(ethDataManager).fetchEthAddress()
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verify(transactionListDataManager).getBtcBalance(any())
        verify(transactionListDataManager).getBchBalance(any())
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        verify(buyDataManager).isCoinifyAllowed

        verify(swipeToReceiveHelper).storeEthAddress()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(ethDataManager)
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
        whenever(exchangeRateFactory.getLastBtcPrice(any())).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastEthPrice(any())).thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastBchPrice(any())).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(false)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        val bchBalance = 20_000_000_000L
        whenever(transactionListDataManager.getBchBalance(any())).thenReturn(bchBalance)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastBchPrice("USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedBtcValueWithUnit(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedBchValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // No Native Buy/Sell announcement
        whenever(prefsUtil.getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false))
            .thenReturn(true)
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(false))

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).locale
        verify(view, atLeastOnce()).scrollToTop()
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastBtcPrice(any())
        verify(exchangeRateFactory, atLeastOnce()).getLastBchPrice(any())
        verify(exchangeRateFactory, atLeastOnce()).getLastEthPrice(any())
        verify(ethDataManager).fetchEthAddress()
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verify(transactionListDataManager).getBtcBalance(any())
        verify(transactionListDataManager).getBchBalance(any())
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        // no announcements allowed while onboarding hasn't been completed

        verify(swipeToReceiveHelper).storeEthAddress()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(ethDataManager)
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
        whenever(exchangeRateFactory.getLastBtcPrice(any())).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastEthPrice(any())).thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastBchPrice(any())).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        val bchBalance = 20_000_000_000L
        whenever(transactionListDataManager.getBchBalance(any())).thenReturn(bchBalance)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastBchPrice("USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedBtcValueWithUnit(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedBchValueWithUnit(any(), any()))
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

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).locale
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastBtcPrice(any())
        verify(exchangeRateFactory, atLeastOnce()).getLastBchPrice(any())
        verify(exchangeRateFactory, atLeastOnce()).getLastEthPrice(any())
        verify(ethDataManager).fetchEthAddress()
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verify(transactionListDataManager).getBtcBalance(any())
        verify(transactionListDataManager).getBchBalance(any())
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        // Native Buy/Sell
        verify(buyDataManager).isCoinifyAllowed
        verify(prefsUtil).getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false)
        verify(prefsUtil, atLeastOnce()).setValue(
            DashboardPresenter.NATIVE_BUY_SELL_DISMISSED,
            true
        )
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).scrollToTop()

        verify(swipeToReceiveHelper).storeEthAddress()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(ethDataManager)
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
        whenever(exchangeRateFactory.getLastBtcPrice(any())).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastEthPrice(any())).thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastBchPrice(any())).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        val bchBalance = 20_000_000_000L
        whenever(transactionListDataManager.getBchBalance(any())).thenReturn(bchBalance)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastBchPrice("USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedBtcValueWithUnit(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedBchValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // checkLatestAnnouncements()
        // No Native Buy/Sell announcement
        whenever(prefsUtil.getValue(DashboardPresenter.NATIVE_BUY_SELL_DISMISSED, false))
            .thenReturn(false)
        whenever(buyDataManager.isCoinifyAllowed).thenReturn(Observable.just(false))

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(view, atLeastOnce()).scrollToTop()
        verify(view, atLeastOnce()).locale
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastBtcPrice(any())
        verify(exchangeRateFactory, atLeastOnce()).getLastBchPrice(any())
        verify(exchangeRateFactory, atLeastOnce()).getLastEthPrice(any())
        verify(ethDataManager).fetchEthAddress()
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verify(transactionListDataManager).getBtcBalance(any())
        verify(transactionListDataManager).getBchBalance(any())
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        // Native Buy/Sell
        verify(buyDataManager).isCoinifyAllowed

        verify(swipeToReceiveHelper).storeEthAddress()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `updateBalances`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn("USD")
        whenever(exchangeRateFactory.getLastBtcPrice(any())).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastEthPrice(any())).thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastBchPrice(any())).thenReturn(3000.00)

        // getOnboardingStatusObservable()
        val metadataObservable = Observable.just(MetadataEvent.SETUP_COMPLETE)
        whenever(rxBus.register(MetadataEvent::class.java)).thenReturn(metadataObservable)
        whenever(prefsUtil.getValue(PrefsUtil.KEY_ONBOARDING_COMPLETE, false))
            .thenReturn(true)
        whenever(accessState.isNewlyCreated).thenReturn(false)

        // doOnSuccess { updateAllBalances() }
        val combinedEthModel: CombinedEthModel = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.just(combinedEthModel))
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(payloadDataManager.updateAllTransactions()).thenReturn(Completable.complete())
        val btcBalance = 21_000_000_000L
        whenever(transactionListDataManager.getBtcBalance(any())).thenReturn(btcBalance)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
        val bchBalance = 20_000_000_000L
        whenever(transactionListDataManager.getBchBalance(any())).thenReturn(bchBalance)
        whenever(exchangeRateFactory.getLastBtcPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastEthPrice("USD")).thenReturn(2.0)
        whenever(exchangeRateFactory.getLastBchPrice("USD")).thenReturn(2.0)

        // PieChartsState
        whenever(currencyFormatManager.getFiatSymbol(any(), any())).thenReturn("$")
        whenever(currencyFormatManager.getFormattedFiatValueFromBtcValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromEthValueWithSymbol(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedFiatValueFromBchValueWithSymbol(any(), any()))
            .thenReturn("$2.00")

        whenever(currencyFormatManager.getFormattedBtcValueWithUnit(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedEthShortValueWithUnit(any(), any()))
            .thenReturn("$2.00")
        whenever(currencyFormatManager.getFormattedBchValueWithUnit(any(), any()))
            .thenReturn("$2.00")

        // storeSwipeToReceiveAddresses()
        whenever(bchDataManager.getWalletTransactions(any(), any()))
            .thenReturn(Observable.empty())

        // Act
        subject.updateBalances()

        // Assert
        verify(view, atLeastOnce()).locale
        verify(view, atLeastOnce()).scrollToTop()

        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastBtcPrice(any())
        verify(exchangeRateFactory, atLeastOnce()).getLastBchPrice(any())
        verify(exchangeRateFactory, atLeastOnce()).getLastEthPrice(any())
        verify(ethDataManager).fetchEthAddress()
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verify(transactionListDataManager).getBtcBalance(any())
        verify(transactionListDataManager).getBchBalance(any())
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(ethDataManager)
        verifyNoMoreInteractions(payloadDataManager)
        verifyNoMoreInteractions(transactionListDataManager)
        verifyNoMoreInteractions(exchangeRateFactory)
    }

    @Test
    fun onViewDestroyed() {
        // Arrange

        // Act
        subject.onViewDestroyed()
        // Assert
        verify(rxBus).unregister(eq(MetadataEvent::class.java), anyOrNull())
    }
}
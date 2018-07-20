package piuk.blockchain.android.ui.dashboard

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.anyOrNull
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.AccountKey
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import io.reactivex.Completable
import io.reactivex.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.android.data.ethereum.EthDataManager
import piuk.blockchain.android.testutils.rxInit
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

class DashboardPresenterTest {

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

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

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
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BTC), any())).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BCH), any())).thenReturn(3000.00)

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
        givenBtcBalance(21_000_000_000L)
        givenBchBalance(20_000_000_000L)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
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
        // No bch or sfox announcements
        whenever(prefsUtil.getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false))
            .thenReturn(true)
        whenever(buyDataManager.isSfoxAllowed).thenReturn(Observable.just(false))

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
        verify(ethDataManager).fetchEthAddress()
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueriesForBtcAndBch()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        verify(prefsUtil).getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false)
        verify(buyDataManager).isSfoxAllowed

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
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BTC), any())).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BCH), any())).thenReturn(3000.00)

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
        givenBtcBalance(21_000_000_000L)
        givenBchBalance(20_000_000_000L)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
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
        // No bch or sfox announcements
        whenever(prefsUtil.getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false))
            .thenReturn(true)
        whenever(buyDataManager.isSfoxAllowed).thenReturn(Observable.just(false))

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
        verify(ethDataManager).fetchEthAddress()
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueriesForBtcAndBch()
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
    fun `onViewReady onboarding complete with bch and Sfox announcement`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn("USD")
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BTC), any())).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BCH), any())).thenReturn(3000.00)

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
        givenBtcBalance(21_000_000_000L)
        givenBchBalance(20_000_000_000L)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
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
        // No bch or sfox announcements
        whenever(prefsUtil.getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false))
            .thenReturn(false)
        whenever(buyDataManager.isSfoxAllowed).thenReturn(Observable.just(true))
        whenever(prefsUtil.getValue(DashboardPresenter.SFOX_ANNOUNCEMENT_DISMISSED, false))
            .thenReturn(false)

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(ethDataManager).fetchEthAddress()
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueriesForBtcAndBch()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        // BCH
        verify(prefsUtil).getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false)
        verify(
            prefsUtil,
            atLeastOnce()
        ).setValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, true)
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).scrollToTop()
        // SFOX
        verify(buyDataManager).isSfoxAllowed
        verify(prefsUtil).getValue(DashboardPresenter.SFOX_ANNOUNCEMENT_DISMISSED, false)
        verify(prefsUtil, atLeastOnce()).setValue(
            DashboardPresenter.SFOX_ANNOUNCEMENT_DISMISSED,
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
    fun `onViewReady onboarding complete with bch but no Sfox announcement`() {
        // Arrange
        whenever(stringUtils.getString(any())).thenReturn("")

        // updatePrices()
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(currencyFormatManager.getFormattedFiatValueWithSymbol(any())).thenReturn("$2.00")
        whenever(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
            .thenReturn("USD")
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BTC), any())).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BCH), any())).thenReturn(3000.00)

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
        givenBtcBalance(21_000_000_000L)
        givenBchBalance(20_000_000_000L)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
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
        // No bch or sfox announcements
        whenever(prefsUtil.getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false))
            .thenReturn(false)
        whenever(buyDataManager.isSfoxAllowed).thenReturn(Observable.just(false))

        // Act
        subject.onViewReady()

        // Assert
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).notifyItemUpdated(any(), any())
        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(ethDataManager).fetchEthAddress()
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueriesForBtcAndBch()
        verify(bchDataManager, atLeastOnce()).updateAllBalances()

        // PieChartsState
        verify(view, atLeastOnce()).updatePieChartState(any())

        // storeSwipeToReceiveAddresses()
        verify(view, atLeastOnce()).startWebsocketService()

        // checkLatestAnnouncements()
        // BCH
        verify(prefsUtil).getValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, false)
        verify(
            prefsUtil,
            atLeastOnce()
        ).setValue(DashboardPresenter.BITCOIN_CASH_ANNOUNCEMENT_DISMISSED, true)
        verify(view, atLeastOnce()).notifyItemAdded(any(), eq(0))
        verify(view, atLeastOnce()).scrollToTop()
        // SFOX
        verify(buyDataManager).isSfoxAllowed

        verify(swipeToReceiveHelper).storeEthAddress()

        verifyNoMoreInteractions(exchangeRateFactory)
        verifyNoMoreInteractions(ethDataManager)
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
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BTC), any())).thenReturn(5000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.ETHER), any())).thenReturn(4000.00)
        whenever(exchangeRateFactory.getLastPrice(eq(CryptoCurrency.BCH), any())).thenReturn(3000.00)

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
        givenBtcBalance(21_000_000_000L)
        givenBchBalance(20_000_000_000L)
        val ethBalance = 22_000_000_000L
        whenever(combinedEthModel.getTotalBalance()).thenReturn(BigInteger.valueOf(ethBalance))
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

        // Act
        subject.updateBalances()

        // Assert
        verify(view, atLeastOnce()).scrollToTop()

        verify(exchangeRateFactory, atLeastOnce()).updateTickers()
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BTC), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.ETHER), any())
        verify(exchangeRateFactory, atLeastOnce()).getLastPrice(eq(CryptoCurrency.BCH), any())
        verify(ethDataManager).fetchEthAddress()
        verify(payloadDataManager).updateAllBalances()
        verify(payloadDataManager).updateAllTransactions()
        verifyBalanceQueriesForBtcAndBch()
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

    private fun givenBtcBalance(balance: Long) {
        givenBalance(CryptoCurrency.BTC, balance)
    }

    private fun givenBchBalance(balance: Long) {
        givenBalance(CryptoCurrency.BCH, balance)
    }

    private fun givenBalance(cryptoCurrency: CryptoCurrency, balance: Long) {
        whenever(transactionListDataManager.balance(argThat { currency == cryptoCurrency })).thenReturn(
            CryptoValue(cryptoCurrency, balance.toBigInteger())
        )
    }

    private fun verifyBalanceQueriesForBtcAndBch() {
        verify(transactionListDataManager).balance(argThat {
            currency == CryptoCurrency.BTC && this is AccountKey.EntireWallet
        })
        verify(transactionListDataManager).balance(argThat {
            currency == CryptoCurrency.BTC && this is AccountKey.WatchOnly
        })
        verify(transactionListDataManager).balance(argThat {
            currency == CryptoCurrency.BCH && this is AccountKey.EntireWallet
        })
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
package piuk.blockchain.android.ui.balance

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.isNull
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.schedulers.TestScheduler
import org.amshove.kluent.`should equal to`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager
import piuk.blockchain.androidcore.data.ethereum.EthDataManager
import com.blockchain.notifications.models.NotificationPayload
import com.blockchain.android.testutils.rxInit
import piuk.blockchain.android.ui.account.ItemAccount
import piuk.blockchain.android.ui.receive.WalletAccountHelper
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.BlockchainDetails
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.androidbuysell.models.coinify.EventData
import piuk.blockchain.androidbuysell.models.coinify.Transfer
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.access.AuthEvent
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import info.blockchain.balance.CryptoCurrency
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager
import piuk.blockchain.androidcore.data.currency.CurrencyState
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager
import piuk.blockchain.androidcore.data.payload.PayloadDataManager
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import piuk.blockchain.androidcore.data.transactions.models.BtcDisplayable
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.ui.base.UiState
import java.math.BigInteger

@Suppress("IllegalIdentifier")
class BalancePresenterTest {

    private lateinit var subject: BalancePresenter
    private var view: BalanceView = mock()
    private var exchangeRateFactory: ExchangeRateDataManager = mock()
    private var transactionListDataManager: TransactionListDataManager = mock()
    private var swipeToReceiveHelper: SwipeToReceiveHelper = mock()
    private var payloadDataManager: PayloadDataManager = mock()
    private var buyDataManager: BuyDataManager = mock()
    private var stringUtils: StringUtils = mock()
    private var prefsUtil: PrefsUtil = mock()
    private var currencyState: CurrencyState = mock()
    private var rxBus: RxBus = mock()
    private var ethDataManager: EthDataManager = mock()
    private var shapeShiftDataManager: ShapeShiftDataManager = mock()
    private val bchDataManager: BchDataManager = mock()
    private val walletAccountHelper: WalletAccountHelper = mock()
    private val environmentSettings: EnvironmentConfig = mock()
    private val currencyFormatManager: CurrencyFormatManager = mock()
    private val exchangeService: ExchangeService = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val testScheduler: TestScheduler = TestScheduler()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
        computation(testScheduler)
    }

    @Before
    fun setUp() {

        subject = BalancePresenter(
            exchangeRateFactory,
            transactionListDataManager,
            ethDataManager,
            swipeToReceiveHelper,
            payloadDataManager,
            buyDataManager,
            stringUtils,
            prefsUtil,
            rxBus,
            currencyState,
            shapeShiftDataManager,
            bchDataManager,
            walletAccountHelper,
            environmentSettings,
            currencyFormatManager,
            exchangeService,
            coinifyDataManager
        )
        subject.initView(view)
    }

    @Test
    fun onViewReady() {

        // Arrange
        whenever(environmentSettings.environment).thenReturn(Environment.PRODUCTION)
        val account: ItemAccount = mock()
        whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(mutableListOf(account))
        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)

        whenever(rxBus.register(NotificationPayload::class.java)).thenReturn(Observable.empty())
        whenever(rxBus.register(AuthEvent::class.java)).thenReturn(Observable.empty())

        // Act
        subject.onViewReady()

        // Assert
        verify(view).setupAccountsAdapter(mutableListOf(account))
        verify(view).setupTxFeedAdapter(true)
    }

    @Test
    fun onViewDestroyed() {
        // Arrange
        val notificationObservable = Observable.just(NotificationPayload(emptyMap()))
        val authEventObservable = Observable.just(AuthEvent.LOGOUT)
        subject.notificationObservable = notificationObservable
        subject.authEventObservable = authEventObservable
        // Act
        subject.onViewDestroyed()
        // Assert
        verify(rxBus).unregister(NotificationPayload::class.java, notificationObservable)
        verify(rxBus).unregister(AuthEvent::class.java, authEventObservable)
    }

    @Test
    fun onResume() {
        // Child function onRefreshRequested
    }

    @Test
    fun areLauncherShortcutsEnabled() {
        // Arrange
        whenever(prefsUtil.getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true))
            .thenReturn(false)
        // Act
        val result = subject.areLauncherShortcutsEnabled()
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_RECEIVE_SHORTCUTS_ENABLED, true)
        verifyNoMoreInteractions(prefsUtil)
        result `should equal to` false
    }

    @Test
    fun onRefreshRequested() {
        // Arrange

        // getCurrentAccount()
        whenever(view.getCurrentAccountPosition()).thenReturn(0)
        val account: ItemAccount = mock()
        whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(mutableListOf(account))
        whenever(account.displayBalance).thenReturn("0.052 BTC")

        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        whenever(bchDataManager.refreshMetadataCompletable()).thenReturn(Completable.complete())
        whenever(ethDataManager.fetchEthAddress()).thenReturn(Observable.empty())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        whenever(payloadDataManager.updateAllBalances()).thenReturn(Completable.complete())
        whenever(transactionListDataManager.fetchTransactions(any(), any(), any()))
            .thenReturn(Observable.empty())

        whenever(currencyState.isDisplayingCryptoCurrency).thenReturn(true)

        // Act
        subject.onRefreshRequested()

        // Assert
        verify(view).setUiState(UiState.LOADING)
        verify(view, times(2)).updateSelectedCurrency(CryptoCurrency.BTC)
        verify(view, times(2)).updateBalanceHeader("0.052 BTC")
        verify(view, times(2)).updateAccountsDataSet(mutableListOf(account))
        verify(view).generateLauncherShortcuts()
        verify(view).updateTransactionValueType(true)
    }

    @Test
    fun updateBalancesCompletable() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        // Act
        subject.updateBalancesCompletable()
        // Assert
        verify(payloadDataManager).updateAllBalances()

        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.ETHER)
        // Act
        subject.updateBalancesCompletable()
        // Assert
        verify(ethDataManager).fetchEthAddressCompletable()

        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BCH)
        // Act
        subject.updateBalancesCompletable()
        // Assert
        verify(bchDataManager).updateAllBalances()
    }

    @Test
    fun getUpdateTickerCompletable() {
        // Arrange
        whenever(exchangeRateFactory.updateTickers()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.getUpdateTickerCompletable().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun updateEthAddress() {
        // Arrange
        val abc: EthAddressResponseMap = mock()
        whenever(ethDataManager.fetchEthAddress()).thenReturn(
            Observable.just(
                CombinedEthModel(
                    abc
                )
            )
        )
        // Act
        val testObserver = subject.updateEthAddress().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun updateBchWallet() {
        // Arrange
        whenever(bchDataManager.refreshMetadataCompletable()).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.updateBchWallet().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
    }

    @Test
    fun `onGetBitcoinClicked API less than 19 canBuy returns true`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(true))
        whenever(view.shouldShowBuy()).thenReturn(false)
        // Act
        subject.onGetBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).shouldShowBuy()
        verify(view).startReceiveFragmentBtc()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onGetBitcoinClicked API less than 19 canBuy returns false`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        whenever(view.shouldShowBuy()).thenReturn(false)
        // Act
        subject.onGetBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).startReceiveFragmentBtc()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onGetBitcoinClicked canBuy returns true`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(true))
        whenever(view.shouldShowBuy()).thenReturn(true)
        // Act
        subject.onGetBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).shouldShowBuy()
        verify(view).startBuyActivity()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onGetBitcoinClicked canBuy returns false`() {
        // Arrange
        whenever(buyDataManager.canBuy).thenReturn(Observable.just(false))
        whenever(view.shouldShowBuy()).thenReturn(true)
        // Act
        subject.onGetBitcoinClicked()
        // Assert
        verify(buyDataManager).canBuy
        verifyNoMoreInteractions(buyDataManager)
        verify(view).startReceiveFragmentBtc()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun refreshBalanceHeader() {
        // Arrange
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        val account: ItemAccount = mock()
        val value = "0.052 BTC"
        whenever(account.displayBalance).thenReturn(value)
        // Act
        subject.refreshBalanceHeader(account)
        // Assert
        verify(view).updateSelectedCurrency(CryptoCurrency.BTC)
        verify(view).updateBalanceHeader(value)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun refreshAccountDataSet() {
        // Arrange
        val mockList = mutableListOf<ItemAccount>()
        whenever(walletAccountHelper.getAccountItemsForOverview()).thenReturn(mockList)
        // Act
        subject.refreshAccountDataSet()
        // Assert
        verify(view).updateAccountsDataSet(mockList)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `update transactions list with coinify labels`() {
        // Arrange
        // Transaction setup
        val itemAccount = ItemAccount()
        val txHash = "TX_HASH"
        val displayable: BtcDisplayable = mock()
        whenever(displayable.hash).thenReturn(txHash)
        whenever(displayable.total).thenReturn(BigInteger.ZERO)
        whenever(displayable.totalDisplayableCrypto).thenReturn("0")
        whenever(displayable.totalDisplayableFiat).thenReturn("0")
        whenever(transactionListDataManager.fetchTransactions(itemAccount, 50, 0))
            .thenReturn(Observable.just(listOf(displayable)))
        // Exchange token setup
        val token = "TOKEN"
        val coinifyData = CoinifyData(1, token)
        val exchangeData = ExchangeData().apply { coinify = coinifyData }
        whenever(exchangeService.getExchangeMetaData())
            .thenReturn(Observable.just(exchangeData))
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))
        // Coinify trade setup
        val coinifyTrade: CoinifyTrade = mock()
        val tradeId = 12345
        whenever(coinifyTrade.id).thenReturn(tradeId)
        whenever(coinifyTrade.isSellTransaction()).thenReturn(false)
        val transferOut: Transfer = mock()
        whenever(coinifyTrade.transferOut).thenReturn(transferOut)
        val details = BlockchainDetails("", EventData(txHash, ""))
        whenever(transferOut.details).thenReturn(details)
        whenever(coinifyDataManager.getTrades(token)).thenReturn(Observable.just(coinifyTrade))
        // ShapeShift
        whenever(shapeShiftDataManager.getTradesList()).thenReturn(Observable.just(emptyList()))
        // Utils
        whenever(stringUtils.getFormattedString(any(), any())).thenReturn(tradeId.toString())
        whenever(currencyState.cryptoCurrency).thenReturn(CryptoCurrency.BTC)
        whenever(currencyFormatManager.getFormattedBtcValueWithUnit(any(), any())).thenReturn("")
        whenever(
            currencyFormatManager.getFormattedFiatValueFromSelectedCoinValueWithSymbol(
                any(),
                any(),
                isNull()
            )
        )
            .thenReturn("")
        // Act
        val testObserver = subject.updateTransactionsListCompletable(itemAccount).test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        verify(view).updateTransactionDataSet(any(), any())
        verify(displayable).note = tradeId.toString()
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getTrades(token)
    }
}

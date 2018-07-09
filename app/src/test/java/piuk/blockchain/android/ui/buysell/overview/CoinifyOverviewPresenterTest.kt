package piuk.blockchain.android.ui.buysell.overview

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.models.coinify.TradeState
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.metadata.MetadataManager
import java.util.Locale

class CoinifyOverviewPresenterTest : RxTest() {

    private lateinit var subject: CoinifyOverviewPresenter
    private val exchangeService: ExchangeService = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val metadataManager: MetadataManager = mock()
    private val stringUtils: StringUtils = mock()
    private val view: CoinifyOverviewView = mock()

    private val token = "TOKEN"

    @Before
    fun setUp() {
        subject = CoinifyOverviewPresenter(
            exchangeService,
            coinifyDataManager,
            metadataManager,
            stringUtils
        )
        subject.initView(view)

        whenever(view.locale).thenReturn(Locale.US)
    }

    @Test
    @Throws(Exception::class)
    fun `refreshTransactionList success`() {
        // Arrange
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))
            // Second invocation will be for comparing metadata, which we aren't testing right now
            .thenReturn(Observable.error { Throwable() })
        val coinifyTrade = CoinifyTrade(
            id = 12345,
            traderId = 12345,
            state = TradeState.Processing,
            inCurrency = "GBP",
            outCurrency = "BTC",
            inAmount = 1.0,
            outAmount = 1.0,
            outAmountExpected = 1.0,
            transferIn = mock(),
            transferOut = mock(),
            receiptUrl = null,
            quoteExpireTime = null,
            updateTime = "2016-07-07T12:10:19Z",
            createTime = "2016-07-07T12:10:19Z"
        )
        whenever(coinifyDataManager.getTrades(token))
            .thenReturn(Observable.just(coinifyTrade))
        // Act
        subject.refreshTransactionList()
        // Assert
        verify(exchangeService, times(2)).getExchangeMetaData()
        verify(coinifyDataManager).getTrades(token)
        verify(view).renderViewState(any(OverViewState.Data::class))
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `refreshTransactionList failure`() {
        // Arrange
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))

        whenever(coinifyDataManager.getTrades(token))
            .thenReturn(Observable.error { Throwable() })
        // Act
        subject.refreshTransactionList()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getTrades(token)
        verify(view).renderViewState(any(OverViewState.Failure::class))
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onBuySelected no pending kyc`() {
        // Arrange
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))

        val kycResponse: KycResponse = mock()
        val reviewState = ReviewState.Completed
        whenever(kycResponse.state).thenReturn(reviewState)
        whenever(coinifyDataManager.getKycReviews(token))
            .thenReturn(Single.just(listOf(kycResponse)))
        // Act
        subject.onBuySelected()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getKycReviews(token)
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).launchBuyPaymentSelectionFlow()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onBuySelected reviewing kyc`() {
        // Arrange
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))

        val kycResponse: KycResponse = mock()
        val reviewState = ReviewState.Reviewing
        whenever(kycResponse.state).thenReturn(reviewState)
        whenever(coinifyDataManager.getKycReviews(token))
            .thenReturn(Single.just(listOf(kycResponse)))
        // Act
        subject.onBuySelected()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getKycReviews(token)
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).launchCardBuyFlow()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onBuySelected failure`() {
        // Arrange
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))

        whenever(coinifyDataManager.getKycReviews(token))
            .thenReturn(Single.error { Throwable() })
        // Act
        subject.onBuySelected()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getKycReviews(token)
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).renderViewState(any(OverViewState.Failure::class))
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onSellSelected no pending kyc`() {
        // Arrange
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))

        val kycResponse: KycResponse = mock()
        val reviewState = ReviewState.Completed
        whenever(kycResponse.state).thenReturn(reviewState)
        whenever(coinifyDataManager.getKycReviews(token))
            .thenReturn(Single.just(listOf(kycResponse)))
        // Act
        subject.onSellSelected()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getKycReviews(token)
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).launchSellFlow()
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onSellSelected reviewing kyc`() {
        // Arrange
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))

        val kycResponse: KycResponse = mock()
        val reviewState = ReviewState.Reviewing
        whenever(kycResponse.state).thenReturn(reviewState)
        whenever(coinifyDataManager.getKycReviews(token))
            .thenReturn(Single.just(listOf(kycResponse)))
        // Act
        subject.onSellSelected()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getKycReviews(token)
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showAlertDialog(any(Int::class))
        verifyNoMoreInteractions(view)
    }

    @Test
    @Throws(Exception::class)
    fun `onSellSelected failure`() {
        // Arrange
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))

        whenever(coinifyDataManager.getKycReviews(token))
            .thenReturn(Single.error { Throwable() })
        // Act
        subject.onSellSelected()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getKycReviews(token)
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).renderViewState(any(OverViewState.Failure::class))
        verifyNoMoreInteractions(view)
    }
}
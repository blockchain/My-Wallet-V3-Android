package piuk.blockchain.android.ui.buysell.coinify.launcher

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.RxTest
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherPresenter
import piuk.blockchain.android.ui.buysell.launcher.BuySellLauncherView
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.services.ExchangeService

class BuySellLauncherPresenterTest : RxTest() {

    private lateinit var subject: BuySellLauncherPresenter

    private val view: BuySellLauncherView = mock()
    private var exchangeService: ExchangeService = mock()
    private var coinifyDataManager: CoinifyDataManager = mock()

    @Before
    fun setup() {
        subject = BuySellLauncherPresenter(exchangeService, coinifyDataManager)
        subject.initView(view)
    }

    @Test
    fun `onViewReady has coinify token and has completed KYC`() {
        // Arrange
        val exchange: ExchangeData = mock()
        val coinify: CoinifyData = mock()
        val token = "TOKEN"
        whenever(coinify.token).thenReturn(token)
        whenever(exchange.coinify).thenReturn(coinify)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchange))
        val kycResponse: KycResponse = mock()
        whenever(coinifyDataManager.getKycReviews(token)).thenReturn(Single.just(listOf(kycResponse)))
        whenever(kycResponse.state).thenReturn(ReviewState.Completed)
        // Act
        subject.onViewReady()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getKycReviews(token)
        verify(view).onStartCoinifyOverview()
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady has coinify token and has not completed KYC`() {
        // Arrange
        val exchange: ExchangeData = mock()
        val coinify: CoinifyData = mock()
        val token = "TOKEN"
        whenever(coinify.token).thenReturn(token)
        whenever(exchange.coinify).thenReturn(coinify)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchange))
        val kycResponse: KycResponse = mock()
        whenever(coinifyDataManager.getKycReviews(token)).thenReturn(Single.just(listOf(kycResponse)))
        whenever(kycResponse.state).thenReturn(ReviewState.Rejected)
        // Act
        subject.onViewReady()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getKycReviews(token)
        verify(view).onStartCoinifySignUp()
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady has exchange data but no coinify token`() {
        // Arrange
        val exchange: ExchangeData = mock()
        val coinify: CoinifyData = mock()
        whenever(coinify.token).thenReturn(null)
        whenever(exchange.coinify).thenReturn(coinify)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchange))
        // Act
        subject.onViewReady()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(view).onStartCoinifySignUp()
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady no exchange data`() {
        // Arrange
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(ExchangeData()))
        // Act
        subject.onViewReady()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(view).onStartCoinifySignUp()
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
    }

    @Test
    fun `onViewReady network exception`() {
        // Arrange
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verify(view).displayProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showErrorToast(any())
        verify(view).finishPage()
    }
}
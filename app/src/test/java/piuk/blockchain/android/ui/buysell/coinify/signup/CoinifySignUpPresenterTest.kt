package piuk.blockchain.android.ui.buysell.coinify.signup

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.util.StringUtils
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.models.coinify.Trader
import piuk.blockchain.androidbuysell.services.ExchangeService

class CoinifySignUpPresenterTest : RxTest() {

    private lateinit var subject: CoinifySignUpPresenter

    private val view: CoinifySignupView = mock()
    private val exchangeService: ExchangeService = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val stringUtils: StringUtils = mock()

    @Before
    fun setup() {
        subject = CoinifySignUpPresenter(
            exchangeService,
            coinifyDataManager,
            stringUtils
        )
        subject.initView(view)
    }

    @Test
    fun `onViewReady error`() {
        // Arrange
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.error(Throwable("Forced fail")))
        whenever(stringUtils.getString(any())).thenReturn("")
        // Act
        subject.onViewReady()
        // Assert
        verify(view).showToast(any())
        verify(view).onFinish()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady no Coinify metadata`() {
        // Arrange
        val mockExchangeData: ExchangeData = mock()
        whenever(mockExchangeData.coinify).thenReturn(null)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(mockExchangeData))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).onStartWelcome()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady no kyc responses`() {

        // Arrange
        val mockExchangeData: ExchangeData = mock()
        val mockCoinifyData: CoinifyData = mock()
        whenever(mockExchangeData.coinify).thenReturn(mockCoinifyData)
        whenever(mockCoinifyData.token).thenReturn("token")
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(mockExchangeData))

        val mockTrader: Trader = mock()
        whenever(coinifyDataManager.getTrader(any())).thenReturn(Single.just(mockTrader))

        val kycResponseList = emptyList<KycResponse>()
        whenever(coinifyDataManager.getKycReviews(any())).thenReturn(Single.just(kycResponseList))

        val kycResponse: KycResponse = mock()
        whenever(kycResponse.redirectUrl).thenReturn(REDIRECT_URL)
        whenever(kycResponse.externalId).thenReturn(KYC_EXTERNAL_ID)
        whenever(coinifyDataManager.startKycReview(any())).thenReturn(Single.just(kycResponse))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).onStartVerifyIdentification(REDIRECT_URL, KYC_EXTERNAL_ID)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady 1 Completed kyc`() {
        // Arrange
        val mockExchangeData: ExchangeData = mock()
        val mockCoinifyData: CoinifyData = mock()
        whenever(mockExchangeData.coinify).thenReturn(mockCoinifyData)
        whenever(mockCoinifyData.token).thenReturn("token")
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(mockExchangeData))

        val mockTrader: Trader = mock()
        whenever(coinifyDataManager.getTrader(any())).thenReturn(Single.just(mockTrader))

        val kyc1: KycResponse = mock()
        whenever(kyc1.state).thenReturn(ReviewState.Completed)
        val kycResponseList = listOf(kyc1)
        whenever(coinifyDataManager.getKycReviews(any())).thenReturn(Single.just(kycResponseList))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).onStartOverview()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady 1 Reviewing kyc`() {
        // Arrange
        val mockExchangeData: ExchangeData = mock()
        val mockCoinifyData: CoinifyData = mock()
        whenever(mockExchangeData.coinify).thenReturn(mockCoinifyData)
        whenever(mockCoinifyData.token).thenReturn("token")
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(mockExchangeData))

        val mockTrader: Trader = mock()
        whenever(coinifyDataManager.getTrader(any())).thenReturn(Single.just(mockTrader))

        val kyc1: KycResponse = mock()
        whenever(kyc1.state).thenReturn(ReviewState.Reviewing)
        val kycResponseList = listOf(kyc1)
        whenever(coinifyDataManager.getKycReviews(any())).thenReturn(Single.just(kycResponseList))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).onStartOverview()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady 1 DocumentsRequested kyc`() {
        // Arrange
        val mockExchangeData: ExchangeData = mock()
        val mockCoinifyData: CoinifyData = mock()
        whenever(mockExchangeData.coinify).thenReturn(mockCoinifyData)
        whenever(mockCoinifyData.token).thenReturn("token")
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(mockExchangeData))

        val mockTrader: Trader = mock()
        whenever(coinifyDataManager.getTrader(any())).thenReturn(Single.just(mockTrader))

        val kycResponse: KycResponse = mock()
        whenever(kycResponse.state).thenReturn(ReviewState.DocumentsRequested)
        whenever(kycResponse.redirectUrl).thenReturn(REDIRECT_URL)
        whenever(kycResponse.externalId).thenReturn(KYC_EXTERNAL_ID)
        val kycResponseList = listOf(kycResponse)
        whenever(coinifyDataManager.getKycReviews(any())).thenReturn(Single.just(kycResponseList))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).onStartVerifyIdentification(REDIRECT_URL, KYC_EXTERNAL_ID)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady 1 Pending kyc`() {
        // Arrange
        val mockExchangeData: ExchangeData = mock()
        val mockCoinifyData: CoinifyData = mock()
        whenever(mockExchangeData.coinify).thenReturn(mockCoinifyData)
        whenever(mockCoinifyData.token).thenReturn("token")
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(mockExchangeData))

        val mockTrader: Trader = mock()
        whenever(coinifyDataManager.getTrader(any())).thenReturn(Single.just(mockTrader))

        val kycResponse: KycResponse = mock()
        whenever(kycResponse.state).thenReturn(ReviewState.Pending)
        whenever(kycResponse.redirectUrl).thenReturn(REDIRECT_URL)
        whenever(kycResponse.externalId).thenReturn(KYC_EXTERNAL_ID)
        val kycResponseList = listOf(kycResponse)
        whenever(coinifyDataManager.getKycReviews(any())).thenReturn(Single.just(kycResponseList))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).onStartVerifyIdentification(REDIRECT_URL, KYC_EXTERNAL_ID)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady 1 Rejected kyc`() {
        // Arrange
        val mockExchangeData: ExchangeData = mock()
        val mockCoinifyData: CoinifyData = mock()
        whenever(mockExchangeData.coinify).thenReturn(mockCoinifyData)
        whenever(mockCoinifyData.token).thenReturn("token")
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(mockExchangeData))

        val mockTrader: Trader = mock()
        whenever(coinifyDataManager.getTrader(any())).thenReturn(Single.just(mockTrader))

        val kyc1: KycResponse = mock()
        whenever(kyc1.redirectUrl).thenReturn(REDIRECT_URL)
        whenever(kyc1.externalId).thenReturn(KYC_EXTERNAL_ID)
        whenever(kyc1.state).thenReturn(ReviewState.Rejected)
        whenever(coinifyDataManager.getKycReviews(any())).thenReturn(Single.just(listOf(kyc1)))

        val kyc2: KycResponse = mock()
        whenever(kyc1.state).thenReturn(ReviewState.DocumentsRequested)
        whenever(kyc2.redirectUrl).thenReturn(REDIRECT_URL)
        whenever(kyc2.externalId).thenReturn(KYC_EXTERNAL_ID)
        whenever(coinifyDataManager.startKycReview("token")).thenReturn(Single.just(kyc2))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).onStartVerifyIdentification(REDIRECT_URL, KYC_EXTERNAL_ID)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady 1 Completed and 1 Expired kyc`() {
        // Arrange
        val mockExchangeData: ExchangeData = mock()
        val mockCoinifyData: CoinifyData = mock()
        whenever(mockExchangeData.coinify).thenReturn(mockCoinifyData)
        whenever(mockCoinifyData.token).thenReturn("token")
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(mockExchangeData))

        val mockTrader: Trader = mock()
        whenever(coinifyDataManager.getTrader(any())).thenReturn(Single.just(mockTrader))

        val kyc1: KycResponse = mock()
        whenever(kyc1.state).thenReturn(ReviewState.Completed)
        val kyc2: KycResponse = mock()
        whenever(kyc2.state).thenReturn(ReviewState.Expired)
        val kycResponseList = listOf(kyc1, kyc2)
        whenever(coinifyDataManager.getKycReviews(any())).thenReturn(Single.just(kycResponseList))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).onStartOverview()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `continueVerifyIdentification 1 Reviewing kyc`() {
        // Arrange
        val mockExchangeData: ExchangeData = mock()
        val mockCoinifyData: CoinifyData = mock()
        whenever(mockExchangeData.coinify).thenReturn(mockCoinifyData)
        whenever(mockCoinifyData.token).thenReturn("token")
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(mockExchangeData))

        val mockTrader: Trader = mock()
        whenever(coinifyDataManager.getTrader(any())).thenReturn(Single.just(mockTrader))

        val kyc1: KycResponse = mock()
        whenever(kyc1.state).thenReturn(ReviewState.Reviewing)
        whenever(coinifyDataManager.getKycReviews(any())).thenReturn(Single.just(listOf(kyc1)))
        // Act
        subject.continueVerifyIdentification()
        // Assert
        verify(view).onStartOverview()
        verifyNoMoreInteractions(view)
    }

    companion object {
        private const val KYC_EXTERNAL_ID = "KYC_EXTERNAL_ID"
        private const val REDIRECT_URL = "REDIRECT_URL"
    }
}
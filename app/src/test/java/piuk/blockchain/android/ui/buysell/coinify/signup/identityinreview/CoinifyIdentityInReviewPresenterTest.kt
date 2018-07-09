package piuk.blockchain.android.ui.buysell.coinify.signup.identityinreview

import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.coinify.KycResponse
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.services.ExchangeService

class CoinifyIdentityInReviewPresenterTest : RxTest() {

    private lateinit var subject: CoinifyIdentityInReviewPresenter

    private val view: CoinifyIdentityInReviewView = mock()
    private val exchangeService: ExchangeService = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()

    @Before
    fun setup() {
        subject =
            CoinifyIdentityInReviewPresenter(
                exchangeService,
                coinifyDataManager
            )
        subject.initView(view)
    }

    @Test
    fun `filterReviewStatus at least 1 Completed`() {

        // Arrange
        val mockKyc1: KycResponse = mock()
        whenever(mockKyc1.state).thenReturn(ReviewState.Completed)

        val mockKyc2: KycResponse = mock()
        whenever(mockKyc2.state).thenReturn(ReviewState.Expired)

        val kycList = listOf(mockKyc1, mockKyc2)

        // Act
        subject.filterReviewStatus(kycList)

        // Assert
        verify(view).onShowCompleted()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `filterReviewStatus at Completed`() {

        // Arrange
        val mockKyc1: KycResponse = mock()
        whenever(mockKyc1.state).thenReturn(ReviewState.Completed)

        val kycList = listOf(mockKyc1)

        // Act
        subject.filterReviewStatus(kycList)

        // Assert
        verify(view).onShowCompleted()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `filterReviewStatus at Reviewing`() {

        // Arrange
        val mockKyc1: KycResponse = mock()
        whenever(mockKyc1.state).thenReturn(ReviewState.Reviewing)

        val kycList = listOf(mockKyc1)

        // Act
        subject.filterReviewStatus(kycList)

        // Assert
        verify(view).onShowReviewing()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `filterReviewStatus at Pending`() {

        // Arrange
        val mockKyc1: KycResponse = mock()
        whenever(mockKyc1.state).thenReturn(ReviewState.Pending)

        val kycList = listOf(mockKyc1)

        // Act
        subject.filterReviewStatus(kycList)

        // Assert
        verify(view).onShowPending()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `filterReviewStatus at DocumentsRequested`() {

        // Arrange
        val mockKyc1: KycResponse = mock()
        whenever(mockKyc1.state).thenReturn(ReviewState.DocumentsRequested)

        val kycList = listOf(mockKyc1)

        // Act
        subject.filterReviewStatus(kycList)

        // Assert
        verify(view).onShowDocumentsRequested()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `filterReviewStatus at Expired`() {

        // Arrange
        val mockKyc1: KycResponse = mock()
        whenever(mockKyc1.state).thenReturn(ReviewState.Expired)

        val kycList = listOf(mockKyc1)

        // Act
        subject.filterReviewStatus(kycList)

        // Assert
        verify(view).onShowExpired()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `filterReviewStatus at Failed`() {

        // Arrange
        val mockKyc1: KycResponse = mock()
        whenever(mockKyc1.state).thenReturn(ReviewState.Failed)

        val kycList = listOf(mockKyc1)

        // Act
        subject.filterReviewStatus(kycList)

        // Assert
        verify(view).onShowFailed()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `filterReviewStatus at Rejected`() {

        // Arrange
        val mockKyc1: KycResponse = mock()
        whenever(mockKyc1.state).thenReturn(ReviewState.Rejected)

        val kycList = listOf(mockKyc1)

        // Act
        subject.filterReviewStatus(kycList)

        // Assert
        verify(view).onShowRejected()
        verifyNoMoreInteractions(view)
    }
}
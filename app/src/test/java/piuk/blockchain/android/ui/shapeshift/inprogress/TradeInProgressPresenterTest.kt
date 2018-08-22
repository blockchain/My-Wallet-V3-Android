package piuk.blockchain.android.ui.shapeshift.inprogress

import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`should equal`
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Test
import piuk.blockchain.android.R
import piuk.blockchain.android.testutils.RxTest
import piuk.blockchain.android.ui.shapeshift.models.TradeProgressUiState
import piuk.blockchain.androidcore.data.shapeshift.ShapeShiftDataManager
import java.util.concurrent.TimeUnit

class TradeInProgressPresenterTest : RxTest() {

    private lateinit var subject: TradeInProgressPresenter
    private val shapeShiftDataManager: ShapeShiftDataManager = mock()
    private val view: TradeInProgressView = mock()
    private val depositAddress = "DEPOSIT_ADDRESS"
    private val noDepositState = TradeProgressUiState(
        R.string.morph_sending_title,
        R.string.morph_in_progress_explanation,
        R.drawable.trade_progress_airplane,
        true,
        1
    )
    private val receivedState = TradeProgressUiState(
        R.string.morph_in_progress_title,
        R.string.morph_in_progress_explanation,
        R.drawable.shapeshift_trade_progress_exchange,
        true,
        2
    )
    private val completeState = TradeProgressUiState(
        R.string.morph_complete_title,
        R.string.morph_in_progress_explanation,
        R.drawable.trade_progress_complete,
        true,
        3
    )
    private val failedState = TradeProgressUiState(
        R.string.morph_failed_title,
        R.string.morph_failed_explanation,
        R.drawable.trade_progress_failed,
        false,
        0
    )

    @Before
    fun setUp() {
        subject = TradeInProgressPresenter(shapeShiftDataManager)
        subject.initView(view)

        whenever(view.depositAddress).thenReturn(depositAddress)
    }

    @Test
    fun `onViewReady error`() {
        // Arrange
        whenever(shapeShiftDataManager.getTradeStatus(depositAddress))
            .thenReturn(Observable.error(Throwable()))
        // Act
        subject.onViewReady()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        // Assert
        verify(shapeShiftDataManager).getTradeStatus(depositAddress)
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).depositAddress
        verify(view).updateUi(noDepositState)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady no deposit`() {
        // Arrange
        val response = TradeStatusResponse().apply { setStatus(Trade.STATUS.NO_DEPOSITS.name) }
        whenever(shapeShiftDataManager.getTradeStatus(depositAddress))
            .thenReturn(Observable.just(response))
        // Act
        subject.onViewReady()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        // Assert
        verify(shapeShiftDataManager).getTradeStatus(depositAddress)
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).depositAddress
        verify(view, times(2)).updateUi(noDepositState)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady received`() {
        // Arrange
        val response = TradeStatusResponse().apply { setStatus(Trade.STATUS.RECEIVED.name) }
        whenever(shapeShiftDataManager.getTradeStatus(depositAddress))
            .thenReturn(Observable.just(response))
        // Act
        subject.onViewReady()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        // Assert
        verify(shapeShiftDataManager).getTradeStatus(depositAddress)
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).depositAddress
        verify(view).updateUi(noDepositState)
        verify(view).updateUi(receivedState)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady complete and saved to metadata`() {
        // Arrange
        val hashOut = "TRANSACTION"
        val response = TradeStatusResponse().apply {
            setStatus(Trade.STATUS.COMPLETE.name)
            this.address = depositAddress
            this.transaction = hashOut
        }
        whenever(shapeShiftDataManager.getTradeStatus(depositAddress))
            .thenReturn(Observable.just(response))
        val trade = Trade()
        whenever(shapeShiftDataManager.findTrade(depositAddress)).thenReturn(Single.just(trade))
        whenever(shapeShiftDataManager.updateTrade(trade)).thenReturn(Completable.complete())
        // Act
        subject.onViewReady()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        // Assert
        verify(shapeShiftDataManager).getTradeStatus(depositAddress)
        verify(shapeShiftDataManager).findTrade(depositAddress)
        verify(shapeShiftDataManager).updateTrade(trade)
        trade.status `should equal` Trade.STATUS.COMPLETE
        trade.hashOut `should equal` hashOut
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).depositAddress
        verify(view).updateUi(noDepositState)
        verify(view).updateUi(completeState)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady failed and saved to metadata`() {
        // Arrange
        val hashOut = "TRANSACTION"
        val response = TradeStatusResponse().apply {
            setStatus(Trade.STATUS.FAILED.name)
            this.address = depositAddress
            this.transaction = hashOut
        }
        whenever(shapeShiftDataManager.getTradeStatus(depositAddress))
            .thenReturn(Observable.just(response))
        val trade = Trade()
        whenever(shapeShiftDataManager.findTrade(depositAddress)).thenReturn(Single.just(trade))
        whenever(shapeShiftDataManager.updateTrade(trade)).thenReturn(Completable.complete())
        // Act
        subject.onViewReady()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        // Assert
        verify(shapeShiftDataManager).getTradeStatus(depositAddress)
        verify(shapeShiftDataManager).findTrade(depositAddress)
        verify(shapeShiftDataManager).updateTrade(trade)
        trade.status `should equal` Trade.STATUS.FAILED
        trade.hashOut `should equal` hashOut
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).depositAddress
        verify(view).updateUi(noDepositState)
        verify(view).updateUi(failedState)
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `onViewReady resolved and saved to metadata`() {
        // Arrange
        val hashOut = "TRANSACTION"
        val response = TradeStatusResponse().apply {
            setStatus(Trade.STATUS.RESOLVED.name)
            this.address = depositAddress
            this.transaction = hashOut
        }
        whenever(shapeShiftDataManager.getTradeStatus(depositAddress))
            .thenReturn(Observable.just(response))
        val trade = Trade()
        whenever(shapeShiftDataManager.findTrade(depositAddress)).thenReturn(Single.just(trade))
        whenever(shapeShiftDataManager.updateTrade(trade)).thenReturn(Completable.complete())
        // Act
        subject.onViewReady()
        testScheduler.advanceTimeBy(10, TimeUnit.SECONDS)
        // Assert
        verify(shapeShiftDataManager).getTradeStatus(depositAddress)
        verify(shapeShiftDataManager).findTrade(depositAddress)
        verify(shapeShiftDataManager).updateTrade(trade)
        trade.status `should equal` Trade.STATUS.RESOLVED
        trade.hashOut `should equal` hashOut
        verifyNoMoreInteractions(shapeShiftDataManager)
        verify(view).depositAddress
        verify(view).updateUi(noDepositState)
        verify(view).updateUi(failedState)
        verifyNoMoreInteractions(view)
    }
}
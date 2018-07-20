package piuk.blockchain.android.ui.buysell.details.trade

import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.testutils.rxInit
import piuk.blockchain.android.ui.buysell.details.models.BuySellDetailsModel
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.CardDetails
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.androidbuysell.models.coinify.Medium
import piuk.blockchain.androidbuysell.models.coinify.Transfer
import piuk.blockchain.androidbuysell.models.coinify.TransferState
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

class CoinifyTransactionDetailPresenterTest {

    private lateinit var subject: CoinifyTransactionDetailPresenter
    private val exchangeService: ExchangeService = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val view: CoinifyTransactionDetailView = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = CoinifyTransactionDetailPresenter(coinifyDataManager, exchangeService)
        subject.initView(view)
    }

    @Test
    fun `finish card payment completes successfully`() {
        // Arrange
        val tradeId = 12345
        val orderDetails = BuySellDetailsModel(
            isSell = true,
            isAwaitingCardPayment = true,
            pageTitle = "",
            headlineAmount = "",
            detailAmount = "",
            date = "",
            tradeIdDisplay = "",
            tradeId = tradeId,
            currencyReceivedTitle = "",
            amountSent = "",
            paymentFee = "",
            totalCost = ""
        )
        whenever(view.orderDetails).thenReturn(orderDetails)
        val token = "TOKEN"
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))
        val trade: CoinifyTrade = mock()
        val transferIn = Transfer(
            details = CardDetails("", "", "", null, null, ""),
            id = 67890,
            currency = "GBP",
            mediumReceiveAccountId = 12345,
            medium = Medium.Card,
            receiveAmount = 100.00,
            sendAmount = 0.001,
            state = TransferState.Waiting
        )
        whenever(trade.transferIn).thenReturn(transferIn)
        whenever(trade.inCurrency).thenReturn("GBP")
        whenever(coinifyDataManager.getTradeStatus(token, tradeId)).thenReturn(Single.just(trade))
        // Act
        subject.onFinishCardPayment()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verifyNoMoreInteractions(exchangeService)
        verify(coinifyDataManager).getTradeStatus(token, tradeId)
        verifyNoMoreInteractions(coinifyDataManager)
        verify(view).orderDetails
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).launchCardPayment(any(), any(), any(), any())
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `finish card payment fails`() {
        // Arrange
        val tradeId = 12345
        val orderDetails = BuySellDetailsModel(
            isSell = true,
            isAwaitingCardPayment = true,
            pageTitle = "",
            headlineAmount = "",
            detailAmount = "",
            date = "",
            tradeIdDisplay = "",
            tradeId = tradeId,
            currencyReceivedTitle = "",
            amountSent = "",
            paymentFee = "",
            totalCost = ""
        )
        whenever(view.orderDetails).thenReturn(orderDetails)
        val token = "TOKEN"
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))
        whenever(coinifyDataManager.getTradeStatus(token, tradeId))
            .thenReturn(Single.error { Throwable() })
        // Act
        subject.onFinishCardPayment()
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verifyNoMoreInteractions(exchangeService)
        verify(coinifyDataManager).getTradeStatus(token, tradeId)
        verifyNoMoreInteractions(coinifyDataManager)
        verify(view).orderDetails
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `cancel trade completes successfully`() {
        // Arrange
        val token = "TOKEN"
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))
        val tradeId = 12345
        val trade: CoinifyTrade = mock()
        whenever(coinifyDataManager.cancelTrade(token, tradeId)).thenReturn(Single.just(trade))
        // Act
        subject.cancelTrade(tradeId)
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verifyNoMoreInteractions(exchangeService)
        verify(coinifyDataManager).cancelTrade(token, tradeId)
        verifyNoMoreInteractions(coinifyDataManager)
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showToast(any(), eq(ToastCustom.TYPE_OK))
        verify(view).finishPage()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `cancel trade fails`() {
        // Arrange
        val token = "TOKEN"
        val exchangeData: ExchangeData = mock()
        val coinifyData: CoinifyData = mock()
        whenever(exchangeData.coinify).thenReturn(coinifyData)
        whenever(coinifyData.token).thenReturn(token)
        whenever(exchangeService.getExchangeMetaData()).thenReturn(Observable.just(exchangeData))
        val tradeId = 12345
        whenever(coinifyDataManager.cancelTrade(token, tradeId))
            .thenReturn(Single.error { Throwable() })
        // Act
        subject.cancelTrade(tradeId)
        // Assert
        verify(exchangeService).getExchangeMetaData()
        verifyNoMoreInteractions(exchangeService)
        verify(coinifyDataManager).cancelTrade(token, tradeId)
        verifyNoMoreInteractions(coinifyDataManager)
        verify(view).showProgressDialog()
        verify(view).dismissProgressDialog()
        verify(view).showToast(any(), eq(ToastCustom.TYPE_ERROR))
        verifyNoMoreInteractions(view)
    }
}
package piuk.blockchain.android.ui.buysell.details.awaitingtransfer

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
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom

class CoinifyAwaitingBankTransferPresenterTest {

    private lateinit var subject: CoinifyAwaitingBankTransferPresenter
    private val exchangeService: ExchangeService = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val view: CoinifyAwaitingBankTransferView = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    @Throws(Exception::class)
    fun setUp() {
        subject = CoinifyAwaitingBankTransferPresenter(exchangeService, coinifyDataManager)
        subject.initView(view)
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
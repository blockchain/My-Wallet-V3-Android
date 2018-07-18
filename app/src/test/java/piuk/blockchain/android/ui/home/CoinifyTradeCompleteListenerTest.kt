package piuk.blockchain.android.ui.home

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Completable
import io.reactivex.Observable
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.android.testutils.rxInit
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager
import piuk.blockchain.androidbuysell.models.CoinifyData
import piuk.blockchain.androidbuysell.models.ExchangeData
import piuk.blockchain.androidbuysell.models.TradeData
import piuk.blockchain.androidbuysell.models.coinify.BlockchainDetails
import piuk.blockchain.androidbuysell.models.coinify.CoinifyTrade
import piuk.blockchain.androidbuysell.models.coinify.EventData
import piuk.blockchain.androidbuysell.models.coinify.TradeState
import piuk.blockchain.androidbuysell.models.coinify.Transfer
import piuk.blockchain.androidbuysell.services.ExchangeService
import piuk.blockchain.androidcore.data.metadata.MetadataManager

class CoinifyTradeCompleteListenerTest {

    private lateinit var subject: CoinifyTradeCompleteListener
    private val exchangeService: ExchangeService = mock()
    private val coinifyDataManager: CoinifyDataManager = mock()
    private val metadataManager: MetadataManager = mock()

    @Suppress("unused")
    @get:Rule
    val initSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = CoinifyTradeCompleteListener(
            exchangeService,
            coinifyDataManager,
            metadataManager
        )
    }

    @Test
    fun `no coinify data, should emit no values`() {
        // Arrange
        whenever(exchangeService.getExchangeMetaData())
            .thenReturn(Observable.just(ExchangeData()))
        // Act
        val testObserver = subject.getCompletedCoinifyTrades().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertNoValues()
        verify(exchangeService).getExchangeMetaData()
    }

    @Test
    fun `none stored in metadata, should emit no values`() {
        // Arrange
        val token = "TOKEN"
        val coinifyData = CoinifyData(1, token)
        val exchangeData = ExchangeData().apply { coinify = coinifyData }
        whenever(exchangeService.getExchangeMetaData())
            .thenReturn(Observable.just(exchangeData))
        whenever(coinifyDataManager.getTrades(token)).thenReturn(Observable.empty())
        // Act
        val testObserver = subject.getCompletedCoinifyTrades().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertNoValues()
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getTrades(token)
    }

    @Test
    fun `trade stored in metadata, no coinify trades, should emit no values`() {
        // Arrange
        val token = "TOKEN"
        val metadataTrades = listOf(TradeData().apply { id = 12345 })
        val coinifyData = CoinifyData(1, token).apply { trades = metadataTrades }
        val exchangeData = ExchangeData().apply { coinify = coinifyData }
        whenever(exchangeService.getExchangeMetaData())
            .thenReturn(Observable.just(exchangeData))
        whenever(coinifyDataManager.getTrades(token)).thenReturn(Observable.empty())
        // Act
        val testObserver = subject.getCompletedCoinifyTrades().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertNoValues()
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getTrades(token)
    }

    @Test
    fun `coinify trade is sell, should be filtered, should emit no values`() {
        // Arrange
        val token = "TOKEN"
        val tradeId = 12345
        val metadataTrades = listOf(TradeData().apply { id = tradeId })
        val coinifyData = CoinifyData(1, token).apply { trades = metadataTrades }
        val exchangeData = ExchangeData().apply { coinify = coinifyData }
        whenever(exchangeService.getExchangeMetaData())
            .thenReturn(Observable.just(exchangeData))
        val coinifyTrade: CoinifyTrade = mock()
        whenever(coinifyTrade.id).thenReturn(tradeId)
        whenever(coinifyTrade.isSellTransaction()).thenReturn(true)
        whenever(coinifyDataManager.getTrades(token)).thenReturn(Observable.just(coinifyTrade))
        // Act
        val testObserver = subject.getCompletedCoinifyTrades().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertNoErrors()
        testObserver.assertNoValues()
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getTrades(token)
    }

    @Test
    fun `coinify trade is buy but data is incomplete, should return error`() {
        // Arrange
        val token = "TOKEN"
        val tradeId = 12345
        val metadataTrades = listOf(TradeData().apply { id = tradeId })
        val coinifyData = CoinifyData(1, token).apply { trades = metadataTrades }
        val exchangeData = ExchangeData().apply { coinify = coinifyData }
        whenever(exchangeService.getExchangeMetaData())
            .thenReturn(Observable.just(exchangeData))
        val coinifyTrade: CoinifyTrade = mock()
        whenever(coinifyTrade.id).thenReturn(tradeId)
        whenever(coinifyTrade.state).thenReturn(TradeState.Completed)
        whenever(coinifyTrade.isSellTransaction()).thenReturn(false)
        val transferOut: Transfer = mock()
        whenever(coinifyTrade.transferOut).thenReturn(transferOut)
        val details = BlockchainDetails("", null)
        whenever(transferOut.details).thenReturn(details)
        whenever(coinifyDataManager.getTrades(token)).thenReturn(Observable.just(coinifyTrade))
        whenever(metadataManager.saveToMetadata(any(), any())).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.getCompletedCoinifyTrades().test()
        // Assert
        testObserver.assertNotComplete()
        testObserver.assertError(IllegalStateException::class.java)
        testObserver.assertNoValues()
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getTrades(token)
        verify(metadataManager).saveToMetadata(any(), any())
    }

    @Test
    fun `should return transaction hash`() {
        // Arrange
        val token = "TOKEN"
        val tradeId = 12345
        val metadataTrades = listOf(TradeData().apply { id = tradeId })
        val coinifyData = CoinifyData(1, token).apply { trades = metadataTrades }
        val exchangeData = ExchangeData().apply { coinify = coinifyData }
        whenever(exchangeService.getExchangeMetaData())
            .thenReturn(Observable.just(exchangeData))
        val coinifyTrade: CoinifyTrade = mock()
        whenever(coinifyTrade.id).thenReturn(tradeId)
        whenever(coinifyTrade.state).thenReturn(TradeState.Completed)
        whenever(coinifyTrade.isSellTransaction()).thenReturn(false)
        val transferOut: Transfer = mock()
        whenever(coinifyTrade.transferOut).thenReturn(transferOut)
        val txHash = "TX_HASH"
        val details = BlockchainDetails("", EventData(txHash, ""))
        whenever(transferOut.details).thenReturn(details)
        whenever(coinifyDataManager.getTrades(token)).thenReturn(Observable.just(coinifyTrade))
        whenever(metadataManager.saveToMetadata(any(), any())).thenReturn(Completable.complete())
        // Act
        val testObserver = subject.getCompletedCoinifyTrades().test()
        // Assert
        testObserver.assertComplete()
        testObserver.assertValue(txHash)
        testObserver.assertValueCount(1)
        verify(exchangeService).getExchangeMetaData()
        verify(coinifyDataManager).getTrades(token)
        verify(metadataManager).saveToMetadata(any(), any())
    }
}
package com.blockchain.morph.ui.homebrew.exchange.history

import com.blockchain.android.testutils.rxInit
import com.blockchain.morph.CoinPair
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeDataManager
import com.blockchain.morph.trade.MorphTradeOrder
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.testutils.gbp
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should equal`
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcoreui.utils.DateUtil
import java.math.BigDecimal
import java.util.Locale

class TradeHistoryPresenterTest {

    private lateinit var subject: TradeHistoryPresenter
    private val nabuTradeManager: MorphTradeDataManager = mock()
    private val shapeShiftTradeManager: MorphTradeDataManager = mock()
    private val dateUtil: DateUtil = mock()
    private val view: TradeHistoryView = mock()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = TradeHistoryPresenter(nabuTradeManager, shapeShiftTradeManager, dateUtil)
        subject.initView(view)

        whenever(view.locale).thenReturn(Locale.UK)
        whenever(dateUtil.formatted(any())).thenReturn("DATE")
    }

    @Test
    fun `onViewReady fails to load trades`() {
        // Arrange
        whenever(nabuTradeManager.getTrades()).thenReturn(Single.error { Throwable() })
        whenever(shapeShiftTradeManager.getTrades()).thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(view).renderUi(ExchangeUiState.Loading)
        verify(view).renderUi(ExchangeUiState.Empty)
    }

    @Test
    fun `onViewReady loads empty list`() {
        // Arrange
        whenever(nabuTradeManager.getTrades()).thenReturn(Single.just(emptyList()))
        whenever(shapeShiftTradeManager.getTrades()).thenReturn(Single.just(emptyList()))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).renderUi(ExchangeUiState.Loading)
        verify(view).renderUi(ExchangeUiState.Empty)
    }

    @Test
    fun `onViewReady loads list of data, sorted by date`() {
        // Arrange
        whenever(nabuTradeManager.getTrades())
            .thenReturn(Single.just(listOf(getMorphTrade(2, "FIRST"))))
        whenever(shapeShiftTradeManager.getTrades())
            .thenReturn(
                Single.just(
                    listOf(
                        getMorphTrade(0, "THIRD"),
                        getMorphTrade(1, "SECOND")
                    )
                )
            )
        // Act
        subject.onViewReady()
        // Assert
        argumentCaptor<ExchangeUiState>().apply {
            verify(view, times(2)).renderUi(capture())

            firstValue `should be instance of` ExchangeUiState.Loading::class.java

            secondValue `should be instance of` ExchangeUiState.Data::class.java
            (secondValue as ExchangeUiState.Data).trades[0].id `should equal` "FIRST"
            (secondValue as ExchangeUiState.Data).trades[1].id `should equal` "SECOND"
            (secondValue as ExchangeUiState.Data).trades[2].id `should equal` "THIRD"
        }
    }

    @Test
    fun `onViewReady shapeshift failure doesn't affect nabu`() {
        // Arrange
        whenever(nabuTradeManager.getTrades()).thenReturn(Single.just(listOf(getMorphTrade())))
        whenever(shapeShiftTradeManager.getTrades()).thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(view).renderUi(ExchangeUiState.Loading)
        verify(view).renderUi(any(ExchangeUiState.Data::class))
    }

    @Test
    fun `onViewReady nabu failure doesn't affect shapeshift`() {
        // Arrange
        whenever(nabuTradeManager.getTrades()).thenReturn(Single.error { Throwable() })
        whenever(shapeShiftTradeManager.getTrades()).thenReturn(Single.just(listOf(getMorphTrade())))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).renderUi(ExchangeUiState.Loading)
        verify(view).renderUi(any(ExchangeUiState.Data::class))
    }

    private fun getMorphTrade(timestamp: Long = 1234567890L, id: String = "ORDER_ID"): MorphTrade {
        return object : MorphTrade {
            override val timestamp: Long
                get() = timestamp
            override val status: MorphTrade.Status
                get() = MorphTrade.Status.COMPLETE
            override val hashOut: String?
                get() = "HASH_OUT"
            override val quote: MorphTradeOrder
                get() = object : MorphTradeOrder {
                    override val pair: CoinPair
                        get() = CoinPair.ETH_TO_BCH
                    override val orderId: String
                        get() = id
                    override val depositAmount: CryptoValue
                        get() = 123.ether()
                    override val withdrawalAmount: CryptoValue
                        get() = 321.bitcoin()
                    override val quotedRate: BigDecimal
                        get() = 10.0.toBigDecimal()
                    override val minerFee: CryptoValue
                        get() = 0.1.bitcoin()
                    override val fiatValue: FiatValue
                        get() = 10.gbp()
                }

            override fun enoughInfoForDisplay(): Boolean = true
        }
    }
}
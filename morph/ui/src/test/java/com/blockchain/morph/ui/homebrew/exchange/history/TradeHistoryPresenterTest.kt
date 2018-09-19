package com.blockchain.morph.ui.homebrew.exchange.history

import com.blockchain.android.testutils.rxInit
import com.blockchain.morph.CoinPair
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeDataManager
import com.blockchain.morph.trade.MorphTradeOrder
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.testutils.gbp
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Single
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
    private val dataManager: MorphTradeDataManager = mock()
    private val dateUtil: DateUtil = mock()
    private val view: TradeHistoryView = mock()

    @get:Rule
    val rxSchedulers = rxInit {
        mainTrampoline()
        ioTrampoline()
    }

    @Before
    fun setUp() {
        subject = TradeHistoryPresenter(dataManager, dateUtil)
        subject.initView(view)

        whenever(view.locale).thenReturn(Locale.UK)
        whenever(dateUtil.formatted(any())).thenReturn("DATE")
    }

    @Test
    fun `onViewReady fails to load trades`() {
        // Arrange
        whenever(dataManager.getTrades()).thenReturn(Single.error { Throwable() })
        // Act
        subject.onViewReady()
        // Assert
        verify(view).renderUi(ExchangeUiState.Loading)
        verify(view).renderUi(ExchangeUiState.Error)
    }

    @Test
    fun `onViewReady loads empty list`() {
        // Arrange
        whenever(dataManager.getTrades()).thenReturn(Single.just(emptyList()))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).renderUi(ExchangeUiState.Loading)
        verify(view).renderUi(ExchangeUiState.Empty)
    }

    @Test
    fun `onViewReady loads list of data`() {
        // Arrange
        whenever(dataManager.getTrades()).thenReturn(Single.just(listOf(morphTrade)))
        // Act
        subject.onViewReady()
        // Assert
        verify(view).renderUi(ExchangeUiState.Loading)
        verify(view).renderUi(any(ExchangeUiState.Data::class))
    }

    private val morphTrade = object : MorphTrade {
        override val timestamp: Long
            get() = 1234567890L
        override val status: MorphTrade.Status
            get() = MorphTrade.Status.COMPLETE
        override val hashOut: String?
            get() = "HASH_OUT"
        override val quote: MorphTradeOrder
            get() = object : MorphTradeOrder {
                override val pair: CoinPair
                    get() = CoinPair.ETH_TO_BCH
                override val orderId: String
                    get() = "ORDER_ID"
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
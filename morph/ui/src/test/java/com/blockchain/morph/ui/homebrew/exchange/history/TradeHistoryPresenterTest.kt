package com.blockchain.morph.ui.homebrew.exchange.history

import com.blockchain.android.testutils.rxInit
import com.blockchain.morph.trade.MorphTradeDataHistoryList
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.Single
import org.amshove.kluent.any
import org.amshove.kluent.mock
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import piuk.blockchain.androidcoreui.utils.DateUtil
import java.util.Locale

class TradeHistoryPresenterTest {

    private lateinit var subject: TradeHistoryPresenter
    private val dataManager: MorphTradeDataHistoryList = mock()
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
}
package com.blockchain.morph.trade

import com.blockchain.morph.CoinPair
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.testutils.gbp
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.whenever
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Single
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

class MergingMorphTradeDataManagerTest {

    private lateinit var subject: MorphTradeDataHistoryList
    private val firstTradeManager: MorphTradeDataHistoryList = mock()
    private val secondTradeDataManager: MorphTradeDataHistoryList = mock()

    @Before
    fun setUp() {
        subject = MergingMorphTradeDataManager(firstTradeManager, secondTradeDataManager)
    }

    @Test
    fun `get trades combines two lists and sorts by date`() {
        // Arrange
        whenever(firstTradeManager.getTrades())
            .thenReturn(Single.just(listOf(getMorphTrade(2))))
        whenever(secondTradeDataManager.getTrades())
            .thenReturn(
                Single.just(
                    listOf(
                        getMorphTrade(0),
                        getMorphTrade(1)
                    )
                )
            )
        // Act
        val testObserver = subject.getTrades().test()
        // Assert
        testObserver.assertComplete()
        testObserver.values().first().apply {
            get(0).timestamp `should equal` 2L
            get(1).timestamp `should equal` 1L
            get(2).timestamp `should equal` 0L
        }
    }

    @Test
    fun `get trades, first data manager failing doesn't affect second`() {
        // Arrange
        val trade = getMorphTrade()
        whenever(firstTradeManager.getTrades()).thenReturn(Single.error { Throwable() })
        whenever(secondTradeDataManager.getTrades()).thenReturn(Single.just(listOf(trade)))
        // Act
        val testObserver = subject.getTrades().test()
        // Assert
        testObserver.assertValue(listOf(trade))
    }

    @Test
    fun `get trades, second data manager failing doesn't affect first`() {
        // Arrange
        val trade = getMorphTrade()
        whenever(firstTradeManager.getTrades()).thenReturn(Single.just(listOf(trade)))
        whenever(secondTradeDataManager.getTrades()).thenReturn(Single.error { Throwable() })
        // Act
        val testObserver = subject.getTrades().test()
        // Assert
        testObserver.assertValue(listOf(trade))
    }

    @Test
    fun `get trades, both data managers failing returns empty list`() {
        // Arrange
        whenever(firstTradeManager.getTrades()).thenReturn(Single.error { Throwable() })
        whenever(secondTradeDataManager.getTrades()).thenReturn(Single.error { Throwable() })
        // Act
        val testObserver = subject.getTrades().test()
        // Assert
        testObserver.assertValue(listOf())
    }

    private fun getMorphTrade(timestamp: Long = 1234567890L): MorphTrade {
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
                        get() = "ID"
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
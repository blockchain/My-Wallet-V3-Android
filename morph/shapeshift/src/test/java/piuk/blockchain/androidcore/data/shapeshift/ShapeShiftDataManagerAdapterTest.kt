package piuk.blockchain.androidcore.data.shapeshift

import com.blockchain.morph.trade.MorphTrade
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should equal`
import org.junit.Test

class ShapeShiftDataManagerAdapterTest {

    @Test
    fun `findTrade returns mapped trade from underlying data manager`() {
        val shapeShiftDataManager = mock<ShapeShiftDataManager> {
            on { findTrade("address") } `it returns` Single.just(Trade().apply { hashOut = "X" })
        }
        ShapeShiftDataManagerAdapter(shapeShiftDataManager)
            .findTrade("address")
            .test()
            .values()
            .single().hashOut `should equal` "X"
    }

    @Test
    fun `getTrades returns mapped trade list from underlying data manager`() {
        val shapeShiftDataManager = mock<ShapeShiftDataManager> {
            on { getTradesList() } `it returns` Observable.just(listOf(Trade().apply { hashOut = "X" }))
        }
        ShapeShiftDataManagerAdapter(shapeShiftDataManager)
            .getTrades()
            .test()
            .values()
            .single()
            .first()
            .hashOut `should equal` "X"
    }

    @Test
    fun `getTradeStatus returns mapped trade status response from underlying data manager`() {
        val shapeShiftDataManager = mock<ShapeShiftDataManager> {
            on { getTradeStatus("address") } `it returns` Observable.just(TradeStatusResponse()
                .apply {
                    setStatus(Trade.STATUS.FAILED.name)
                }
            )
        }
        ShapeShiftDataManagerAdapter(shapeShiftDataManager)
            .getTradeStatus("address")
            .test()
            .values()
            .single()
            .status `should equal` MorphTrade.Status.FAILED
    }

    @Test
    fun `updateTrade doesn't update the trade in underlying data manager if not found`() {
        val shapeShiftDataManager = mock<ShapeShiftDataManager>()
        ShapeShiftDataManagerAdapter(shapeShiftDataManager)
            .updateTrade("order", MorphTrade.Status.COMPLETE, "Hash")
            .test()
            .assertErrorMessage("Trade not found")
        verify(shapeShiftDataManager, never()).updateTrade(any())
    }

    @Test
    fun `updateTrade updates the trade in underlying data manager if found`() {
        val shapeShiftDataManager = mock<ShapeShiftDataManager> {
            on { findTradeByOrderId("order") } `it returns` Trade()
            on { updateTrade(any()) } `it returns` Completable.complete()
        }
        ShapeShiftDataManagerAdapter(shapeShiftDataManager)
            .updateTrade("order", MorphTrade.Status.COMPLETE, "Hash")
            .test()
            .assertNoErrors()
        argumentCaptor<Trade>().apply {
            verify(shapeShiftDataManager).updateTrade(capture())
            firstValue.hashOut `should equal` "Hash"
            firstValue.status `should equal` Trade.STATUS.COMPLETE
        }
    }

    @Test
    fun `updateTrade updates the trade in underlying data manager if found - all status`() {
        MorphTrade.Status.values().filter {
            it != MorphTrade.Status.UNKNOWN &&
                it != MorphTrade.Status.REFUNDED &&
                it != MorphTrade.Status.REFUND_IN_PROGRESS &&
                it != MorphTrade.Status.EXPIRED &&
                it != MorphTrade.Status.IN_PROGRESS
        }.forEach { morphStatus ->
                val shapeShiftDataManager = mock<ShapeShiftDataManager> {
                    on { findTradeByOrderId("order") } `it returns` Trade()
                    on { updateTrade(any()) } `it returns` Completable.complete()
                }
                ShapeShiftDataManagerAdapter(shapeShiftDataManager)
                    .updateTrade("order", morphStatus, "Hash")
                    .test()
                    .assertNoErrors()
                argumentCaptor<Trade>().apply {
                    verify(shapeShiftDataManager).updateTrade(capture())
                    firstValue.status?.name `should equal` morphStatus.name
                }
            }
    }

    @Test
    fun `updateTrade doesn't update the trade in underlying data manager if found but unknown state`() {
        val shapeShiftDataManager = mock<ShapeShiftDataManager> {
            on { findTradeByOrderId("order") } `it returns` Trade()
        }
        ShapeShiftDataManagerAdapter(shapeShiftDataManager)
            .updateTrade("order", MorphTrade.Status.UNKNOWN, "Hash")
            .test()
            .assertErrorMessage("Unknown Status")
        verify(shapeShiftDataManager, never()).updateTrade(any())
    }
}

package piuk.blockchain.androidcore.data.shapeshift.dataadapters

import com.blockchain.morph.trade.MorphTrade
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import info.blockchain.wallet.shapeshift.data.Trade
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import org.amshove.kluent.`should equal`
import org.junit.Test

class TradeStatusResponseAdapterTest {

    @Test
    fun `incoming value`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse().apply {
                incomingType = "bch"
                incomingCoin = 1.23.toBigDecimal()
            }
        ).incomingValue `should equal` 1.23.bitcoinCash()
    }

    @Test
    fun `outgoing value`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse().apply {
                outgoingType = "btc"
                outgoingCoin = 9.876.toBigDecimal()
            }
        ).outgoingValue `should equal` 9.876.bitcoin()
    }

    @Test
    fun `Bitcoin is the default incoming type`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse().apply {
                incomingCoin = 5.68.toBigDecimal()
            }
        ).incomingValue `should equal` 5.68.bitcoin()
    }

    @Test
    fun `Ether is the default outgoing type`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse().apply {
                outgoingCoin = 5.2.toBigDecimal()
            }
        ).outgoingValue `should equal` 5.2.ether()
    }

    @Test
    fun `Zero is the default incoming value`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse()
        ).incomingValue `should equal` 0.0.bitcoin()
    }

    @Test
    fun `Zero is the default outgoing value`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse()
        ).outgoingValue `should equal` 0.0.ether()
    }

    @Test
    fun `address passed through`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse().apply {
                address = "Address"
            }
        ).address `should equal` "Address"
    }

    @Test
    fun `default address is empty string`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse()
        ).address `should equal` ""
    }

    @Test
    fun `transaction passed through`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse().apply {
                transaction = "Transaction 1"
            }
        ).transaction `should equal` "Transaction 1"
    }

    @Test
    fun `default transaction is empty string`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse()
        ).transaction `should equal` ""
    }

    @Test
    fun `all statuses are mapped`() {
        Trade.STATUS.values().forEach {
            TradeStatusResponseAdapter(
                TradeStatusResponse().apply {
                    setStatus(it.name)
                }
            ).status.name `should equal` it.name
        }
    }

    @Test
    fun `default status is Unknown`() {
        TradeStatusResponseAdapter(
            TradeStatusResponse()
        ).status `should equal` MorphTrade.Status.UNKNOWN
    }
}

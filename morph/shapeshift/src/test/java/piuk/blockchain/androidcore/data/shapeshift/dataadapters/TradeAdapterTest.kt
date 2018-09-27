package piuk.blockchain.androidcore.data.shapeshift.dataadapters

import com.blockchain.morph.CoinPair
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import info.blockchain.wallet.shapeshift.data.Quote
import info.blockchain.wallet.shapeshift.data.Trade
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test
import java.math.BigDecimal

class TradeAdapterTest {

    @Test
    fun `quote timestamp should be in seconds not millis`() {
        TradeAdapter(Trade().apply {
            timestamp = 1234567890L
        }).timestamp `should equal` 1234567890L / 1000
    }

    @Test
    fun `quote hash out`() {
        TradeAdapter(Trade().apply {
            hashOut = "Hash out"
        }).hashOut `should equal` "Hash out"
    }

    @Test
    fun `default hash out`() {
        TradeAdapter(Trade())
            .hashOut `should equal` null
    }

    @Test
    fun `quote order id`() {
        TradeAdapter(Trade().apply {
            quote = Quote().apply {
                orderId = "Order 1"
            }
        }).quote.orderId `should equal` "Order 1"
    }

    @Test
    fun `quote order id default is empty string`() {
        TradeAdapter(Trade())
            .quote.orderId `should equal` ""
    }

    @Test
    fun `quote pair default`() {
        TradeAdapter(Trade())
            .quote.pair `should be` CoinPair.BTC_TO_ETH
    }

    @Test
    fun `quote pair`() {
        TradeAdapter(Trade().apply {
            quote = Quote().apply {
                pair = "eth_bch"
            }
        }).quote.pair `should be` CoinPair.ETH_TO_BCH
    }

    @Test
    fun `quote withdrawal amount`() {
        TradeAdapter(Trade().apply {
            quote = Quote().apply {
                withdrawalAmount = 4.56.toBigDecimal()
                pair = "eth_bch"
            }
        }).quote.withdrawalAmount `should equal` 4.56.bitcoinCash()
    }

    @Test
    fun `quote withdrawal amount default is 0 Ether`() {
        TradeAdapter(Trade())
            .quote.withdrawalAmount `should equal` 0.0.ether()
    }

    @Test
    fun `quote deposit amount`() {
        TradeAdapter(Trade().apply {
            quote = Quote().apply {
                depositAmount = 1.23.toBigDecimal()
                pair = "eth_bch"
            }
        }).quote.depositAmount `should equal` 1.23.ether()
    }

    @Test
    fun `quote deposit amount default is 0 Bitcoin`() {
        TradeAdapter(Trade())
            .quote.depositAmount `should equal` 0.0.bitcoin()
    }

    @Test
    fun `default status is unknown`() {
        TradeAdapter(Trade())
            .status `should be` MorphTrade.Status.UNKNOWN
    }

    @Test
    fun `all statuses are mapped`() {
        Trade.STATUS.values().forEach {
            TradeAdapter(
                Trade().apply {
                    status = it
                }
            ).status.name `should equal` it.name
        }
    }

    @Test
    fun `enoughInfoForDisplay default`() {
        TradeAdapter(Trade())
            .enoughInfoForDisplay() `should be` false
    }

    @Test
    fun `enoughInfoForDisplay with deposit`() {
        TradeAdapter(
            Trade().apply {
                quote = Quote().apply {
                    depositAmount = 1.23.toBigDecimal()
                }
            }
        ).enoughInfoForDisplay() `should be` false
    }

    @Test
    fun `enoughInfoForDisplay with pair`() {
        TradeAdapter(
            Trade().apply {
                quote = Quote().apply {
                    pair = "eth_btc"
                }
            }
        ).enoughInfoForDisplay() `should be` false
    }

    @Test
    fun `enoughInfoForDisplay with both pair`() {
        TradeAdapter(
            Trade().apply {
                quote = Quote().apply {
                    depositAmount = 1.23.toBigDecimal()
                    pair = "eth_btc"
                }
            }
        ).enoughInfoForDisplay() `should be` true
    }

    @Test
    fun `quote mining fee default`() {
        TradeAdapter(Trade())
            .quote.minerFee `should equal` 0.0.ether()
    }

    @Test
    fun `quote mining fee uses "to" currency`() {
        TradeAdapter(
            Trade().apply {
                quote = Quote().apply {
                    minerFee = 0.0001.toBigDecimal()
                    pair = "eth_btc"
                }
            }
        ).quote.minerFee `should equal` 0.0001.bitcoin()
    }

    @Test
    fun `quote rate default`() {
        TradeAdapter(Trade())
            .quote.quotedRate `should equal` BigDecimal.ZERO
    }

    @Test
    fun `quote rate`() {
        TradeAdapter(
            Trade().apply {
                quote = Quote().apply {
                    quotedRate = 17.56.toBigDecimal()
                }
            }
        ).quote.quotedRate `should equal` 17.56.toBigDecimal()
    }

    @Test
    fun `fiat value is null, as not provided by API`() {
        TradeAdapter(
            Trade().apply {
                quote = Quote()
            }
        ).quote.fiatValue `should equal` null
    }
}

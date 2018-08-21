package piuk.blockchain.androidcore.data.shapeshift

import com.blockchain.morph.CoinPair
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeOrder
import info.blockchain.wallet.shapeshift.data.Trade
import java.math.BigDecimal

class TradeAdapter(private val trade: Trade) : MorphTrade {

    override val hashOut: String?
        get() = trade.hashOut

    override val status: MorphTrade.Status
        get() = trade.status.map()

    override val quote: MorphTradeOrder
        get() = object : MorphTradeOrder {
            override val pair: CoinPair
                get() = CoinPair.fromPairCodeOrNull(trade.quote.pair)
                    ?: CoinPair.BTC_TO_ETH

            override val orderId: String
                get() = trade.quote.orderId

            override val depositAmount: BigDecimal?
                get() = trade.quote.depositAmount

            override val withdrawalAmount: BigDecimal?
                get() = trade.quote.withdrawalAmount

            override val quotedRate: BigDecimal?
                get() = trade.quote.quotedRate

            override val minerFee: BigDecimal?
                get() = trade.quote.minerFee
        }

    override fun requiresMoreInfoForUi(): Boolean {
        return trade.quote.depositAmount == null ||
            CoinPair.fromPairCodeOrNull(trade.quote.pair) == null
    }
}

internal fun Trade.STATUS.map(): MorphTrade.Status =
    when (this) {
        Trade.STATUS.COMPLETE -> MorphTrade.Status.COMPLETE
        Trade.STATUS.FAILED -> MorphTrade.Status.FAILED
        Trade.STATUS.NO_DEPOSITS -> MorphTrade.Status.NO_DEPOSITS
        Trade.STATUS.RECEIVED -> MorphTrade.Status.RECEIVED
        Trade.STATUS.RESOLVED -> MorphTrade.Status.RESOLVED
    }

internal fun MorphTrade.Status.map(): Trade.STATUS =
    when (this) {
        MorphTrade.Status.COMPLETE -> Trade.STATUS.COMPLETE
        MorphTrade.Status.FAILED -> Trade.STATUS.FAILED
        MorphTrade.Status.NO_DEPOSITS -> Trade.STATUS.NO_DEPOSITS
        MorphTrade.Status.RECEIVED -> Trade.STATUS.RECEIVED
        MorphTrade.Status.RESOLVED -> Trade.STATUS.RESOLVED
    }

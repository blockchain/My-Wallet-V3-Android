package piuk.blockchain.androidcore.data.shapeshift.dataadapters

import com.blockchain.morph.CoinPair
import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeOrder
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.wallet.shapeshift.data.Trade
import java.math.BigDecimal

internal class TradeAdapter(private val trade: Trade) : MorphTrade {

    override val timestamp: Long
        get() = trade.timestamp

    override val hashOut: String?
        get() = trade.hashOut

    override val status: MorphTrade.Status
        get() = trade.status.map()

    override val quote: MorphTradeOrder
        get() = object : MorphTradeOrder {
            override val pair: CoinPair
                get() = CoinPair.fromPairCodeOrNull(trade.quote?.pair)
                    ?: CoinPair.BTC_TO_ETH

            override val orderId: String
                get() = trade.quote?.orderId ?: ""

            override val depositAmount: CryptoValue
                get() = CryptoValue.fromMajor(pair.to, trade.quote?.depositAmount ?: BigDecimal.ZERO)

            override val withdrawalAmount: CryptoValue
                get() = CryptoValue.fromMajor(pair.from, trade.quote?.withdrawalAmount ?: BigDecimal.ZERO)

            override val quotedRate: BigDecimal
                get() = trade.quote?.quotedRate ?: BigDecimal.ZERO

            override val minerFee: CryptoValue
                get() = CryptoValue.fromMajor(pair.to, trade.quote?.minerFee ?: BigDecimal.ZERO)

            override val fiatValue: FiatValue?
                get() = null
        }

    override fun enoughInfoForDisplay() =
        trade.quote?.depositAmount != null &&
            CoinPair.fromPairCodeOrNull(trade.quote?.pair) != null
}

internal fun Trade.STATUS?.map(): MorphTrade.Status =
    when (this) {
        null -> MorphTrade.Status.UNKNOWN
        Trade.STATUS.COMPLETE -> MorphTrade.Status.COMPLETE
        Trade.STATUS.FAILED -> MorphTrade.Status.FAILED
        Trade.STATUS.NO_DEPOSITS -> MorphTrade.Status.NO_DEPOSITS
        Trade.STATUS.RECEIVED -> MorphTrade.Status.RECEIVED
        Trade.STATUS.RESOLVED -> MorphTrade.Status.RESOLVED
    }

internal fun MorphTrade.Status.map(): Trade.STATUS? =
    when (this) {
        MorphTrade.Status.COMPLETE -> Trade.STATUS.COMPLETE
        MorphTrade.Status.FAILED -> Trade.STATUS.FAILED
        MorphTrade.Status.NO_DEPOSITS -> Trade.STATUS.NO_DEPOSITS
        MorphTrade.Status.RECEIVED -> Trade.STATUS.RECEIVED
        MorphTrade.Status.RESOLVED -> Trade.STATUS.RESOLVED
        MorphTrade.Status.UNKNOWN,
        MorphTrade.Status.REFUNDED,
        MorphTrade.Status.REFUND_IN_PROGRESS,
        MorphTrade.Status.EXPIRED,
        MorphTrade.Status.IN_PROGRESS -> null
    }

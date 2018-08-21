package piuk.blockchain.androidcore.data.shapeshift

import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import java.math.BigDecimal

class TradeStatusResponseAdapter(private val tradeStatusResponse: TradeStatusResponse) :
    MorphTradeStatus {

    override val incomingType: CryptoCurrency
        get() = CryptoCurrency.fromSymbol(tradeStatusResponse.incomingType ?: "btc")!!

    override val outgoingType: CryptoCurrency
        get() = CryptoCurrency.fromSymbol(tradeStatusResponse.outgoingType ?: "eth")!!

    override val incomingCoin: BigDecimal?
        get() = tradeStatusResponse.incomingCoin

    override val outgoingCoin: BigDecimal?
        get() = tradeStatusResponse.outgoingCoin

    override val status: MorphTrade.Status
        get() = tradeStatusResponse.status.map()

    override val address: String
        get() = tradeStatusResponse.address

    override val transaction: String
        get() = tradeStatusResponse.transaction
}
package piuk.blockchain.androidcore.data.shapeshift.dataadapters

import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeStatus
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.shapeshift.data.TradeStatusResponse
import java.math.BigDecimal

internal class TradeStatusResponseAdapter(private val tradeStatusResponse: TradeStatusResponse) :
    MorphTradeStatus {

    override val incomingValue: CryptoValue
        get() = CryptoValue.fromMajor(incomingType, tradeStatusResponse.incomingCoin ?: BigDecimal.ZERO)

    override val outgoingValue: CryptoValue
        get() = CryptoValue.fromMajor(outgoingType, tradeStatusResponse.outgoingCoin ?: BigDecimal.ZERO)

    private val incomingType: CryptoCurrency
        get() = CryptoCurrency.fromSymbol(tradeStatusResponse.incomingType) ?: CryptoCurrency.BTC

    private val outgoingType: CryptoCurrency
        get() = CryptoCurrency.fromSymbol(tradeStatusResponse.outgoingType) ?: CryptoCurrency.ETHER

    override val status: MorphTrade.Status
        get() = tradeStatusResponse.status.map()

    override val address: String
        get() = tradeStatusResponse.address ?: ""

    override val transaction: String
        get() = tradeStatusResponse.transaction ?: ""
}

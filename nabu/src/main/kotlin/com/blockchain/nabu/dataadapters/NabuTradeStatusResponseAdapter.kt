package com.blockchain.nabu.dataadapters

import com.blockchain.morph.trade.MorphTrade
import com.blockchain.morph.trade.MorphTradeStatus
import com.blockchain.nabu.api.NabuTransaction
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue

internal class NabuTradeStatusResponseAdapter(private val tradeStatusResponse: NabuTransaction) :
    MorphTradeStatus {

    override val incomingValue: CryptoValue
        get() = tradeStatusResponse.withdrawal

    override val outgoingValue: CryptoValue
        get() = tradeStatusResponse.deposit

    private val incomingType: CryptoCurrency
        get() = tradeStatusResponse.withdrawal.currency

    private val outgoingType: CryptoCurrency
        get() = tradeStatusResponse.deposit.currency

    override val status: MorphTrade.Status
        get() = tradeStatusResponse.state.map()

    override val address: String
        get() = tradeStatusResponse.depositAddress

    override val transaction: String
        get() = tradeStatusResponse.hashOut ?: ""
}

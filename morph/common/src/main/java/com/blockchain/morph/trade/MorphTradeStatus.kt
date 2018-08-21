package com.blockchain.morph.trade

import info.blockchain.balance.CryptoCurrency
import java.math.BigDecimal

interface MorphTradeStatus {

    val incomingType: CryptoCurrency
    val outgoingType: CryptoCurrency

    val incomingCoin: BigDecimal?
    val outgoingCoin: BigDecimal?

    val address: String
    val transaction: String

    val status: MorphTrade.Status
}

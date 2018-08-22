package com.blockchain.morph.trade

import com.blockchain.morph.CoinPair
import info.blockchain.balance.CryptoValue
import java.math.BigDecimal

interface MorphTradeOrder {

    val pair: CoinPair

    val orderId: String

    val depositAmount: CryptoValue
    val withdrawalAmount: CryptoValue
    val quotedRate: BigDecimal
    val minerFee: CryptoValue
}

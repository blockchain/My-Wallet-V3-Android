package com.blockchain.morph.trade

import com.blockchain.morph.CoinPair
import java.math.BigDecimal

interface MorphTradeOrder {

    val pair: CoinPair

    val orderId: String

    val depositAmount: BigDecimal?
    val withdrawalAmount: BigDecimal?
    val quotedRate: BigDecimal?
    val minerFee: BigDecimal?
}

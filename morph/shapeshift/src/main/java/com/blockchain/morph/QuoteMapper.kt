package com.blockchain.morph

import info.blockchain.balance.CryptoValue
import java.math.BigDecimal
import com.blockchain.morph.quote.ExchangeQuote.Success as MorphQuote
import info.blockchain.wallet.shapeshift.data.Quote as DataQuote

internal fun DataQuote.map(): MorphQuote {
    val pair = CoinPair.fromPairCode(this.pair)
    return MorphQuote(
        pair = pair,
        withdrawalAmount = CryptoValue.fromMajor(pair.from, this.withdrawalAmount ?: BigDecimal.ZERO),
        depositAmount = CryptoValue.fromMajor(pair.to, this.depositAmount ?: BigDecimal.ZERO)
    )
}

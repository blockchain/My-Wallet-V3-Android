package com.blockchain.morph

import com.blockchain.morph.exchange.mvi.ExchangeIntent
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable

interface RateStream {

    fun rateStream(
        from: CryptoCurrency,
        to: CryptoCurrency,
        fiat: String
    ): Observable<ExchangeIntent>
}

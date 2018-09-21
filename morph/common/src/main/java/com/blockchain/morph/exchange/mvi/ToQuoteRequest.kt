package com.blockchain.morph.exchange.mvi

import com.blockchain.morph.quote.ExchangeQuoteRequest
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Observable

fun Observable<Pair<FieldUpdateIntent, Params>>.toQuoteRequest(): Observable<ExchangeQuoteRequest> =
    map { (fieldUpdate, params) ->
        when (fieldUpdate.fixedField) {
            Fix.BASE_CRYPTO -> ExchangeQuoteRequest.Selling(
                offering = CryptoValue.fromMajor(params.from, fieldUpdate.userValue),
                wanted = params.to
            )
            Fix.COUNTER_CRYPTO -> ExchangeQuoteRequest.Buying(
                offering = params.from,
                wanted = CryptoValue.fromMajor(params.to, fieldUpdate.userValue)
            )
            Fix.BASE_FIAT -> ExchangeQuoteRequest.SellingFiatLinked(
                offering = params.from,
                wanted = params.to,
                offeringFiatValue = FiatValue.fromMajor(params.fiat, fieldUpdate.userValue)
            )
            Fix.COUNTER_FIAT -> ExchangeQuoteRequest.BuyingFiatLinked(
                offering = params.from,
                wanted = params.to,
                wantedFiatValue = FiatValue.fromMajor(params.fiat, fieldUpdate.userValue)
            )
        }
    }

package com.blockchain.morph.exchange.mvi

import com.blockchain.morph.quote.ExchangeQuoteRequest
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.Observable

fun Observable<Pair<FieldUpdateIntent, Params>>.toQuoteRequest(): Observable<ExchangeQuoteRequest> =
    map { (fieldUpdate, params) ->
        when (fieldUpdate.field) {
            FieldUpdateIntent.Field.FROM_CRYPTO -> ExchangeQuoteRequest.Selling(
                offering = CryptoValue.fromMajor(params.from, fieldUpdate.userValue),
                wanted = params.to
            )
            FieldUpdateIntent.Field.TO_CRYPTO -> ExchangeQuoteRequest.Buying(
                offering = params.from,
                wanted = CryptoValue.fromMajor(params.to, fieldUpdate.userValue)
            )
            FieldUpdateIntent.Field.FROM_FIAT -> ExchangeQuoteRequest.SellingFiatLinked(
                offering = params.from,
                wanted = params.to,
                offeringFiatValue = FiatValue(params.fiat, fieldUpdate.userValue)
            )
            FieldUpdateIntent.Field.TO_FIAT -> ExchangeQuoteRequest.BuyingFiatLinked(
                offering = params.from,
                wanted = params.to,
                wantedFiatValue = FiatValue(params.fiat, fieldUpdate.userValue)
            )
        }
    }

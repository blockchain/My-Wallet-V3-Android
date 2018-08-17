package com.blockchain.morph

import com.blockchain.morph.quote.ExchangeQuoteRequest
import info.blockchain.balance.CryptoValue
import info.blockchain.wallet.shapeshift.data.QuoteRequest
import java.math.RoundingMode

fun ExchangeQuoteRequest.map(): QuoteRequest =
    QuoteRequest().also {
        it.pair = this.pair.pairCode
    }.also {
        when (this) {
            is ExchangeQuoteRequest.Selling -> mapSell(it)
            is ExchangeQuoteRequest.Buying -> mapBuy(it)
            else -> throw Exception("Not supported by shapeshift")
        }
    }

private fun ExchangeQuoteRequest.Selling.mapSell(request: QuoteRequest): QuoteRequest =
    request.also {
        request.depositAmount = offering.majorRoundedTo8()
    }

private fun ExchangeQuoteRequest.Buying.mapBuy(request: QuoteRequest): QuoteRequest =
    request.also {
        request.withdrawalAmount = wanted.majorRoundedTo8()
    }

private fun CryptoValue.majorRoundedTo8() = toMajorUnit().setScale(8, RoundingMode.HALF_DOWN)

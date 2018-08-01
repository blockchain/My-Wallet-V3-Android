package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.ExchangeRate

/**
 * The intents represent user/system actions
 */
sealed class ExchangeIntent

class FieldUpdateIntent(val field: Field, val userText: String) : ExchangeIntent() {

    enum class Field {
        FROM_CRYPTO,
        TO_CRYPTO,
        FROM_FIAT,
        TO_FIAT
    }
}

class CoinExchangeRateUpdateIntent(val exchangeRate: ExchangeRate.CryptoToCrypto) : ExchangeIntent() {
    val from = exchangeRate.from
    val to = exchangeRate.to
    val rate = exchangeRate.rate
}

class SwapIntent : ExchangeIntent()

class FiatExchangeRateUpdateIntent(val exchangeRate: ExchangeRate.CryptoToFiat) : ExchangeIntent() {
    val cryptoCurrency = exchangeRate.from
    val fiatCode = exchangeRate.to
    val rate = exchangeRate.rate
}

fun ExchangeRate.CryptoToCrypto.toIntent(): ExchangeIntent = CoinExchangeRateUpdateIntent(this)
fun ExchangeRate.CryptoToFiat.toIntent(): ExchangeIntent = FiatExchangeRateUpdateIntent(this)

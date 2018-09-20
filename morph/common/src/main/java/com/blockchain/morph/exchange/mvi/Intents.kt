package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.AccountReference
import info.blockchain.utils.tryParseBigDecimal
import java.math.BigDecimal

/**
 * The intents represent user/system actions
 */
sealed class ExchangeIntent

class FieldUpdateIntent(
    val field: Field,
    userText: String,
    val userValue: BigDecimal = userText.tryParseBigDecimal() ?: BigDecimal.ZERO
) : ExchangeIntent() {

    enum class Field {
        FROM_CRYPTO,
        TO_CRYPTO,
        FROM_FIAT,
        TO_FIAT
    }
}

class SwapIntent : ExchangeIntent()

class QuoteIntent(val quote: Quote) : ExchangeIntent()

class ChangeCryptoFromAccount(val from: AccountReference) : ExchangeIntent()

class ChangeCryptoToAccount(val to: AccountReference) : ExchangeIntent()

fun Quote.toIntent(): ExchangeIntent = QuoteIntent(this)

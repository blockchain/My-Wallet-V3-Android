package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.AccountReference
import info.blockchain.utils.tryParseBigDecimal
import java.math.BigDecimal

/**
 * The intents represent user/system actions
 */
sealed class ExchangeIntent

class FieldUpdateIntent(
    val fixedField: Fix,
    userText: String,
    val userValue: BigDecimal = userText.tryParseBigDecimal() ?: BigDecimal.ZERO
) : ExchangeIntent()

class SwapIntent : ExchangeIntent()

class QuoteIntent(val quote: Quote) : ExchangeIntent()

class ChangeCryptoFromAccount(val from: AccountReference) : ExchangeIntent()

class ChangeCryptoToAccount(val to: AccountReference) : ExchangeIntent()

fun Quote.toIntent(): ExchangeIntent = QuoteIntent(this)

package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import java.math.BigDecimal

/**
 * The intents represent user/system actions
 */
sealed class ExchangeIntent

class SimpleFieldUpdateIntent(val userValue: BigDecimal) : ExchangeIntent()

class SwapIntent : ExchangeIntent()

class QuoteIntent(val quote: Quote) : ExchangeIntent()

class SetFixIntent(val fix: Fix) : ExchangeIntent()

class ToggleFiatCryptoIntent : ExchangeIntent()

class ToggleFromToIntent : ExchangeIntent()

class ChangeCryptoFromAccount(val from: AccountReference) : ExchangeIntent()

class ChangeCryptoToAccount(val to: AccountReference) : ExchangeIntent()

fun Quote.toIntent(): ExchangeIntent = QuoteIntent(this)

class SetTradeLimits(val min: FiatValue, val max: FiatValue) : ExchangeIntent()

class ApplyMinimumLimit : ExchangeIntent()

class ApplyMaximumLimit : ExchangeIntent()

class FiatExchangeRateIntent(val c2fRate: ExchangeRate.CryptoToFiat) : ExchangeIntent()

class SpendableValueIntent(val cryptoValue: CryptoValue) : ExchangeIntent()

package com.blockchain.nabu.api

import com.blockchain.serialization.JsonSerializable

class TradeRequest(
    val destinationAddress: String,
    val refundAddress: String,
    val quote: ReceivedQuote
) : JsonSerializable

class ReceivedQuote(
    val pair: String,
    val fiatCurrency: String,
    val fix: String,
    val volume: Double,
    val currencyRatio: CurrencyRatio
) : JsonSerializable

class CurrencyRatio(
    val base: CryptoAndFiat,
    val counter: CryptoAndFiat,
    val baseToFiatRate: String,
    val baseToCounterRate: String,
    val counterToBaseRate: String,
    val counterToFiatRate: String
) : JsonSerializable

class CryptoAndFiat(
    val fiat: Value,
    val crypto: Value
) : JsonSerializable

class Value(
    val symbol: String,
    val value: Double
) : JsonSerializable
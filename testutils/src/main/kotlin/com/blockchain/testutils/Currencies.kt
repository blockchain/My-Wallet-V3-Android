package com.blockchain.testutils

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import java.math.BigDecimal

fun Number.gbp() = FiatValue.fromMajor("GBP", numberToBigDecimal())

fun Number.usd() = FiatValue.fromMajor("USD", numberToBigDecimal())

fun Number.cad() = FiatValue.fromMajor("CAD", numberToBigDecimal())

private fun Number.numberToBigDecimal(): BigDecimal =
    when (this) {
        is Double -> toBigDecimal()
        is Int -> toBigDecimal()
        is Long -> toBigDecimal()
        else -> throw NotImplementedError(this.javaClass.name)
    }

fun Number.bitcoin() = CryptoValue.bitcoinFromMajor(numberToBigDecimal())
fun Number.ether() = CryptoValue.etherFromMajor(numberToBigDecimal())
fun Number.bitcoinCash() = CryptoValue.bitcoinCashFromMajor(numberToBigDecimal())

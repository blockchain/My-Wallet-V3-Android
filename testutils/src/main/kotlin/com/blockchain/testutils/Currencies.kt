package com.blockchain.testutils

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue

fun Double.cad() = FiatValue("CAD", toBigDecimal())
fun Double.usd() = FiatValue("USD", toBigDecimal())
fun Double.gbp() = FiatValue("GBP", toBigDecimal())

fun Double.bitcoin() = CryptoValue.bitcoinFromMajor(toBigDecimal())
fun Double.ether() = CryptoValue.etherFromMajor(toBigDecimal())
fun Double.bitcoinCash() = CryptoValue.bitcoinCashFromMajor(toBigDecimal())

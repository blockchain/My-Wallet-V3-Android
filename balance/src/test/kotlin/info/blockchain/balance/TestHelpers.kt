package info.blockchain.balance

import java.math.BigDecimal

internal fun Number.gbp() = FiatValue.fromMajor("GBP", numberToBigDecimal())

internal fun Number.usd() = FiatValue.fromMajor("USD", numberToBigDecimal())

internal fun Number.jpy() = FiatValue.fromMajor("JPY", numberToBigDecimal())

internal fun Number.eur() = FiatValue.fromMajor("EUR", numberToBigDecimal())

internal fun Number.cad() = FiatValue.fromMajor("CAD", numberToBigDecimal())

internal fun Number.bitcoin() = CryptoValue.bitcoinFromMajor(numberToBigDecimal())

internal fun Number.ether() = CryptoValue.etherFromMajor(numberToBigDecimal())

private fun Number.numberToBigDecimal(): BigDecimal =
    when (this) {
        is Double -> toBigDecimal()
        is Int -> toBigDecimal()
        is Long -> toBigDecimal()
        else -> throw NotImplementedError(this.javaClass.name)
    }

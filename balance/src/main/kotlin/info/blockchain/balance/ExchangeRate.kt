package info.blockchain.balance

import java.math.BigDecimal

sealed class ExchangeRate(var rate: BigDecimal) {

    protected val rateInverse: BigDecimal get() = BigDecimal.valueOf(1.0 / rate.toDouble())

    class CryptoToCrypto(
        val from: CryptoCurrency,
        val to: CryptoCurrency,
        rate: BigDecimal
    ) : ExchangeRate(rate) {
        fun applyRate(cryptoValue: CryptoValue?): CryptoValue? {
            if (cryptoValue?.currency != from) return null
            return CryptoValue.fromMajor(
                to,
                rate.multiply(cryptoValue.toBigDecimal())
            )
        }

        fun inverse() =
            CryptoToCrypto(to, from, rateInverse)
    }

    class CryptoToFiat(
        val from: CryptoCurrency,
        val to: String,
        rate: BigDecimal
    ) : ExchangeRate(rate) {
        fun applyRate(cryptoValue: CryptoValue?): FiatValue? {
            if (cryptoValue?.currency != from) return null
            return FiatValue.fromMajor(
                currencyCode = to,
                major = rate.multiply(cryptoValue.toBigDecimal())
            )
        }

        fun inverse() =
            FiatToCrypto(to, from, rateInverse)
    }

    class FiatToCrypto(
        val from: String,
        val to: CryptoCurrency,
        rate: BigDecimal
    ) : ExchangeRate(rate) {
        fun applyRate(fiatValue: FiatValue?): CryptoValue? {
            if (fiatValue?.currencyCode != from) return null
            return CryptoValue.fromMajor(
                to,
                rate.multiply(fiatValue.toBigDecimal())
            )
        }

        fun inverse() =
            CryptoToFiat(to, from, rateInverse)
    }
}

operator fun CryptoValue?.times(rate: ExchangeRate.CryptoToCrypto?) =
    rate?.applyRate(this)

operator fun CryptoValue?.div(rate: ExchangeRate.CryptoToCrypto?) =
    rate?.inverse()?.applyRate(this)

operator fun FiatValue?.times(rate: ExchangeRate.FiatToCrypto?) =
    rate?.applyRate(this)

operator fun CryptoValue?.times(exchangeRate: ExchangeRate.CryptoToFiat?) =
    exchangeRate?.applyRate(this)

operator fun CryptoValue?.div(exchangeRate: ExchangeRate.FiatToCrypto?) =
    exchangeRate?.inverse()?.applyRate(this)

operator fun FiatValue?.div(exchangeRate: ExchangeRate.CryptoToFiat?) =
    exchangeRate?.inverse()?.applyRate(this)

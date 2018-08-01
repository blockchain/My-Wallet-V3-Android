package info.blockchain.balance

import java.math.BigDecimal

sealed class ExchangeRate(var rate: BigDecimal?) {

    protected val rateInverse get() = rate?.let { BigDecimal.valueOf(1.0 / it.toDouble()) }

    class CryptoToCrypto(
        val from: CryptoCurrency,
        val to: CryptoCurrency,
        rate: BigDecimal?
    ) : ExchangeRate(rate) {
        fun applyRate(cryptoValue: CryptoValue?): CryptoValue? {
            if (cryptoValue?.currency != from) return null
            return rate?.let {
                CryptoValue.fromMajor(
                    to,
                    it.multiply(cryptoValue.toMajorUnit())
                )
            }
        }
    }

    class CryptoToFiat(
        val from: CryptoCurrency,
        val to: String,
        rate: BigDecimal?
    ) : ExchangeRate(rate) {
        fun applyRate(cryptoValue: CryptoValue?): FiatValue? {
            if (cryptoValue?.currency != from) return null
            return rate?.let {
                FiatValue(
                    currencyCode = to,
                    value = it.multiply(cryptoValue.toMajorUnit())
                )
            }
        }

        fun inverse(): FiatToCrypto {
            return FiatToCrypto(to, from, rateInverse)
        }
    }

    class FiatToCrypto(
        val from: String,
        val to: CryptoCurrency,
        rate: BigDecimal?
    ) : ExchangeRate(rate) {
        fun applyRate(fiatValue: FiatValue): CryptoValue? {
            if (fiatValue.currencyCode != from) return null
            return rate?.let {
                CryptoValue.fromMajor(
                    to,
                    it.multiply(fiatValue.value)
                )
            }
        }

        fun inverse(): CryptoToFiat {
            return CryptoToFiat(to, from, rateInverse)
        }
    }
}

operator fun CryptoValue.times(rate: ExchangeRate.CryptoToCrypto?) =
    rate?.applyRate(this)

operator fun FiatValue.times(rate: ExchangeRate.FiatToCrypto?) =
    rate?.applyRate(this)

operator fun CryptoValue.times(exchangeRate: ExchangeRate.CryptoToFiat?) =
    exchangeRate?.applyRate(this)

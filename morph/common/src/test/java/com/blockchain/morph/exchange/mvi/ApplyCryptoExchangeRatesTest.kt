package com.blockchain.morph.exchange.mvi

import com.blockchain.morph.CoinPair
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import org.junit.Test

class ApplyCryptoExchangeRatesTest {

    @Test
    fun `"from crypto" entered`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.FROM_CRYPTO,
                "10"
            ),
            coinExchangeRateUpdateIntent(CoinPair.BTC_TO_ETH rate 2.5.toBigDecimal())
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        userEntered(CryptoValue.bitcoinFromMajor(10)),
                        outOfDate(zeroFiat("CAD"))
                    ),
                    to = value(
                        upToDate(CryptoValue.etherFromMajor(25)),
                        outOfDate(zeroFiat("CAD"))
                    )
                )
            )
        }
    }

    @Test
    fun `"to crypto" entered`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.TO_CRYPTO,
                "10"
            ),
            coinExchangeRateUpdateIntent(CoinPair.ETH_TO_BTC rate 2.5.toBigDecimal())
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(CryptoValue.bitcoinFromMajor(25.toBigDecimal())),
                        outOfDate(zeroFiat("CAD"))
                    ),
                    to = value(
                        userEntered(CryptoValue.etherFromMajor(10)),
                        outOfDate(zeroFiat("CAD"))
                    )
                )
            )
        }
    }

    @Test
    fun `"from crypto" entered - wrong symbol on exchange rate`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.FROM_CRYPTO,
                "10"
            ),
            coinExchangeRateUpdateIntent(CoinPair.ETH_TO_BTC rate 2.5.toBigDecimal())
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        userEntered(CryptoValue.bitcoinFromMajor(10)),
                        outOfDate(zeroFiat("CAD"))
                    ),
                    to = value(
                        outOfDate(CryptoValue.ZeroEth),
                        outOfDate(
                            zeroFiat("CAD")
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `"from crypto" entered - wrong "to" symbol on exchange rate`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.FROM_CRYPTO,
                "10"
            ),
            coinExchangeRateUpdateIntent(CoinPair.BTC_TO_BCH rate 2.5.toBigDecimal())
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        userEntered(CryptoValue.bitcoinFromMajor(10)),
                        outOfDate(zeroFiat("CAD"))
                    ),
                    to = value(
                        outOfDate(CryptoValue.ZeroEth),
                        outOfDate(zeroFiat("CAD"))
                    )
                )
            )
        }
    }

    @Test
    fun `"to crypto" entered - wrong "to" symbol on exchange rate`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.TO_CRYPTO,
                "10"
            ),
            coinExchangeRateUpdateIntent(CoinPair.BTC_TO_BCH rate 2.5.toBigDecimal())
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        outOfDate(CryptoValue.ZeroBtc),
                        outOfDate(zeroFiat("CAD"))
                    ),
                    to = value(
                        userEntered(CryptoValue.etherFromMajor(10)),
                        outOfDate(zeroFiat("CAD"))
                    )
                )
            )
        }
    }
}

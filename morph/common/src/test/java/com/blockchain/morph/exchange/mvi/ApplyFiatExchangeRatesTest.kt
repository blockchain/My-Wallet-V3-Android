package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.junit.Test

class ApplyFiatExchangeRatesTest {

    @Test
    fun `fiat update - matching the "to crypto"`() {
        given(
            ExchangeViewModel(
                from = value(
                    upToDate(CryptoValue.etherFromMajor(10)),
                    outOfDate(zeroFiat("CAD"))
                ),
                to = value(
                    userEntered(CryptoValue.bitcoinFromMajor(25)),
                    outOfDate(zeroFiat("CAD"))
                )
            )
        ).on(
            fiatExchangeRateUpdateIntent(
                "CAD",
                CryptoCurrency.BTC,
                800.toBigDecimal()
            )
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        outOfDate(CryptoValue.ZeroEth),
                        outOfDate(zeroFiat("CAD"))
                    ),
                    to = value(
                        userEntered(CryptoValue.bitcoinFromMajor(25)),
                        upToDate(
                            FiatValue("CAD", (800 * 25).toBigDecimal().setScale(CryptoCurrency.BTC.dp))
                        )
                    )
                )
            )
        }
    }

    @Test
    fun `fiat update - matching the "from crypto"`() {
        given(
            ExchangeViewModel(
                from = value(
                    userEntered(CryptoValue.etherFromMajor(10)),
                    outOfDate(zeroFiat("CAD"))
                ),
                to = value(
                    upToDate(CryptoValue.bitcoinFromMajor(25)),
                    outOfDate(zeroFiat("CAD"))
                )
            )
        ).on(
            fiatExchangeRateUpdateIntent(
                "CAD",
                CryptoCurrency.ETHER,
                500.toBigDecimal()
            )
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        userEntered(CryptoValue.etherFromMajor(10)),
                        upToDate(
                            FiatValue("CAD", (500 * 10).toBigDecimal().setScale(CryptoCurrency.ETHER.dp))
                        )
                    ),
                    to = value(
                        outOfDate(CryptoValue.ZeroBtc),
                        outOfDate(zeroFiat("CAD"))
                    )
                )
            )
        }
    }

    @Test
    fun `fiat update - matching the "to crypto" but not the currency code`() {
        val model = ExchangeViewModel(
            from = value(
                userEntered(CryptoValue.etherFromMajor(10)),
                outOfDate(zeroFiat("USD"))
            ),
            to = value(
                upToDate(CryptoValue.bitcoinFromMajor(25)),
                outOfDate(zeroFiat("USD"))
            )
        )
        given(
            model
        ).on(
            fiatExchangeRateUpdateIntent(
                "CAD",
                CryptoCurrency.BTC,
                800.toBigDecimal()
            )
        ) {
            assertValue(model)
        }
    }

    @Test
    fun `fiat update - matching the "from crypto" but not the currency code`() {
        val model = ExchangeViewModel(
            from = value(
                userEntered(CryptoValue.etherFromMajor(10)),
                outOfDate(zeroFiat("USD"))
            ),
            to = value(
                upToDate(CryptoValue.bitcoinFromMajor(25)),
                outOfDate(zeroFiat("USD"))
            )
        )
        given(
            model
        ).on(
            fiatExchangeRateUpdateIntent(
                "CAD",
                CryptoCurrency.ETHER,
                800.toBigDecimal()
            )
        ) {
            assertValue(model)
        }
    }
}

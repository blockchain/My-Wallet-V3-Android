package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.ExchangeRate
import info.blockchain.balance.FiatValue
import org.junit.Test

/**
 * Tests that cover the conversion from one field to each of the other three.
 */
class FullCircleTest {

    @Test
    fun `"from crypto" updates the other three fields`() {
        given(
            initial("GBP", CryptoCurrency.BCH to CryptoCurrency.BTC)
        ).on(
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BTC, 7000.toBigDecimal()),
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BCH, 3500.toBigDecimal()),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BCH,
                    CryptoCurrency.BTC,
                    0.49.toBigDecimal()
                )
            ),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BTC,
                    CryptoCurrency.BCH,
                    1.99.toBigDecimal()
                )
            ),
            FieldUpdateIntent(FieldUpdateIntent.Field.FROM_CRYPTO, "5")
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        userEntered(CryptoValue.bitcoinCashFromMajor(5)),
                        upToDate(FiatValue("GBP", 17500.toBigDecimal().setScale(8)))

                    ),
                    to = value(
                        upToDate(CryptoValue.bitcoinFromMajor(2.45.toBigDecimal())),
                        upToDate(FiatValue("GBP", 17150.toBigDecimal().setScale(8)))
                    )
                )
            )
        }
    }

    @Test
    fun `"to crypto" updates the other three fields`() {
        given(
            initial("GBP", CryptoCurrency.BCH to CryptoCurrency.BTC)
        ).on(
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BTC, 7000.toBigDecimal()),
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BCH, 3500.toBigDecimal()),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BCH,
                    CryptoCurrency.BTC,
                    0.49.toBigDecimal()
                )
            ),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BTC,
                    CryptoCurrency.BCH,
                    1.99.toBigDecimal()
                )
            ),
            FieldUpdateIntent(FieldUpdateIntent.Field.TO_CRYPTO, "5")
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(CryptoValue.bitcoinCashFromMajor(9.95.toBigDecimal())),
                        upToDate(FiatValue("GBP", 34825.toBigDecimal().setScale(8)))
                    ),
                    to = value(
                        userEntered(CryptoValue.bitcoinFromMajor(5.toBigDecimal())),
                        upToDate(FiatValue("GBP", 35000.toBigDecimal().setScale(8)))
                    )
                )
            )
        }
    }

    @Test
    fun `"from fiat" updates the other three fields`() {
        given(
            initial("GBP", CryptoCurrency.BCH to CryptoCurrency.BTC)
        ).on(
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BTC, 7000.toBigDecimal()),
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BCH, 3500.toBigDecimal()),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BCH,
                    CryptoCurrency.BTC,
                    0.49.toBigDecimal()
                )
            ),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BTC,
                    CryptoCurrency.BCH,
                    1.99.toBigDecimal()
                )
            ),
            FieldUpdateIntent(FieldUpdateIntent.Field.FROM_FIAT, "35000")
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(CryptoValue.bitcoinCashFromMajor(10)),
                        userEntered(FiatValue("GBP", 35000.toBigDecimal()))
                    ),
                    to = value(
                        upToDate(CryptoValue.bitcoinFromMajor(4.9.toBigDecimal())),
                        upToDate(FiatValue("GBP", 34300.toBigDecimal().setScale(8)))
                    )
                )
            )
        }
    }

    @Test
    fun `"to fiat" updates the other three fields`() {
        given(
            initial("GBP", CryptoCurrency.BCH to CryptoCurrency.BTC)
        ).on(
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BTC, 7000.toBigDecimal()),
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BCH, 3500.toBigDecimal()),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BCH,
                    CryptoCurrency.BTC,
                    0.49.toBigDecimal()
                )
            ),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BTC,
                    CryptoCurrency.BCH,
                    1.99.toBigDecimal()
                )
            ),
            FieldUpdateIntent(FieldUpdateIntent.Field.TO_FIAT, "35000")
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(CryptoValue.bitcoinCashFromMajor(9.95.toBigDecimal())),
                        upToDate(FiatValue("GBP", 34825.toBigDecimal().setScale(8)))
                    ),
                    to = value(
                        upToDate(CryptoValue.bitcoinFromMajor(5.toBigDecimal())),
                        userEntered(FiatValue("GBP", 35000.toBigDecimal()))
                    )
                )
            )
        }
    }

    @Test
    fun `"from fiat" updates the other three fields - with swap`() {
        given(
            initial("GBP", CryptoCurrency.BTC to CryptoCurrency.BCH)
        ).on(
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BTC, 7000.toBigDecimal()),
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BCH, 3500.toBigDecimal()),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BCH,
                    CryptoCurrency.BTC,
                    0.49.toBigDecimal()
                )
            ),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BTC,
                    CryptoCurrency.BCH,
                    1.99.toBigDecimal()
                )
            ),
            SwapIntent(),
            FieldUpdateIntent(FieldUpdateIntent.Field.FROM_FIAT, "35000")
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(CryptoValue.bitcoinCashFromMajor(10)),
                        userEntered(FiatValue("GBP", 35000.toBigDecimal()))
                    ),
                    to = value(
                        upToDate(CryptoValue.bitcoinFromMajor(4.9.toBigDecimal())),
                        upToDate(FiatValue("GBP", 34300.toBigDecimal().setScale(8)))
                    )
                )
            )
        }
    }

    @Test
    fun `"to fiat" updates the other three fields - with swap`() {
        given(
            initial("GBP", CryptoCurrency.BTC to CryptoCurrency.BCH)
        ).on(
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BTC, 7000.toBigDecimal()),
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BCH, 3500.toBigDecimal()),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BCH,
                    CryptoCurrency.BTC,
                    0.49.toBigDecimal()
                )
            ),
            coinExchangeRateUpdateIntent(
                ExchangeRate.CryptoToCrypto(
                    CryptoCurrency.BTC,
                    CryptoCurrency.BCH,
                    1.99.toBigDecimal()
                )
            ),
            SwapIntent(),
            FieldUpdateIntent(FieldUpdateIntent.Field.TO_FIAT, "35000")
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(CryptoValue.bitcoinCashFromMajor(9.95.toBigDecimal())),
                        upToDate(FiatValue("GBP", 34825.toBigDecimal().setScale(8)))
                    ),
                    to = value(
                        upToDate(CryptoValue.bitcoinFromMajor(5.toBigDecimal())),
                        userEntered(FiatValue("GBP", 35000.toBigDecimal()))
                    )
                )
            )
        }
    }
}

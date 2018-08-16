package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.junit.Test

class ApplyQuoteTest {

    @Test
    fun `"from crypto" entered`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.FROM_CRYPTO,
                "10"
            ),
            Quote(
                from = 10.0.bitcoin() `equivalent to` 99.12.cad(),
                to = 25.0.ether() `equivalent to` 95.32.cad()
            ).toIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        userEntered(10.0.bitcoin()),
                        upToDate(99.12.cad())
                    ),
                    to = value(
                        upToDate(25.0.ether()),
                        upToDate(95.32.cad())
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
                "15"
            ),
            Quote(
                from = 9.0.bitcoin() `equivalent to` 299.12.cad(),
                to = 15.0.ether() `equivalent to` 295.32.cad()
            ).toIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(9.0.bitcoin()),
                        upToDate(299.12.cad())
                    ),
                    to = value(
                        userEntered(15.0.ether()),
                        upToDate(295.32.cad())
                    )
                )
            )
        }
    }

    @Test
    fun `"from fiat" entered`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.FROM_FIAT,
                "10"
            ),
            Quote(
                from = 10.0.bitcoin() `equivalent to` 99.12.cad(),
                to = 25.0.ether() `equivalent to` 95.32.cad()
            ).toIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(10.0.bitcoin()),
                        userEntered(99.12.cad())
                    ),
                    to = value(
                        upToDate(25.0.ether()),
                        upToDate(95.32.cad())
                    )
                )
            )
        }
    }

    @Test
    fun `"to fiat" entered`() {
        given(
            initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.TO_FIAT,
                "10"
            ),
            Quote(
                from = 10.0.bitcoin() `equivalent to` 99.12.usd(),
                to = 25.0.ether() `equivalent to` 95.32.usd()
            ).toIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(10.0.bitcoin()),
                        upToDate(99.12.usd())
                    ),
                    to = value(
                        upToDate(25.0.ether()),
                        userEntered(95.32.usd())
                    )
                )
            )
        }
    }

    @Test
    fun `ignore mismatch quote by "from fiat" currency`() {
        given(
            initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.TO_FIAT,
                "10"
            ),
            Quote(
                from = 10.0.bitcoin() `equivalent to` 99.12.usd(),
                to = 25.0.ether() `equivalent to` 95.32.usd()
            ).toIntent(),
            Quote(
                from = 10.0.bitcoin() `equivalent to` 999.12.cad(),
                to = 25.0.ether() `equivalent to` 995.32.usd()
            ).toIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(10.0.bitcoin()),
                        upToDate(99.12.usd())
                    ),
                    to = value(
                        upToDate(25.0.ether()),
                        userEntered(95.32.usd())
                    )
                )
            )
        }
    }

    @Test
    fun `ignore mismatch quote by "to fiat" currency`() {
        given(
            initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.TO_FIAT,
                "10"
            ),
            Quote(
                from = 10.0.bitcoin() `equivalent to` 99.12.usd(),
                to = 25.0.ether() `equivalent to` 95.32.usd()
            ).toIntent(),
            Quote(
                from = 10.0.bitcoin() `equivalent to` 999.12.usd(),
                to = 25.0.ether() `equivalent to` 995.32.cad()
            ).toIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(10.0.bitcoin()),
                        upToDate(99.12.usd())
                    ),
                    to = value(
                        upToDate(25.0.ether()),
                        userEntered(95.32.usd())
                    )
                )
            )
        }
    }

    @Test
    fun `ignore mismatch quote by "from crypto" currency`() {
        given(
            initial("USD", CryptoCurrency.BCH to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.TO_FIAT,
                "10"
            ),
            Quote(
                from = 10.0.bitcoinCash() `equivalent to` 99.12.usd(),
                to = 25.0.ether() `equivalent to` 95.32.usd()
            ).toIntent(),
            Quote(
                from = 20.0.bitcoin() `equivalent to` 199.12.usd(),
                to = 35.0.ether() `equivalent to` 295.32.usd()
            ).toIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(10.0.bitcoinCash()),
                        upToDate(99.12.usd())
                    ),
                    to = value(
                        upToDate(25.0.ether()),
                        userEntered(95.32.usd())
                    )
                )
            )
        }
    }

    @Test
    fun `ignore mismatch quote by "to crypto" currency`() {
        given(
            initial("USD", CryptoCurrency.BCH to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(
                FieldUpdateIntent.Field.TO_FIAT,
                "10"
            ),
            Quote(
                from = 10.0.bitcoinCash() `equivalent to` 99.12.usd(),
                to = 25.0.ether() `equivalent to` 95.32.usd()
            ).toIntent(),
            Quote(
                from = 910.0.bitcoinCash() `equivalent to` 999.12.usd(),
                to = 99.0.bitcoin() `equivalent to` 995.32.usd()
            ).toIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(10.0.bitcoinCash()),
                        upToDate(99.12.usd())
                    ),
                    to = value(
                        upToDate(25.0.ether()),
                        userEntered(95.32.usd())
                    )
                )
            )
        }
    }

    fun Double.cad() = FiatValue("CAD", toBigDecimal())
    fun Double.usd() = FiatValue("USD", toBigDecimal())

    fun Double.bitcoin() = CryptoValue.bitcoinFromMajor(toBigDecimal())
    fun Double.ether() = CryptoValue.etherFromMajor(toBigDecimal())
    fun Double.bitcoinCash() = CryptoValue.bitcoinCashFromMajor(toBigDecimal())

    infix fun CryptoValue.`equivalent to`(fiatValue: FiatValue) =
        Quote.Value(this, fiatValue)
}
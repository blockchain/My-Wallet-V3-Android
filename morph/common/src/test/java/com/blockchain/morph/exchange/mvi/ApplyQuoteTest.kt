package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.cad
import com.blockchain.testutils.ether
import com.blockchain.testutils.usd
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
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
                from = 10.bitcoin() `equivalent to` 99.12.cad(),
                to = 25.ether() `equivalent to` 95.32.cad()
            ).toIntent()
        ) {
            assertValue {
                it.from `should equal` value(
                    userEntered(10.bitcoin()),
                    upToDate(99.12.cad())
                )
                it.to `should equal` value(
                    upToDate(25.ether()),
                    upToDate(95.32.cad())
                )
                true
            }
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
                from = 9.bitcoin() `equivalent to` 299.12.cad(),
                to = 15.ether() `equivalent to` 295.32.cad()
            ).toIntent()
        ) {
            assertValue {
                it.from `should equal` value(
                    upToDate(9.bitcoin()),
                    upToDate(299.12.cad())
                )
                it.to `should equal` value(
                    userEntered(15.ether()),
                    upToDate(295.32.cad())
                )
                true
            }
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
                from = 10.bitcoin() `equivalent to` 99.12.cad(),
                to = 25.ether() `equivalent to` 95.32.cad()
            ).toIntent()
        ) {
            assertValue {
                it.from `should equal` value(
                    upToDate(10.bitcoin()),
                    userEntered(99.12.cad())
                )
                it.to `should equal` value(
                    upToDate(25.ether()),
                    upToDate(95.32.cad())
                )
                true
            }
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
                from = 10.bitcoin() `equivalent to` 99.12.usd(),
                to = 25.ether() `equivalent to` 95.32.usd()
            ).toIntent()
        ) {
            assertValue {
                it.from `should equal` value(
                    upToDate(10.bitcoin()),
                    upToDate(99.12.usd())
                )
                it.to `should equal` value(
                    upToDate(25.ether()),
                    userEntered(95.32.usd())
                )
                true
            }
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
                from = 10.bitcoin() `equivalent to` 99.12.usd(),
                to = 25.ether() `equivalent to` 95.32.usd()
            ).toIntent(),
            Quote(
                from = 10.bitcoin() `equivalent to` 999.12.cad(),
                to = 25.ether() `equivalent to` 995.32.usd()
            ).toIntent()
        ) {
            assertValue {
                it.from `should equal` value(
                    upToDate(10.bitcoin()),
                    upToDate(99.12.usd())
                )
                it.to `should equal` value(
                    upToDate(25.ether()),
                    userEntered(95.32.usd())
                )
                true
            }
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
                from = 10.bitcoin() `equivalent to` 99.12.usd(),
                to = 25.ether() `equivalent to` 95.32.usd()
            ).toIntent(),
            Quote(
                from = 10.bitcoin() `equivalent to` 999.12.usd(),
                to = 25.ether() `equivalent to` 995.32.cad()
            ).toIntent()
        ) {
            assertValue {
                it.from `should equal` value(
                    upToDate(10.bitcoin()),
                    upToDate(99.12.usd())
                )
                it.to `should equal` value(
                    upToDate(25.ether()),
                    userEntered(95.32.usd())
                )
                true
            }
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
                from = 10.bitcoinCash() `equivalent to` 99.12.usd(),
                to = 25.ether() `equivalent to` 95.32.usd()
            ).toIntent(),
            Quote(
                from = 20.bitcoin() `equivalent to` 199.12.usd(),
                to = 35.ether() `equivalent to` 295.32.usd()
            ).toIntent()
        ) {
            assertValue {
                it.from `should equal` value(
                    upToDate(10.bitcoinCash()),
                    upToDate(99.12.usd())
                )
                it.to `should equal` value(
                    upToDate(25.ether()),
                    userEntered(95.32.usd())
                )
                true
            }
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
                from = 10.bitcoinCash() `equivalent to` 99.12.usd(),
                to = 25.ether() `equivalent to` 95.32.usd()
            ).toIntent(),
            Quote(
                from = 910.bitcoinCash() `equivalent to` 999.12.usd(),
                to = 99.bitcoin() `equivalent to` 995.32.usd()
            ).toIntent()
        ) {
            assertValue {
                it.from `should equal` value(
                    upToDate(10.bitcoinCash()),
                    upToDate(99.12.usd())
                )
                it.to `should equal` value(
                    upToDate(25.ether()),
                    userEntered(95.32.usd())
                )
                true
            }
        }
    }

    @Test
    fun `quote is available in raw form on the view model`() {
        val theQuote = Quote(
            from = 10.bitcoin() `equivalent to` 99.12.cad(),
            to = 25.ether() `equivalent to` 95.32.cad()
        )
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        ).on(
            theQuote.toIntent()
        ) {
            assertValue {
                it.latestQuote `should be` theQuote
                true
            }
        }
    }

    private infix fun CryptoValue.`equivalent to`(fiatValue: FiatValue) =
        Quote.Value(this, fiatValue)
}

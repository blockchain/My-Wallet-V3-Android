package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.after
import com.blockchain.testutils.before
import com.blockchain.testutils.gbp
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import org.amshove.kluent.`should equal`
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class UserInputTest {

    @Test
    fun `user value is sanitized`() {
        FieldUpdateIntent(
            Fix.BASE_CRYPTO,
            "1,000.123 BTC"
        )
            .userValue `should equal` 1000.123.toBigDecimal()
    }

    @Test
    fun `user "from crypto" input with some extra characters`() {
        given(
            initial(
                "GBP",
                CryptoCurrency.BCH to CryptoCurrency.ETHER
            )
        ).on(
            FieldUpdateIntent(
                Fix.BASE_CRYPTO,
                "1,000.123 BTC"
            )
        ) {
            assertValue {
                it.from `should equal` value(
                    userEntered(
                        CryptoValue.bitcoinCashFromMajor(
                            1000.123.toBigDecimal()
                        )
                    ),
                    outOfDate(zeroFiat("GBP"))
                )
                it.to `should equal` value(
                    outOfDate(CryptoValue.ZeroEth),
                    outOfDate(zeroFiat("GBP"))
                )
                true
            }
        }
    }

    @Test
    fun `user "to crypto" input with some extra characters`() {
        given(
            initial(
                "GBP",
                CryptoCurrency.BCH to CryptoCurrency.ETHER
            )
        ).on(
            FieldUpdateIntent(
                Fix.COUNTER_CRYPTO,
                "1,000.123 BCH"
            )
        ) {
            assertValue { it.to.cryptoValue == CryptoValue.etherFromMajor(1000.123.toBigDecimal()) }
        }
    }

    @Test
    fun `user "from fiat" input with some extra characters`() {
        given(
            initial(
                "GBP",
                CryptoCurrency.BCH to CryptoCurrency.ETHER
            )
        ).on(
            FieldUpdateIntent(
                Fix.BASE_FIAT,
                "GBP 99.34"
            )
        ) {
            assertValue { it.from.fiatValue == 99.34.gbp() }
        }
    }

    @Test
    fun `user "to fiat" input with some extra characters`() {
        given(
            initial(
                "GBP",
                CryptoCurrency.BCH to CryptoCurrency.ETHER
            )
        ).on(
            FieldUpdateIntent(
                Fix.COUNTER_FIAT,
                "IGNORED88.56X"
            )
        ) {
            assertValue { it.to.fiatValue == 88.56.gbp() }
        }
    }
}

class UserInputWithAlternativeLocaleTest {

    @get:Rule
    val italianLocale = before {
        Locale.setDefault(Locale.ITALIAN)
    } after {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `user value is sanitized`() {
        FieldUpdateIntent(Fix.BASE_FIAT, "$1.000,123")
            .userValue `should equal` 1000.123.toBigDecimal()
    }

    @Test
    fun `user "from crypto" input with some extra characters`() {
        given(
            initial("GBP", CryptoCurrency.BCH to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(Fix.BASE_CRYPTO, "1.000,123 BTC")
        ) {
            assertValue { it.from.cryptoValue == CryptoValue.bitcoinCashFromMajor(1000.123.toBigDecimal()) }
        }
    }
}

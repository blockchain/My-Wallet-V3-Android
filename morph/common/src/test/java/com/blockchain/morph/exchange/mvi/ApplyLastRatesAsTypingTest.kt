package com.blockchain.morph.exchange.mvi

import com.blockchain.morph.CoinPair
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.junit.Test

class ApplyLastRatesAsTypingTest {

    @Test
    fun `apply last crypto rates when "from crypto" was entered`() {
        given(
            initial("GBP", CryptoCurrency.BCH to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(FieldUpdateIntent.Field.FROM_CRYPTO, "1"),
            coinExchangeRateUpdateIntent(CoinPair.BCH_TO_ETH rate 0.5.toBigDecimal()),
            FieldUpdateIntent(FieldUpdateIntent.Field.FROM_CRYPTO, "10")
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        userEntered(CryptoValue.bitcoinCashFromMajor(10)),
                        outOfDate(zeroFiat("GBP"))
                    ),
                    to = value(
                        upToDate(CryptoValue.etherFromMajor(5)),
                        outOfDate(zeroFiat("GBP"))
                    )
                )
            )
        }
    }

    @Test
    fun `apply last crypto rates when "to crypto" was entered`() {
        given(
            initial("GBP", CryptoCurrency.BCH to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(FieldUpdateIntent.Field.TO_CRYPTO, "1"),
            coinExchangeRateUpdateIntent(CoinPair.BCH_TO_ETH rate 0.5.toBigDecimal()),
            FieldUpdateIntent(FieldUpdateIntent.Field.TO_CRYPTO, "10")
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(CryptoValue.bitcoinCashFromMajor(20)),
                        outOfDate(zeroFiat("GBP"))
                    ),
                    to = value(
                        userEntered(CryptoValue.etherFromMajor(10)),
                        outOfDate(zeroFiat("GBP"))
                    )
                )
            )
        }
    }

    @Test
    fun `apply last fiat rates when "from crypto" was entered`() {
        given(
            initial("GBP", CryptoCurrency.BCH to CryptoCurrency.ETHER)
        ).on(
            FieldUpdateIntent(FieldUpdateIntent.Field.FROM_CRYPTO, "1"),
            fiatExchangeRateUpdateIntent("GBP", CryptoCurrency.BCH, 1000.toBigDecimal()),
            FieldUpdateIntent(FieldUpdateIntent.Field.FROM_CRYPTO, "10")
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        userEntered(CryptoValue.bitcoinCashFromMajor(10)),
                        upToDate(FiatValue("GBP", 10000.toBigDecimal().setScale(8)))
                    ),
                    to = value(
                        outOfDate(CryptoValue.ZeroEth),
                        outOfDate(zeroFiat("GBP"))
                    )
                )
            )
        }
    }
}

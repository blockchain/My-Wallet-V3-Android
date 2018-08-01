package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import org.junit.Test

class SwapIntentTest {

    @Test
    fun `can swap the "to" and "from" currencies`() {
        given(
            ExchangeViewModel(
                from = value(
                    userEntered(CryptoValue.etherFromMajor(10)),
                    upToDate(FiatValue("GBP", 100.toBigDecimal()))
                ),
                to = value(
                    upToDate(CryptoValue.bitcoinCashFromMajor(25)),
                    upToDate(FiatValue("GBP", 99.toBigDecimal()))
                )
            )
        ).on(
            SwapIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(CryptoValue.ZeroBch),
                        upToDate(zeroFiat("GBP"))
                    ),
                    to = value(
                        upToDate(CryptoValue.ZeroEth),
                        upToDate(zeroFiat("GBP"))
                    )
                )
            )
        }
    }
}
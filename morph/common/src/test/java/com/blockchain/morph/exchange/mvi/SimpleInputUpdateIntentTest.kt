package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.ether
import com.blockchain.testutils.gbp
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import org.junit.Test

class SimpleInputUpdateIntentTest {

    @Test
    fun `can update the active field - Base crypto fix`() {
        given(
            ExchangeViewModel(
                fromAccount = aFromAccount(),
                toAccount = aToAccount(),
                from = value(
                    userEntered(10.ether()),
                    upToDate(100.gbp())
                ),
                to = value(
                    upToDate(25.bitcoinCash()),
                    upToDate(99.gbp())
                )
            )
        ).on(
            SimpleFieldUpdateIntent(123.45.toBigDecimal())
        ) {
            assertValue(
                ExchangeViewModel(
                    fromAccount = aFromAccount(),
                    toAccount = aToAccount(),
                    from = value(
                        userEntered(123.45.ether()),
                        outOfDate(100.gbp())
                    ),
                    to = value(
                        outOfDate(25.bitcoinCash()),
                        outOfDate(99.gbp())
                    )
                )
            )
        }
    }

    @Test
    fun `can update the active field - Base fiat fix`() {
        given(
            ExchangeViewModel(
                fromAccount = aFromAccount(),
                toAccount = aToAccount(),
                from = value(
                    upToDate(10.ether()),
                    userEntered(100.gbp())
                ),
                to = value(
                    upToDate(25.bitcoinCash()),
                    upToDate(99.gbp())
                )
            )
        ).on(
            SimpleFieldUpdateIntent(123.45.toBigDecimal())
        ) {
            assertValue(
                ExchangeViewModel(
                    fromAccount = aFromAccount(),
                    toAccount = aToAccount(),
                    from = value(
                        outOfDate(10.ether()),
                        userEntered(123.45.gbp())
                    ),
                    to = value(
                        outOfDate(25.bitcoinCash()),
                        outOfDate(99.gbp())
                    )
                )
            )
        }
    }

    @Test
    fun `can update the active field - Counter crypto fix`() {
        given(
            ExchangeViewModel(
                fromAccount = aFromAccount(),
                toAccount = aToAccount(),
                from = value(
                    upToDate(10.ether()),
                    upToDate(100.gbp())
                ),
                to = value(
                    userEntered(25.bitcoinCash()),
                    upToDate(99.gbp())
                )
            )
        ).on(
            SimpleFieldUpdateIntent(123.45.toBigDecimal())
        ) {
            assertValue(
                ExchangeViewModel(
                    fromAccount = aFromAccount(),
                    toAccount = aToAccount(),
                    from = value(
                        outOfDate(10.ether()),
                        outOfDate(100.gbp())
                    ),
                    to = value(
                        userEntered(123.45.bitcoinCash()),
                        outOfDate(99.gbp())
                    )
                )
            )
        }
    }

    @Test
    fun `can update the active field - Counter fiat fix`() {
        given(
            ExchangeViewModel(
                fromAccount = aFromAccount(),
                toAccount = aToAccount(),
                from = value(
                    upToDate(10.ether()),
                    upToDate(100.gbp())
                ),
                to = value(
                    upToDate(25.bitcoinCash()),
                    userEntered(99.gbp())
                )
            )
        ).on(
            SimpleFieldUpdateIntent(456.78.toBigDecimal())
        ) {
            assertValue(
                ExchangeViewModel(
                    fromAccount = aFromAccount(),
                    toAccount = aToAccount(),
                    from = value(
                        outOfDate(10.ether()),
                        outOfDate(100.gbp())
                    ),
                    to = value(
                        outOfDate(25.bitcoinCash()),
                        userEntered(456.78.gbp())
                    )
                )
            )
        }
    }

    private fun aToAccount() =
        AccountReference.BitcoinLike(CryptoCurrency.BCH, "BCH Account", "xbub123")

    private fun aFromAccount() =
        AccountReference.Ethereum("Ether Account", "0xeth1")
}

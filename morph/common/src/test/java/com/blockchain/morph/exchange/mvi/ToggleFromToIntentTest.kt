package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.gbp
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import org.junit.Test

class ToggleFromToIntentTest {

    @Test
    fun `can toggle between "from" and "to" entry with crypto`() {
        given(
            ExchangeViewModel(
                fromAccount = aFromAccount(),
                toAccount = aToAccount(),
                from = value(
                    userEntered(CryptoValue.etherFromMajor(10)),
                    upToDate(100.gbp())
                ),
                to = value(
                    upToDate(CryptoValue.bitcoinCashFromMajor(25)),
                    upToDate(99.gbp())
                )
            )
        ).on(
            ToggleFromToIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    fromAccount = aFromAccount(),
                    toAccount = aToAccount(),
                    from = value(
                        upToDate(CryptoValue.etherFromMajor(10)),
                        upToDate(100.gbp())
                    ),
                    to = value(
                        userEntered(CryptoValue.bitcoinCashFromMajor(25)),
                        upToDate(99.gbp())
                    )
                )
            )
        }
    }

    @Test
    fun `can toggle between "from" and "to" entry with fiat`() {
        given(
            ExchangeViewModel(
                fromAccount = aFromAccount(),
                toAccount = aToAccount(),
                from = value(
                    upToDate(CryptoValue.etherFromMajor(10)),
                    upToDate(100.gbp())
                ),
                to = value(
                    upToDate(CryptoValue.bitcoinCashFromMajor(25)),
                    userEntered(99.gbp())
                )
            )
        ).on(
            ToggleFromToIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    fromAccount = aFromAccount(),
                    toAccount = aToAccount(),
                    from = value(
                        upToDate(CryptoValue.etherFromMajor(10)),
                        userEntered(100.gbp())
                    ),
                    to = value(
                        upToDate(CryptoValue.bitcoinCashFromMajor(25)),
                        upToDate(99.gbp())
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

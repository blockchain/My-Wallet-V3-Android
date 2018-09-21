package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.gbp
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import org.junit.Test

class SwapIntentTest {

    @Test
    fun `can swap the "to" and "from" currencies`() {
        given(
            ExchangeViewModel(
                fromAccount = AccountReference.Ethereum("Ether Account", "0xeth1"),
                toAccount = AccountReference.BitcoinLike(CryptoCurrency.BCH, "BCH Account", "xbub123"),
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
            SwapIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    fromAccount = AccountReference.BitcoinLike(CryptoCurrency.BCH, "BCH Account", "xbub123"),
                    toAccount = AccountReference.Ethereum("Ether Account", "0xeth1"),
                    from = value(
                        userEntered(CryptoValue.ZeroBch),
                        outOfDate(zeroFiat("GBP"))
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
package com.blockchain.morph.exchange.mvi

import com.blockchain.testutils.gbp
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import org.junit.Test

class ToggleFiatCryptoIntentTest {

    @Test
    fun `can toggle between fiat and crypto entry on the "from" side`() {
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
            ToggleFiatCryptoIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    fromAccount = AccountReference.Ethereum("Ether Account", "0xeth1"),
                    toAccount = AccountReference.BitcoinLike(CryptoCurrency.BCH, "BCH Account", "xbub123"),
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

    @Test
    fun `can toggle between fiat and crypto entry on the "to" side`() {
        given(
            ExchangeViewModel(
                fromAccount = AccountReference.Ethereum("Ether Account", "0xeth1"),
                toAccount = AccountReference.BitcoinLike(CryptoCurrency.BCH, "BCH Account", "xbub123"),
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
            ToggleFiatCryptoIntent()
        ) {
            assertValue(
                ExchangeViewModel(
                    fromAccount = AccountReference.Ethereum("Ether Account", "0xeth1"),
                    toAccount = AccountReference.BitcoinLike(CryptoCurrency.BCH, "BCH Account", "xbub123"),
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
}

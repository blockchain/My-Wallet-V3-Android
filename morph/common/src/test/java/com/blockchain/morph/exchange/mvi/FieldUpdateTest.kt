package com.blockchain.morph.exchange.mvi

import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import io.reactivex.subjects.PublishSubject
import org.junit.Test

class FieldUpdateTest {

    @Test
    fun `initial state`() {
        val subject = PublishSubject.create<ExchangeIntent>()
        ExchangeDialog(subject, initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER))
            .viewModel
            .test()
            .assertValue(
                ExchangeViewModel(
                    from = value(
                        upToDate(CryptoValue.ZeroBtc),
                        upToDate(zeroFiat("USD"))
                    ),
                    to = value(
                        upToDate(CryptoValue.ZeroEth),
                        upToDate(zeroFiat("USD"))
                    )
                )
            )
    }

    @Test
    fun `update "from crypto"`() {
        given(
            initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        )
            .on(
                FieldUpdateIntent(
                    FieldUpdateIntent.Field.FROM_CRYPTO,
                    "123.45"
                )
            ) {
                assertValue(
                    ExchangeViewModel(
                        from = value(
                            userEntered(CryptoValue.fromMajor(CryptoCurrency.BTC, 123.45.toBigDecimal())),
                            outOfDate(zeroFiat("USD"))
                        ),
                        to = value(
                            outOfDate(CryptoValue.ZeroEth),
                            outOfDate(zeroFiat("USD"))
                        )
                    )
                )
            }
    }

    @Test
    fun `update "to crypto"`() {
        given(
            initial("GBP", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        )
            .on(
                FieldUpdateIntent(
                    FieldUpdateIntent.Field.TO_CRYPTO,
                    "99.12"
                )
            ) {
                assertValue(
                    ExchangeViewModel(
                        from = value(
                            outOfDate(CryptoValue.ZeroBtc),
                            outOfDate(zeroFiat("GBP"))
                        ),
                        to = value(
                            userEntered(CryptoValue.fromMajor(CryptoCurrency.ETHER, 99.12.toBigDecimal())),
                            outOfDate(zeroFiat("GBP"))
                        )
                    )
                )
            }
    }

    @Test
    fun `update "from fiat"`() {
        given(
            initial("USD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        )
            .on(
                FieldUpdateIntent(
                    FieldUpdateIntent.Field.FROM_FIAT,
                    "123.45"
                )
            ) {
                assertValue(
                    ExchangeViewModel(
                        from = value(
                            outOfDate(CryptoValue.ZeroBtc),
                            userEntered(FiatValue("USD", 123.45.toBigDecimal()))
                        ),
                        to = value(
                            outOfDate(CryptoValue.ZeroEth),
                            outOfDate(zeroFiat("USD"))
                        )
                    )
                )
            }
    }

    @Test
    fun `update "to fiat"`() {
        given(
            initial("CAD", CryptoCurrency.BTC to CryptoCurrency.ETHER)
        )
            .on(
                FieldUpdateIntent(
                    FieldUpdateIntent.Field.TO_FIAT,
                    "45.67"
                )
            ) {
                assertValue(
                    ExchangeViewModel(
                        from = value(
                            outOfDate(CryptoValue.ZeroBtc),
                            outOfDate(zeroFiat("CAD"))
                        ),
                        to = value(
                            outOfDate(CryptoValue.ZeroEth),
                            userEntered(FiatValue("CAD", 45.67.toBigDecimal()))
                        )
                    )
                )
            }
    }
}

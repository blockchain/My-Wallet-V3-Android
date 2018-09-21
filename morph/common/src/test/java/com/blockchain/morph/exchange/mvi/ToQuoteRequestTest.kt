package com.blockchain.morph.exchange.mvi

import com.blockchain.morph.CoinPair
import com.blockchain.morph.quote.ExchangeQuoteRequest
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.bitcoinCash
import com.blockchain.testutils.gbp
import com.blockchain.testutils.usd
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Test

class ToQuoteRequestTest {

    @Test
    fun `from crypto`() {
        Observable.just(
            FieldUpdateIntent(
                Fix.BASE_CRYPTO,
                "10"
            )
        )
            .latestPair(
                Observable.just(
                    Params(
                        from = CryptoCurrency.BTC,
                        to = CryptoCurrency.ETHER,
                        fiat = "USD"
                    )
                )
            )
            .toQuoteRequest()
            .test()
            .values().single().apply {
                this `should be instance of` ExchangeQuoteRequest.Selling::class
                (this as ExchangeQuoteRequest.Selling).apply {
                    this.offering `should equal` 10.0.bitcoin()
                    this.wanted `should be` CryptoCurrency.ETHER
                    this.pair `should be` CoinPair.BTC_TO_ETH
                }
            }
    }

    @Test
    fun `to crypto`() {
        Observable.just(
            FieldUpdateIntent(
                Fix.COUNTER_CRYPTO,
                "25.5"
            )
        )
            .latestPair(
                Observable.just(
                    Params(
                        from = CryptoCurrency.BTC,
                        to = CryptoCurrency.BCH,
                        fiat = "USD"
                    )
                )
            )
            .toQuoteRequest()
            .test()
            .values().single().apply {
                this `should be instance of` ExchangeQuoteRequest.Buying::class
                (this as ExchangeQuoteRequest.Buying).apply {
                    this.offering `should be` CryptoCurrency.BTC
                    this.wanted `should equal` 25.5.bitcoinCash()
                    this.pair `should be` CoinPair.BTC_TO_BCH
                }
            }
    }

    @Test
    fun `from fiat`() {
        Observable.just(
            FieldUpdateIntent(
                Fix.BASE_FIAT,
                "30.99"
            )
        )
            .latestPair(
                Observable.just(
                    Params(
                        from = CryptoCurrency.ETHER,
                        to = CryptoCurrency.BTC,
                        fiat = "USD"
                    )
                )
            )
            .toQuoteRequest()
            .test()
            .values().single().apply {
                this `should be instance of` ExchangeQuoteRequest.SellingFiatLinked::class
                (this as ExchangeQuoteRequest.SellingFiatLinked).apply {
                    this.offering `should be` CryptoCurrency.ETHER
                    this.wanted `should be` CryptoCurrency.BTC
                    this.offeringFiatValue `should equal` 30.99.usd()
                    this.pair `should be` CoinPair.ETH_TO_BTC
                }
            }
    }

    @Test
    fun `to fiat`() {
        Observable.just(
            FieldUpdateIntent(
                Fix.COUNTER_FIAT,
                "12.34"
            )
        )
            .latestPair(
                Observable.just(
                    Params(
                        from = CryptoCurrency.ETHER,
                        to = CryptoCurrency.BTC,
                        fiat = "GBP"
                    )
                )
            )
            .toQuoteRequest()
            .test()
            .values().single().apply {
                this `should be instance of` ExchangeQuoteRequest.BuyingFiatLinked::class
                (this as ExchangeQuoteRequest.BuyingFiatLinked).apply {
                    this.offering `should be` CryptoCurrency.ETHER
                    this.wanted `should be` CryptoCurrency.BTC
                    this.wantedFiatValue `should equal` 12.34.gbp()
                    this.pair `should be` CoinPair.ETH_TO_BTC
                }
            }
    }
}
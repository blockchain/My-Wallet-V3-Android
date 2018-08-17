package com.blockchain.morph.homebrew

import com.blockchain.morph.quote.ExchangeQuoteRequest
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.testutils.usd
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import org.amshove.kluent.`should equal`
import org.junit.Test

class QuoteWebSocketParamsTest {

    @Test
    fun `Selling crypto`() {
        ExchangeQuoteRequest.Selling(
            offering = 2.0.bitcoin(),
            wanted = CryptoCurrency.ETHER
        ).mapToSocketParameters() `should equal`
            QuoteWebSocketParams(
                pair = "BTC-ETH",
                volume = "2.0",
                volumeCurrency = "BTC",
                isBase = null
            )
    }

    @Test
    fun `Buying crypto`() {
        ExchangeQuoteRequest.Buying(
            offering = CryptoCurrency.BCH,
            wanted = 1.0.bitcoin()
        ).mapToSocketParameters() `should equal`
            QuoteWebSocketParams(
                pair = "BTC-BCH",
                volume = "1.0",
                volumeCurrency = "BTC",
                isBase = null
            )
    }

    @Test
    fun `Selling crypto - fiat linked`() {
        ExchangeQuoteRequest.SellingFiatLinked(
            offering = CryptoCurrency.BTC,
            wanted = CryptoCurrency.ETHER,
            offeringFiatValue = 12.34.usd()
        ).mapToSocketParameters() `should equal`
            QuoteWebSocketParams(
                pair = "BTC-ETH",
                volume = "12.34",
                volumeCurrency = "USD",
                isBase = true
            )
    }

    @Test
    fun `Buying crypto - fiat linked`() {
        ExchangeQuoteRequest.BuyingFiatLinked(
            offering = CryptoCurrency.ETHER,
            wanted = CryptoCurrency.BCH,
            wantedFiatValue = 45.67.usd()
        ).mapToSocketParameters() `should equal`
            QuoteWebSocketParams(
                pair = "ETH-BCH",
                volume = "45.67",
                volumeCurrency = "USD",
                isBase = false
            )
    }

    @Test
    fun `map several`() {
        Observable.just<ExchangeQuoteRequest>(
            ExchangeQuoteRequest.Selling(
                offering = 2.0.bitcoin(),
                wanted = CryptoCurrency.ETHER
            ),
            ExchangeQuoteRequest.Buying(
                wanted = 4.0.ether(),
                offering = CryptoCurrency.BCH
            )
        ).mapToSocketParameters()
            .test()
            .values() `should equal`
            listOf(
                QuoteWebSocketParams("BTC-ETH", "2.0", "BTC", null),
                QuoteWebSocketParams("ETH-BCH", "4.0", "ETH", null)
            )
    }
}

package com.blockchain.morph.homebrew

import com.blockchain.morph.quote.ExchangeQuoteRequest
import com.blockchain.testutils.after
import com.blockchain.testutils.before
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.testutils.usd
import info.blockchain.balance.CryptoCurrency
import io.reactivex.Observable
import org.amshove.kluent.`should equal`
import org.junit.Rule
import org.junit.Test
import java.util.Locale

class QuoteWebSocketParamsTest {

    @get:Rule
    val locale = before {
        Locale.setDefault(Locale.FRANCE)
    } after {
        Locale.setDefault(Locale.US)
    }

    @Test
    fun `Selling crypto`() {
        ExchangeQuoteRequest.Selling(
            offering = 2345.45679.bitcoin(),
            wanted = CryptoCurrency.ETHER,
            indicativeFiatSymbol = "USD"
        ).mapToSocketParameters() `should equal`
            QuoteWebSocketParams(
                pair = "BTC-ETH",
                volume = "2345.45679",
                fiatCurrency = "USD",
                fix = "base"
            )
    }

    @Test
    fun `Buying crypto`() {
        ExchangeQuoteRequest.Buying(
            offering = CryptoCurrency.BCH,
            wanted = 4567.5649.bitcoin(),
            indicativeFiatSymbol = "GBP"
        ).mapToSocketParameters() `should equal`
            QuoteWebSocketParams(
                pair = "BCH-BTC",
                volume = "4567.5649",
                fiatCurrency = "GBP",
                fix = "counter"
            )
    }

    @Test
    fun `Selling crypto - fiat linked`() {
        ExchangeQuoteRequest.SellingFiatLinked(
            offering = CryptoCurrency.BTC,
            wanted = CryptoCurrency.ETHER,
            offeringFiatValue = 5612.34.usd()
        ).mapToSocketParameters() `should equal`
            QuoteWebSocketParams(
                pair = "BTC-ETH",
                volume = "5612.34",
                fiatCurrency = "USD",
                fix = "baseInFiat"
            )
    }

    @Test
    fun `Buying crypto - fiat linked`() {
        ExchangeQuoteRequest.BuyingFiatLinked(
            offering = CryptoCurrency.ETHER,
            wanted = CryptoCurrency.BCH,
            wantedFiatValue = 2345.67.usd()
        ).mapToSocketParameters() `should equal`
            QuoteWebSocketParams(
                pair = "ETH-BCH",
                volume = "2345.67",
                fiatCurrency = "USD",
                fix = "counterInFiat"
            )
    }

    @Test
    fun `map several`() {
        Observable.just<ExchangeQuoteRequest>(
            ExchangeQuoteRequest.Selling(
                offering = 2.0.bitcoin(),
                wanted = CryptoCurrency.ETHER,
                indicativeFiatSymbol = "GBP"
            ),
            ExchangeQuoteRequest.Buying(
                wanted = 4.0.ether(),
                offering = CryptoCurrency.BCH,
                indicativeFiatSymbol = "YEN"
            )
        ).mapToSocketParameters()
            .test()
            .values() `should equal`
            listOf(
                QuoteWebSocketParams(
                    pair = "BTC-ETH",
                    volume = "2.0",
                    fiatCurrency = "GBP",
                    fix = "base"
                ),
                QuoteWebSocketParams(
                    pair = "BCH-ETH",
                    volume = "4.0",
                    fiatCurrency = "YEN",
                    fix = "counter"
                )
            )
    }
}

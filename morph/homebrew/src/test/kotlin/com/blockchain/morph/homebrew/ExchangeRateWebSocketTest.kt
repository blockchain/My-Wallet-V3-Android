package com.blockchain.morph.homebrew

import com.blockchain.koin.modules.homeBrewModule
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.quote.ExchangeQuoteRequest
import com.blockchain.network.modules.MoshiBuilderInterceptorList
import com.blockchain.network.modules.apiModule
import com.blockchain.network.websocket.WebSocket
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.blockchain.testutils.getStringFromResource
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.verifyNoMoreInteractions
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.ExchangeRate
import io.reactivex.subjects.PublishSubject
import org.amshove.kluent.`it returns`
import org.amshove.kluent.`should be instance of`
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Test
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext
import org.koin.standalone.get
import org.koin.test.AutoCloseKoinTest

class ExchangeRateWebSocketTest : AutoCloseKoinTest() {

    @Before
    fun startKoin() {
        StandAloneContext.startKoin(
            listOf(
                homeBrewModule,
                apiModule,
                applicationContext {
                    bean {
                        MoshiBuilderInterceptorList(
                            listOf(
                                get("BigDecimal")
                            )
                        )
                    }
                }
            )
        )
    }

    @Test
    fun `sends a request down the socket`() {
        val actualSocket =
            mock<WebSocket<String, String>>()

        givenAWebSocket(actualSocket)
            .updateQuoteRequest(
                ExchangeQuoteRequest.Selling(
                    offering = 100.bitcoin(),
                    wanted = CryptoCurrency.ETHER,
                    indicativeFiatSymbol = "USD"
                )
            )

        verify(actualSocket).send(
            "{\"channel\":\"exchange_rate\",\"operation\":\"subscribe\"," +
                "\"params\":{\"pairs\":[\"BTC-USD\"],\"type\":\"exchangeRates\"}}"
        )

        verifyNoMoreInteractions(actualSocket)
    }

    @Test
    fun `when you subscribe to the same parameters, just one request goes down socket`() {
        val actualSocket =
            mock<WebSocket<String, String>>()

        givenAWebSocket(actualSocket)
            .apply {
                updateQuoteRequest(
                    ExchangeQuoteRequest.Selling(
                        offering = 100.bitcoin(),
                        wanted = CryptoCurrency.ETHER,
                        indicativeFiatSymbol = "USD"
                    )
                )
                updateQuoteRequest(
                    ExchangeQuoteRequest.Selling(
                        offering = 100.bitcoin(),
                        wanted = CryptoCurrency.ETHER,
                        indicativeFiatSymbol = "USD"
                    )
                )
            }

        verify(actualSocket).send(any())
        verifyNoMoreInteractions(actualSocket)
    }

    @Test
    fun `when you change the subscription, an unsubscribe doesn't happen`() {
        val actualSocket =
            mock<WebSocket<String, String>>()

        givenAWebSocket(actualSocket)
            .apply {
                updateQuoteRequest(
                    ExchangeQuoteRequest.Selling(
                        offering = 200.bitcoin(),
                        wanted = CryptoCurrency.ETHER,
                        indicativeFiatSymbol = "USD"
                    )
                )
                updateQuoteRequest(
                    ExchangeQuoteRequest.Selling(
                        offering = 300.ether(),
                        wanted = CryptoCurrency.BCH,
                        indicativeFiatSymbol = "CAD"
                    )
                )
            }

        verify(actualSocket).send(
            "{\"channel\":\"exchange_rate\",\"operation\":\"subscribe\"," +
                "\"params\":{\"pairs\":[\"BTC-USD\"],\"type\":\"exchangeRates\"}}"
        )

        verify(actualSocket).send(
            "{\"channel\":\"exchange_rate\",\"operation\":\"subscribe\"," +
                "\"params\":{\"pairs\":[\"ETH-CAD\"],\"type\":\"exchangeRates\"}}"
        )
    }

    @Test
    fun `when the socket responds with a message it is converted from json to incoming type`() {
        val subject = PublishSubject.create<String>()
        val actualSocket =
            mock<WebSocket<String, String>> {
                on { responses } `it returns` subject
            }

        val test = givenAWebSocket(actualSocket)
            .rates
            .test()

        subject.onNext(
            """
                {
                  "sequenceNumber":2,"channel":"exchange_rate",
                  "type":"exchangeRate",
                  "rates":[{"pair":"GBP-ETH","price":"2018.41"}]
                }
            """.trimMargin()
        )

        test.values().single().apply {
            this `should be instance of` ExchangeRate.CryptoToFiat::class
            val c2f = this as ExchangeRate.CryptoToFiat
            c2f.from `should be` CryptoCurrency.ETHER
            c2f.to `should equal` "GBP"
            c2f.rate `should equal` "2018.41".toBigDecimal()
        }
    }

    @Test
    fun `when the socket responds with a subscribed message, don't pass it on`() {
        val subject = PublishSubject.create<String>()
        val actualSocket =
            mock<WebSocket<String, String>> {
                on { responses } `it returns` subject
            }

        val test = givenAWebSocket(actualSocket)
            .rates
            .test()

        subject.onNext(getStringFromResource("quotes/exchange_subscription_confirmation.json"))

        test.values() `should equal` emptyList()
    }

    private fun givenAWebSocket(actualSocket: WebSocket<String, String>): QuoteService =
        QuoteWebSocket(actualSocket, get(), quoteWebSocketStream = mock())
}

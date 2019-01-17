package com.blockchain.nabu.service

import com.blockchain.koin.nabuModule
import com.blockchain.morph.CoinPair
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.api.CryptoAndFiat
import com.blockchain.nabu.api.CurrencyRatio
import com.blockchain.nabu.api.PeriodicLimit
import com.blockchain.nabu.api.QuoteJson
import com.blockchain.nabu.api.TradeRequest
import com.blockchain.nabu.api.TradesLimits
import com.blockchain.nabu.api.TradingConfig
import com.blockchain.nabu.api.TransactionState
import com.blockchain.nabu.api.Value
import com.blockchain.network.initRule
import com.blockchain.network.modules.apiModule
import com.blockchain.serialization.JsonSerializable
import com.blockchain.testutils.`should be assignable from`
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.cad
import com.blockchain.testutils.ether
import com.blockchain.testutils.gbp
import com.blockchain.testutils.lumens
import com.blockchain.testutils.usd
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import io.fabric8.mockwebserver.DefaultMockServer
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.get
import org.koin.standalone.inject
import org.koin.test.AutoCloseKoinTest
import java.math.BigDecimal

class NabuMarketsServiceTest : AutoCloseKoinTest() {

    private val server = DefaultMockServer()

    @get:Rule
    val initMockServer = server.initRule()

    private val subject: NabuMarketsService by inject()

    @Before
    fun startKoin() {
        startKoin(
            listOf(
                apiModule,
                nabuModule,
                apiServerTestModule(server)
            )
        )
    }

    @Test
    fun `can get min order size from json`() {
        server.expect().get().withPath("/nabu-gateway/markets/quotes/BTC-ETH/config")
            .andReturn(
                200,
                """
{
  "pair": "BTC-ETH",
  "orderIncrement": "0.3",
  "minOrderSize": "0.1",
  "updated": "2018-08-28T15:42:04.490Z"
}
"""
            )
            .once()

        subject.getTradingConfig(CoinPair.BTC_TO_ETH)
            .test()
            .values()
            .single()
            .apply {
                minOrderSize `should equal` 0.1.bitcoin()
            }
    }

    @Test
    fun `can get min order size, alternative currency`() {
        server.expect().get().withPath("/nabu-gateway/markets/quotes/ETH-BCH/config")
            .andReturn(
                200,
                TradingConfig(minOrderSize = 1.4.toBigDecimal())
            )
            .once()

        subject.getTradingConfig(CoinPair.ETH_TO_BCH)
            .test()
            .values()
            .single()
            .apply {
                minOrderSize `should equal` 1.4.ether()
            }
    }

    @Test
    fun `mock server lacks ways to ensure headers were set, so at least verify authenticate was called`() {
        subject.getTradingConfig(CoinPair.ETH_TO_BCH)
            .test()
        verify(get<Authenticator>())
            .authenticate<com.blockchain.nabu.service.TradingConfig>(any())
    }

    @Test
    fun `ensure type is JsonSerializable for proguard`() {
        JsonSerializable::class `should be assignable from` TradingConfig::class
    }

    @Test
    fun `ensure trades limits is JsonSerializable for proguard`() {
        JsonSerializable::class `should be assignable from` TradesLimits::class
        JsonSerializable::class `should be assignable from` PeriodicLimit::class
    }

    @Test
    fun `can get trade limits from json`() {
        server.expect().get().withPath("/nabu-gateway/trades/limits?currency=CAD")
            .andReturn(
                200,
                """
{
    "currency": "USD",
    "minOrder": "10.0",
    "maxOrder": "1000.0",
    "maxPossibleOrder": "100.0",
    "daily": {
        "limit": "5000.0",
        "available": "100.1",
        "used": "4900.0"
    },
    "weekly": {
        "limit": "10000.0",
        "available": "5000.1",
        "used": "5000.2"
    },
    "annual": {
        "limit": "50000.0",
        "available": "40000.0",
        "used": "10000.1"
    }
}
"""
            )
            .once()

        subject.getTradesLimits("CAD")
            .test()
            .values()
            .single()
            .apply {
                minOrder `should equal` 10.usd()
                maxOrder `should equal` 1000.usd()
                maxPossibleOrder `should equal` 100.usd()

                daily.limit `should equal` 5000.usd()
                daily.available `should equal` 100.1.usd()
                daily.used `should equal` 4900.usd()

                weekly.limit `should equal` 10000.usd()
                weekly.available `should equal` 5000.1.usd()
                weekly.used `should equal` 5000.2.usd()

                annual.limit `should equal` 50000.usd()
                annual.available `should equal` 40000.usd()
                annual.used `should equal` 10000.1.usd()
            }
    }

    @Test
    fun `can get trade limits from json - nulls`() {
        server.expect().get().withPath("/nabu-gateway/trades/limits?currency=CAD")
            .andReturn(
                200,
                """
{
    "currency": "USD",
    "minOrder": "10.0",
    "maxOrder": "1000.0",
    "maxPossibleOrder": "100.0",
    "daily": {
        "limit": null,
        "available": null,
        "used": "4900.0"
    },
    "weekly": {
        "limit": "10000.0",
        "available": "5000.1",
        "used": null
    },
    "annual": null
}
"""
            )
            .once()

        subject.getTradesLimits("CAD")
            .test()
            .values()
            .single()
            .apply {
                minOrder `should equal` 10.usd()
                maxOrder `should equal` 1000.usd()
                maxPossibleOrder `should equal` 100.usd()

                daily.limit `should be` null
                daily.available `should be` null
                daily.used `should equal` 4900.usd()

                weekly.limit `should equal` 10000.usd()
                weekly.available `should equal` 5000.1.usd()
                weekly.used `should be` null

                annual.limit `should be` null
                annual.available `should be` null
                annual.used `should be` null
            }
    }

    @Test
    fun `can get trade limits alternative values`() {
        server.expect().get().withPath("/nabu-gateway/trades/limits?currency=CAD")
            .andReturn(
                200,
                TradesLimits(
                    currency = "CAD",
                    minOrder = 1.toBigDecimal(),
                    maxOrder = 2.toBigDecimal(),
                    maxPossibleOrder = 3.toBigDecimal(),
                    daily = PeriodicLimit(
                        limit = 4.toBigDecimal(),
                        available = 5.toBigDecimal(),
                        used = 6.toBigDecimal()
                    ),
                    weekly = PeriodicLimit(
                        limit = 7.toBigDecimal(),
                        available = 8.toBigDecimal(),
                        used = 9.toBigDecimal()
                    ),
                    annual = PeriodicLimit(
                        limit = 10.toBigDecimal(),
                        available = 11.toBigDecimal(),
                        used = 12.toBigDecimal()
                    )
                )
            )
            .once()

        subject.getTradesLimits("CAD")
            .test()
            .values()
            .single()
            .apply {
                minOrder `should equal` 1.cad()
                maxOrder `should equal` 2.cad()
                maxPossibleOrder `should equal` 3.cad()

                daily.limit `should equal` 4.cad()
                daily.available `should equal` 5.cad()
                daily.used `should equal` 6.cad()

                weekly.limit `should equal` 7.cad()
                weekly.available `should equal` 8.cad()
                weekly.used `should equal` 9.cad()

                annual.limit `should equal` 10.cad()
                annual.available `should equal` 11.cad()
                annual.used `should equal` 12.cad()
            }
    }

    @Test
    fun `can execute trade from json`() {
        server.expect().post().withPath("/nabu-gateway/trades")
            .andReturn(
                200,
                """
{
  "id": "039267ab-de16-4093-8cdf-a7ea1c732dbd",
  "state": "FINISHED",
  "createdAt": "2018-09-19T12:20:42.894Z",
  "updatedAt": "2018-09-19T12:24:18.943Z",
  "pair": "ETH-BTC",
  "refundAddress": "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
  "rate": "0.1",
  "depositAddress": "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
  "deposit": {
    "symbol": "ETH",
    "value": "100.0"
  },
  "withdrawalAddress": "3H4w1Sqk8UNNEfZoa9Z8FZJ6RYHrxLmzGU",
  "withdrawal": {
    "symbol": "BTC",
    "value": "10.0"
  },
  "withdrawalFee": {
    "symbol": "BTC",
    "value": "0.0000001"
  },
  "fiatValue": {
    "symbol": "GBP",
    "value": "10.0"
  }
}
"""
            )
            .once()

        subject.executeTrade(emptyTradeRequest)
            .test()
            .values()
            .single()
            .apply {
                id `should equal` "039267ab-de16-4093-8cdf-a7ea1c732dbd"
                createdAt `should equal` "2018-09-19T12:20:42.894Z"
                pair `should equal` CoinPair.ETH_TO_BTC
                refundAddress `should equal` "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb"
                depositAddress `should equal` "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"
                deposit `should equal` 100.0.ether()
                withdrawalAddress `should equal` "3H4w1Sqk8UNNEfZoa9Z8FZJ6RYHrxLmzGU"
                withdrawal `should equal` 10.0.bitcoin()
                state `should equal` TransactionState.Finished
                fee `should equal` 0.0000001.bitcoin()
                fiatValue `should equal` 10.0.gbp()
                depositTextMemo `should be` null
            }
    }

    @Test
    fun `can execute trade from json - xlm - memo response`() {
        server.expect().post().withPath("/nabu-gateway/trades")
            .andReturn(
                200,
                """
{
  "id": "039267ab-de16-4093-8cdf-a7ea1c732dbd",
  "state": "FINISHED",
  "createdAt": "2018-09-19T12:20:42.894Z",
  "updatedAt": "2018-09-19T12:24:18.943Z",
  "pair": "ETH-XLM",
  "refundAddress": "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
  "rate": "0.1",
  "depositAddress": "GBHVJOTGY2723JY2OJI5Q6LG4J3QQU2FCOLA7BKMUSHMR5343ZTZ6BNV",
  "depositMemo": "reference for the hotwallet",
  "deposit": {
    "symbol": "ETH",
    "value": "100.0"
  },
  "withdrawalAddress": "3H4w1Sqk8UNNEfZoa9Z8FZJ6RYHrxLmzGU",
  "withdrawal": {
    "symbol": "XLM",
    "value": "10.0"
  },
  "withdrawalFee": {
    "symbol": "XLM",
    "value": "0.0000001"
  },
  "fiatValue": {
    "symbol": "GBP",
    "value": "10.0"
  }
}
"""
            )
            .once()

        subject.executeTrade(emptyTradeRequest)
            .test()
            .values()
            .single()
            .apply {
                id `should equal` "039267ab-de16-4093-8cdf-a7ea1c732dbd"
                createdAt `should equal` "2018-09-19T12:20:42.894Z"
                pair `should equal` CoinPair.ETH_TO_XLM
                refundAddress `should equal` "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb"
                depositAddress `should equal` "GBHVJOTGY2723JY2OJI5Q6LG4J3QQU2FCOLA7BKMUSHMR5343ZTZ6BNV"
                deposit `should equal` 100.0.ether()
                withdrawalAddress `should equal` "3H4w1Sqk8UNNEfZoa9Z8FZJ6RYHrxLmzGU"
                withdrawal `should equal` 10.0.lumens()
                state `should equal` TransactionState.Finished
                fee `should equal` 0.0000001.lumens()
                fiatValue `should equal` 10.0.gbp()
                depositTextMemo `should equal` "reference for the hotwallet"
            }
    }

    @Test
    fun `can get list of trades`() {
        server.expect().get().withPath("/nabu-gateway/trades?userFiatCurrency=GBP")
            .andReturn(
                200,
                """
[
    {
      "id": "039267ab-de16-4093-8cdf-a7ea1c732dbd",
      "state": "FINISHED",
      "createdAt": "2018-09-19T12:20:42.894Z",
      "updatedAt": "2018-09-19T12:24:18.943Z",
      "pair": "ETH-BTC",
      "refundAddress": "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
      "rate": "0.1",
      "depositAddress": "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
      "deposit": {
        "symbol": "ETH",
        "value": "100.0"
      },
      "withdrawalAddress": "3H4w1Sqk8UNNEfZoa9Z8FZJ6RYHrxLmzGU",
      "withdrawal": {
        "symbol": "BTC",
        "value": "10.0"
      },
      "withdrawalFee": {
        "symbol": "BTC",
        "value": "0.0000001"
      },
      "fiatValue": {
        "symbol": "GBP",
        "value": "10.0"
      },
      "depositTxHash": "e6a5cfee8063330577babb6fb92eabccf5c3c1aeea120c550b6779a6c657dfce",
      "withdrawalTxHash": "0xf902adc8862c6c6ad2cd06f12d952e95c50ad783bae50ef952e1f54b7762a50e"
    }
]
"""
            )
            .once()

        subject.getTrades("GBP")
            .test()
            .values()
            .asSequence()
            .single()
            .first()
            .apply {
                id `should equal` "039267ab-de16-4093-8cdf-a7ea1c732dbd"
                createdAt `should equal` "2018-09-19T12:20:42.894Z"
                pair `should equal` CoinPair.ETH_TO_BTC
                refundAddress `should equal` "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb"
                depositAddress `should equal` "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"
                deposit `should equal` 100.0.ether()
                withdrawalAddress `should equal` "3H4w1Sqk8UNNEfZoa9Z8FZJ6RYHrxLmzGU"
                withdrawal `should equal` 10.0.bitcoin()
                state `should equal` TransactionState.Finished
                fee `should equal` 0.0000001.bitcoin()
                fiatValue `should equal` 10.0.gbp()
            }
    }

    @Test
    fun `can get list of trades with minimal json`() {
        server.expect().get().withPath("/nabu-gateway/trades?userFiatCurrency=GBP")
            .andReturn(
                200,
                """
[
    {
      "id": "039267ab-de16-4093-8cdf-a7ea1c732dbd",
      "state": "FINISHED",
      "createdAt": "2018-09-19T12:20:42.894Z",
      "updatedAt": "2018-09-19T12:24:18.943Z",
      "pair": "ETH-BTC",
      "refundAddress": "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb",
      "depositAddress": "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359",
      "withdrawalAddress": "3H4w1Sqk8UNNEfZoa9Z8FZJ6RYHrxLmzGU",
      "withdrawalFee": {
        "symbol": "BTC",
        "value": "0.0000001"
      },
      "fiatValue": {
        "symbol": "GBP",
        "value": "10.0"
      }
    }
]
"""
            )
            .once()

        subject.getTrades("GBP")
            .test()
            .values()
            .asSequence()
            .single()
            .first()
            .apply {
                id `should equal` "039267ab-de16-4093-8cdf-a7ea1c732dbd"
                createdAt `should equal` "2018-09-19T12:20:42.894Z"
                pair `should equal` CoinPair.ETH_TO_BTC
                refundAddress `should equal` "0xD1220A0cf47c7B9Be7A2E6BA89F429762e7b9aDb"
                depositAddress `should equal` "0xfB6916095ca1df60bB79Ce92cE3Ea74c37c5d359"
                deposit `should equal` 0.ether()
                withdrawalAddress `should equal` "3H4w1Sqk8UNNEfZoa9Z8FZJ6RYHrxLmzGU"
                withdrawal `should equal` 0.bitcoin()
                state `should equal` TransactionState.Finished
                fee `should equal` 0.0000001.bitcoin()
                fiatValue `should equal` 10.0.gbp()
            }
    }

    private val emptyTradeRequest = TradeRequest(
        destinationAddress = "",
        refundAddress = "",
        quote = QuoteJson(
            pair = "",
            fiatCurrency = "",
            fix = "",
            volume = BigDecimal.ZERO,
            currencyRatio = CurrencyRatio(
                base = CryptoAndFiat(Value("", BigDecimal.ZERO), Value("", BigDecimal.ZERO)),
                counter = CryptoAndFiat(Value("", BigDecimal.ZERO), Value("", BigDecimal.ZERO)),
                baseToFiatRate = BigDecimal.ZERO,
                baseToCounterRate = BigDecimal.ZERO,
                counterToBaseRate = BigDecimal.ZERO,
                counterToFiatRate = BigDecimal.ZERO
            )
        )
    )
}

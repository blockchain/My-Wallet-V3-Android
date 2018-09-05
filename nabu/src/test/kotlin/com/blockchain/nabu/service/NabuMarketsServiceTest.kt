package com.blockchain.nabu.service

import com.blockchain.koin.nabuModule
import com.blockchain.morph.CoinPair
import com.blockchain.nabu.Authenticator
import com.blockchain.nabu.api.TradingConfig
import com.blockchain.network.initRule
import com.blockchain.network.modules.apiModule
import com.blockchain.serialization.JsonSerializable
import com.blockchain.testutils.`should be assignable from`
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.verify
import io.fabric8.mockwebserver.DefaultMockServer
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.get
import org.koin.standalone.inject
import org.koin.test.AutoCloseKoinTest

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
                TradingConfig(minOrderSize = "1.4")
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
}

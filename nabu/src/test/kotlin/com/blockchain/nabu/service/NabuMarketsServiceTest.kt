package com.blockchain.nabu.service

import com.blockchain.koin.nabuModule
import com.blockchain.morph.CoinPair
import com.blockchain.nabu.api.TradingConfig
import com.blockchain.network.mockWebServerInit
import com.blockchain.network.modules.apiModule
import com.blockchain.testutils.bitcoin
import com.blockchain.testutils.ether
import io.fabric8.mockwebserver.DefaultMockServer
import org.amshove.kluent.`should equal`
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.koin.standalone.StandAloneContext.startKoin
import org.koin.standalone.inject
import org.koin.test.AutoCloseKoinTest

class NabuMarketsServiceTest : AutoCloseKoinTest() {

    private val server = DefaultMockServer()

    @get:Rule
    val initMockServer = mockWebServerInit(server)

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
        server.expect().get().withPath("/nabu-app/markets/quotes/BTC-ETH/config")
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
        server.expect().get().withPath("/nabu-app/markets/quotes/ETH-BCH/config")
            .andReturn(200,
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
}

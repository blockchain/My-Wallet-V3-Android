package com.blockchain.koin

import com.blockchain.network.modules.MoshiBuilderInterceptorList
import com.blockchain.network.modules.apiModule
import com.squareup.moshi.Moshi
import org.amshove.kluent.`should not be`
import org.junit.Test
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext
import org.koin.standalone.get
import org.koin.test.AutoCloseKoinTest
import piuk.blockchain.androidbuysell.models.coinify.BuyFrequency
import piuk.blockchain.androidbuysell.models.coinify.CannotTradeReason
import piuk.blockchain.androidbuysell.models.coinify.Details
import piuk.blockchain.androidbuysell.models.coinify.GrantType
import piuk.blockchain.androidbuysell.models.coinify.Medium
import piuk.blockchain.androidbuysell.models.coinify.ReviewState
import piuk.blockchain.androidbuysell.models.coinify.TradeState
import piuk.blockchain.androidbuysell.models.coinify.TransferState

class BuySellModuleTest : AutoCloseKoinTest() {

    @Test
    fun `the buySell module registers all of the buy sell adapters`() {
        StandAloneContext.startKoin(
            listOf(
                buySellModule,
                apiModule,
                applicationContext {
                    bean {
                        MoshiBuilderInterceptorList(
                            listOf(
                                get("buySell")
                            )
                        )
                    }
                }
            )
        )

        get<Moshi>().apply {
            adapter(CannotTradeReason::class.java) `should not be` null
            adapter(ReviewState::class.java) `should not be` null
            adapter(Medium::class.java) `should not be` null
            adapter(TradeState::class.java) `should not be` null
            adapter(TransferState::class.java) `should not be` null
            adapter(Details::class.java) `should not be` null
            adapter(GrantType::class.java) `should not be` null
            adapter(BuyFrequency::class.java) `should not be` null
        }
    }
}

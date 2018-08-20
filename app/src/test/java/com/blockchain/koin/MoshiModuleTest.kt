package com.blockchain.koin

import com.blockchain.koin.modules.moshiModule
import com.blockchain.network.modules.apiModule
import com.squareup.moshi.Moshi
import org.amshove.kluent.`should not be`
import org.junit.Test
import org.koin.standalone.StandAloneContext
import org.koin.standalone.get
import org.koin.test.AutoCloseKoinTest
import piuk.blockchain.androidbuysell.models.coinify.CannotTradeReason

class MoshiModuleTest : AutoCloseKoinTest() {

    @Test
    fun `the moshi module injects at least one of the buy sell adapters`() {
        StandAloneContext.startKoin(
            listOf(
                apiModule,
                moshiModule
            )
        )

        get<Moshi>().adapter(CannotTradeReason::class.java) `should not be` null
    }
}

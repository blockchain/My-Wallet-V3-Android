package com.blockchain.koin.modules

import com.blockchain.koin.walletModule
import com.nhaarman.mockito_kotlin.mock
import info.blockchain.api.blockexplorer.BlockExplorer
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.PayloadManagerWiper
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should not be`
import org.junit.Test
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext
import org.koin.standalone.inject
import org.koin.test.AutoCloseKoinTest

class PayloadManagerWipingTest : AutoCloseKoinTest() {

    @Test
    fun `After wiping the payload manager, a new request for a payload manager gets a distinct instance`() {
        StandAloneContext.startKoin(listOf(
            walletModule,
            applicationContext {
                bean { mock<WalletApi>() }
                bean { mock<BlockExplorer>() }
            }
        ))

        val firstPayloadManager: PayloadManager by inject()
        val secondPayloadManager: PayloadManager by inject()

        firstPayloadManager `should be` secondPayloadManager

        val thirdPayloadManager: PayloadManager by inject()

        val wiper: PayloadManagerWiper by inject()

        wiper.wipe()

        thirdPayloadManager `should not be` secondPayloadManager
    }
}

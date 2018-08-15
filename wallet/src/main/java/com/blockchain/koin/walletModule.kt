package com.blockchain.koin

import info.blockchain.wallet.multiaddress.MultiAddressFactory
import info.blockchain.wallet.payload.BalanceManagerBch
import info.blockchain.wallet.payload.BalanceManagerBtc
import info.blockchain.wallet.payload.PayloadManager
import info.blockchain.wallet.payload.PayloadManagerWiper
import org.koin.KoinContext
import org.koin.dsl.module.applicationContext
import org.koin.standalone.StandAloneContext

val walletModule = applicationContext {

    factory { MultiAddressFactory(get()) }

    factory { BalanceManagerBtc(get()) }

    factory { BalanceManagerBch(get()) }

    context("Payload") {

        bean { PayloadManager(get(), get(), get(), get()) }
    }

    bean {
        object : PayloadManagerWiper {
            override fun wipe() {
                (StandAloneContext.koinContext as KoinContext).releaseContext("Payload")
            }
        } as PayloadManagerWiper
    }
}

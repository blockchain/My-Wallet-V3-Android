package com.blockchain.koin

import info.blockchain.wallet.multiaddress.MultiAddressFactory
import info.blockchain.wallet.payload.BalanceManagerBch
import info.blockchain.wallet.payload.BalanceManagerBtc
import info.blockchain.wallet.payload.PayloadManager
import org.koin.dsl.module.applicationContext

val walletModule = applicationContext {

    factory { MultiAddressFactory(get()) }

    factory { BalanceManagerBtc(get()) }

    factory { BalanceManagerBch(get()) }

    bean { PayloadManager(get(), get(), get(), get()) }

}

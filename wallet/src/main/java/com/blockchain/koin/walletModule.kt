package com.blockchain.koin

import info.blockchain.wallet.payload.PayloadManager
import org.koin.dsl.module.applicationContext

val walletModule = applicationContext {

    bean { PayloadManager.getInstance() }
}

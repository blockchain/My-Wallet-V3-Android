package com.blockchain.lockbox.koin

import com.blockchain.accounts.AsyncAccountList
import com.blockchain.balance.TotalBalance
import com.blockchain.lockbox.balance.LockboxTotalBalance
import com.blockchain.lockbox.data.LockboxDataManager
import com.blockchain.lockbox.data.remoteconfig.LockboxRemoteConfig
import com.blockchain.lockbox.ui.LockboxLandingPresenter
import com.blockchain.remoteconfig.FeatureFlag
import org.koin.dsl.module.applicationContext

val lockboxModule = applicationContext {

    context("Payload") {

        factory { LockboxDataManager(get(), get("lockbox")) }

        factory("lockbox") { get<LockboxDataManager>() as AsyncAccountList }

        factory { LockboxLandingPresenter(get(), get()) }

        factory("lockbox") { LockboxTotalBalance(get("lockbox"), get("all")) as TotalBalance }
    }

    factory("lockbox") { LockboxRemoteConfig(get()) }
        .bind(FeatureFlag::class)
}

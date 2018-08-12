package com.blockchain.koin.modules

import com.blockchain.network.EnvironmentUrls
import info.blockchain.wallet.BlockchainFramework
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

val environmentModule = applicationContext {

    bean { EnvironmentSettings() as EnvironmentConfig }
        .bind(EnvironmentUrls::class)

    bean("explorer-url") { get<EnvironmentUrls>().explorerUrl }

    bean("app-version") { get<EnvironmentConfig>().appVersion }

    bean("api-code") { BlockchainFramework.getApiCode() }

    bean("device-id") { BlockchainFramework.getDeviceId() }
}

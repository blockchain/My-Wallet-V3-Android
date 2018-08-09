package com.blockchain.koin.modules

import com.blockchain.network.EnvironmentUrls
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.data.api.EnvironmentSettings
import piuk.blockchain.androidcore.data.api.EnvironmentConfig

val environment = applicationContext {

    bean { EnvironmentSettings() as EnvironmentConfig }

    bean { get<EnvironmentConfig>() as EnvironmentUrls }
}

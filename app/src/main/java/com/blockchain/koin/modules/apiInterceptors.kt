package com.blockchain.koin.modules

import android.os.Build
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.BuildConfig
import piuk.blockchain.androidcore.data.api.interceptors.ApiInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.UserAgentInterceptor

val apiInterceptors = applicationContext {

    bean {
        listOf(
            // Add logging for debugging purposes
            ApiInterceptor(),
            // Add header in all requests
            UserAgentInterceptor(BuildConfig.VERSION_NAME, Build.VERSION.RELEASE)
        )
    }
}

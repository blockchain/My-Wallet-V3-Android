package com.blockchain.koin.modules

import android.os.Build
import com.blockchain.network.modules.OkHttpInterceptors
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.BuildConfig
import piuk.blockchain.androidcore.data.api.interceptors.ApiInterceptor
import piuk.blockchain.androidcore.data.api.interceptors.UserAgentInterceptor

val apiInterceptorsModule = applicationContext {

    bean {
        OkHttpInterceptors(
            listOf(
                // Add logging for debugging purposes
                ApiInterceptor(),
                // Add header in all requests
                UserAgentInterceptor(BuildConfig.VERSION_NAME, Build.VERSION.RELEASE)
            )
        )
    }
}

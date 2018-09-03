package com.blockchain.morph.dev

import android.app.Application
import com.blockchain.koin.morphUiModule
import com.blockchain.koin.walletModule
import com.blockchain.network.EnvironmentUrls
import com.blockchain.network.modules.OkHttpInterceptors
import com.blockchain.network.modules.apiModule
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.BlockchainFramework
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.applicationContext
import timber.log.Timber

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        startKoin(
            this, listOf(
                apiModule,
                serviceModule,
                environmentModule,
                morphUiModule,
                walletModule,
                applicationContext {
                    bean { OkHttpInterceptors(emptyList()) }
                }
            )
        )
    }
}

val serviceModule = applicationContext {
    bean {
        object : ApiCode {
            override val apiCode: String
                get() = "ABC1234"
        } as ApiCode
    }
}

val environmentModule = applicationContext {

    bean {
        object : EnvironmentUrls {
            override val explorerUrl: String
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val apiUrl: String
                get() = "https://api.blockchain.info/"

            override fun websocketUrl(currency: CryptoCurrency): String {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        } as EnvironmentUrls
    }

    bean("explorer-url") { get<EnvironmentUrls>().explorerUrl }

    bean("device-id") { BlockchainFramework.getDeviceId() }
}

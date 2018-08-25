package com.blockchain.morph.dev

import android.app.Application
import com.blockchain.morph.ui.homebrew.exchange.RateStream
import com.blockchain.network.EnvironmentUrls
import com.blockchain.network.modules.OkHttpInterceptors
import com.blockchain.network.modules.apiModule
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.prices.CurrentPriceApi
import info.blockchain.wallet.prices.PriceApi
import info.blockchain.wallet.prices.PriceEndpoints
import info.blockchain.wallet.prices.toCachedIndicativeFiatPriceService
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.applicationContext
import retrofit2.Retrofit
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
                applicationContext {
                    bean { OkHttpInterceptors(emptyList()) }
                    bean { FakeRatesStream(get()) as RateStream }
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

    factory { get<Retrofit>("api").create(PriceEndpoints::class.java) }
    factory { PriceApi(get(), get()) }
    factory { get<PriceApi>() as CurrentPriceApi }

    factory { get<CurrentPriceApi>().toCachedIndicativeFiatPriceService() }
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

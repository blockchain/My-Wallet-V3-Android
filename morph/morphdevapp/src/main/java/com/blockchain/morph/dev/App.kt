package com.blockchain.morph.dev

import android.app.Application
import com.blockchain.koin.morphUiModule
import com.blockchain.koin.walletModule
import com.blockchain.morph.ui.homebrew.exchange.ExchangeHistoryActivity
import com.blockchain.injection.kycModule
import android.content.Context
import android.provider.Settings
import com.blockchain.koin.coreModule
import com.blockchain.morph.dev.BuildConfig
import com.blockchain.network.EnvironmentUrls
import com.blockchain.network.modules.MoshiBuilderInterceptorList
import com.blockchain.network.modules.OkHttpInterceptors
import com.blockchain.network.modules.apiModule
import info.blockchain.balance.CryptoCurrency
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.BlockchainFramework
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.applicationContext
import info.blockchain.wallet.FrameworkInterface
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.api.FeeApi
import info.blockchain.wallet.api.FeeEndpoints
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.api.WalletExplorerEndpoints
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.settings.SettingsManager
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import org.koin.android.ext.android.get
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import retrofit2.Retrofit
import timber.log.Timber
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PrefsUtil

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        startKoin(
            this, listOf(
                apiModule,
                coreModule,
                serviceModule,
                environmentModule,
                morphUiModule,
                walletModule,
                kycModule,
                walletModule,
                applicationContext {
                    bean { OkHttpInterceptors(emptyList()) }
                }
            ),
            extraProperties = mapOf(
                "api-code" to "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3",
                "app-version" to BuildConfig.VERSION_NAME
            )
        )
        AccessState.getInstance().initAccessState(this, PrefsUtil(this), RxBus(), ExchangeHistoryActivity::class.java)

        BlockchainFramework.init(object : FrameworkInterface {
            override fun getRetrofitApiInstance(): Retrofit {
                return get("api")
            }

            override fun getRetrofitExplorerInstance(): Retrofit {
                return get("explorer")
            }

            override fun getEnvironment(): Environment {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getBitcoinParams(): NetworkParameters {
                return BitcoinMainNetParams.get()
            }

            override fun getBitcoinCashParams(): NetworkParameters {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getDevice(): String {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getAppVersion(): String {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getDeviceId(): String {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override val apiCode: String
                get() = get<ApiCode>().apiCode
        })
    }
}

val serviceModule = applicationContext {
    bean {
        object : ApiCode {
            override val apiCode: String
                get() = getProperty("api-code")
        } as ApiCode
    }

    bean { SettingsManager(get()) }

    bean { get<Retrofit>("explorer").create(WalletExplorerEndpoints::class.java) }

    bean { get<Retrofit>("api").create(FeeEndpoints::class.java) }

    factory { WalletApi(get(), get()) }

    factory { Payment() }

    factory { FeeApi(get()) }

    factory { EthAccountApi() }
}

val environmentModule = applicationContext {

    bean {
        object : EnvironmentConfig {
            override val environment: Environment
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val bitcoinNetworkParameters: NetworkParameters
                get() = BitcoinCashMainNetParams.get()
            override val bitcoinCashNetworkParameters: NetworkParameters
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.
            override val coinifyUrl: String
                get() = TODO("not implemented") //To change initializer of created properties use File | Settings | File Templates.

            override fun shouldShowDebugMenu(): Boolean {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override val explorerUrl: String
                get() = "https://explorer.dev.blockchain.info/"
            override val apiUrl: String
                get() = "https://api.dev.blockchain.info/"

            override fun websocketUrl(currency: CryptoCurrency): String {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        } as EnvironmentConfig
    }.bind(EnvironmentUrls::class)

    bean("explorer-url") { get<EnvironmentUrls>().explorerUrl }

    bean("device-id") {
        Settings.Secure.getString(
            get<Context>().contentResolver,
            Settings.Secure.ANDROID_ID
        )
    }

    bean { MoshiBuilderInterceptorList(emptyList()) }
}

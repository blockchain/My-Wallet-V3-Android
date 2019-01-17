package com.blockchain.morph.dev

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import com.blockchain.datamanagers.MaximumSpendableCalculator
import com.blockchain.injection.kycModule
import com.blockchain.koin.morphUiModule
import com.blockchain.koin.walletModule
import com.blockchain.morph.exchange.service.FiatPeriodicLimit
import com.blockchain.morph.exchange.service.FiatTradesLimits
import com.blockchain.morph.exchange.service.QuoteService
import com.blockchain.morph.exchange.service.QuoteServiceFactory
import com.blockchain.morph.exchange.service.TradeLimitService
import com.blockchain.morph.ui.homebrew.exchange.history.TradeHistoryActivity
import com.blockchain.nabu.CurrentTier
import com.blockchain.nabu.StartKyc
import com.blockchain.network.EnvironmentUrls
import com.blockchain.network.modules.MoshiBuilderInterceptorList
import com.blockchain.network.modules.OkHttpInterceptors
import com.blockchain.network.modules.apiModule
import com.blockchain.notifications.analytics.EventLogger
import com.blockchain.notifications.analytics.Loggable
import com.blockchain.preferences.FiatCurrencyPreference
import info.blockchain.balance.AccountReference
import info.blockchain.balance.CryptoCurrency
import info.blockchain.balance.CryptoValue
import info.blockchain.balance.FiatValue
import info.blockchain.balance.withMajorValue
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.FrameworkInterface
import info.blockchain.wallet.api.Environment
import info.blockchain.wallet.api.FeeApi
import info.blockchain.wallet.api.FeeEndpoints
import info.blockchain.wallet.api.WalletApi
import info.blockchain.wallet.api.WalletExplorerEndpoints
import info.blockchain.wallet.ethereum.EthAccountApi
import info.blockchain.wallet.payment.Payment
import info.blockchain.wallet.settings.SettingsManager
import io.reactivex.Single
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.BitcoinCashMainNetParams
import org.bitcoinj.params.BitcoinMainNetParams
import org.koin.android.ext.android.get
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.data.access.AccessState
import piuk.blockchain.androidcore.data.access.LogoutTimer
import piuk.blockchain.androidcore.data.api.EnvironmentConfig
import piuk.blockchain.androidcore.data.rxjava.RxBus
import piuk.blockchain.androidcore.utils.PrefsUtil
import retrofit2.Retrofit
import timber.log.Timber
import java.util.concurrent.TimeUnit

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
                kycModule,
                walletModule,
                fakesModule,
                simulatedAccountsModule,
                applicationContext {
                    bean { OkHttpInterceptors(emptyList()) }
                }
            ),
            extraProperties = mapOf(
                "api-code" to "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3",
                "app-version" to BuildConfig.VERSION_NAME
            )
        )
        AccessState.getInstance().initAccessState(this, PrefsUtil(this), RxBus(), TradeHistoryActivity::class.java)

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

val fakesModule = applicationContext {

    bean {
        object : StartKyc {
            override fun startKycActivity(context: Any) {
                Timber.d("Would start KYC here")
                Toast.makeText(context as Context, "Would start KYC here", Toast.LENGTH_SHORT).show()
                context.startActivity(Intent(context, MainActivity::class.java))
            }
        } as StartKyc
    }

    bean {
        object : QuoteServiceFactory {
            override fun createQuoteService(): QuoteService {
                return FakeQuoteService()
            }
        } as QuoteServiceFactory
    }

    bean {
        object : MaximumSpendableCalculator {
            override fun getMaximumSpendable(accountReference: AccountReference): Single<CryptoValue> {
                return Single.just(accountReference.cryptoCurrency.withMajorValue(1000.toBigDecimal()))
            }
        } as MaximumSpendableCalculator
    }

    bean {
        object : CurrentTier {
            var count = 0
            override fun usersCurrentTier(): Single<Int> =
                Single.just(count++ % 2).delay(100, TimeUnit.MILLISECONDS)
        } as CurrentTier
    }

    bean {
        object : FiatCurrencyPreference {
            override val fiatCurrencyPreference: String
                get() = "USD"
        } as FiatCurrencyPreference
    }

    bean {
        object : EventLogger {
            override fun logEvent(loggable: Loggable) {
                Timber.d(loggable.eventName)
            }
        } as EventLogger
    }

    bean {
        object : LogoutTimer {
            override fun start(context: Context) {}

            override fun stop(context: Context) {}
        } as LogoutTimer
    }

    bean {
        object : TradeLimitService {
            override fun getTradesLimits(fiatCurrency: String): Single<FiatTradesLimits> {
                fun Int.usd() = FiatValue.fromMajor("USD", toBigDecimal())
                return Single.just(
                    FiatTradesLimits(
                        minOrder = 1.usd(),
                        maxOrder = 50000.usd(),
                        maxPossibleOrder = 10000.usd(),
                        daily = FiatPeriodicLimit(100.usd(), 30.usd(), 70.usd()),
                        weekly = FiatPeriodicLimit(700.usd(), (7 * 30).usd(), (7 * 70).usd()),
                        annual = FiatPeriodicLimit(1000.usd(), 300.usd(), 700.usd())
                    )
                ).delay(100, TimeUnit.MILLISECONDS)
            }
        } as TradeLimitService
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

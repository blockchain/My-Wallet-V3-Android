package com.blockchain.kyc.dev

import android.app.Application
import android.content.Context
import android.widget.Toast
import com.blockchain.activities.StartSwap
import com.blockchain.injection.kycModule
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager
import com.blockchain.kyc.models.nabu.KycTierState
import com.blockchain.kyc.models.nabu.LimitsJson
import com.blockchain.kyc.models.nabu.TierJson
import com.blockchain.kyc.models.nabu.TiersJson
import com.blockchain.kyc.services.nabu.TierService
import com.blockchain.kyc.services.nabu.TierUpdater
import com.blockchain.kycui.address.Tier2Decision
import com.blockchain.metadata.MetadataRepository
import com.blockchain.nabu.NabuToken
import com.blockchain.nabu.models.NabuOfflineTokenResponse
import com.blockchain.notifications.analytics.Loggable
import info.blockchain.wallet.ApiCode
import info.blockchain.wallet.BlockchainFramework
import info.blockchain.wallet.FrameworkInterface
import info.blockchain.wallet.api.Environment
import io.reactivex.Completable
import io.reactivex.Single
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.params.BitcoinMainNetParams
import org.koin.android.ext.android.get
import org.koin.android.ext.android.startKoin
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.data.access.LogoutTimer
import piuk.blockchain.androidcore.data.settings.Email
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater
import piuk.blockchain.androidcore.data.settings.PhoneNumber
import piuk.blockchain.androidcore.data.settings.PhoneNumberUpdater
import piuk.blockchain.androidcore.data.settings.PhoneVerificationQuery
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
                kycModule,
                fakesModule
            ),
            extraProperties = mapOf(
                "api-code" to "25a6ad13-1633-4dfb-b6ee-9b91cdf0b5c3",
                "app-version" to BuildConfig.VERSION_NAME
            )
        )

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
        object : StartSwap {
            override fun startSwapActivity(context: Any) {
                Timber.d("Would start Swap here")
                Toast.makeText(context as Context, "Would start Swap here", Toast.LENGTH_SHORT).show()
            }
        } as StartSwap
    }

    bean { PrefsUtil(get()) }

    bean {
        object : TierUpdater {
            override fun setUserTier(tier: Int): Completable {
                Timber.d("setUserTier($tier)")
                return Completable.complete()
            }
        } as TierUpdater
    }

    bean {
        object : TierService {
            override fun tiers(): Single<TiersJson> {
                return Single.just(
                    TiersJson(
                        tiers = listOf(
                            TierJson(
                                0,
                                "Tier 0",
                                state = KycTierState.Verified,
                                limits = LimitsJson(
                                    currency = "USD",
                                    daily = null,
                                    annual = null
                                )
                            ),
                            TierJson(
                                1,
                                "Tier 1",
                                state = KycTierState.Verified,
                                limits = LimitsJson(
                                    currency = "USD",
                                    daily = null,
                                    annual = 1000.0.toBigDecimal()
                                )
                            ),
                            TierJson(
                                2,
                                "Tier 2",
                                state = KycTierState.None,
                                limits = LimitsJson(
                                    currency = "USD",
                                    daily = 25000.0.toBigDecimal(),
                                    annual = null
                                )
                            )
                        )
                    )
                ).delay(1, TimeUnit.SECONDS)
            }
        } as TierService
    }

    bean {
        object : Tier2Decision {
            override fun progressToTier2(): Single<Tier2Decision.NextStep> {
                return Single.just(Tier2Decision.NextStep.Tier1Complete)
                    .delay(1, TimeUnit.SECONDS)
            }
        } as Tier2Decision
    }

    bean {
        object : PhoneVerificationQuery {
            override fun isPhoneNumberVerified(): Single<Boolean> =
                Single.just(false).delay(1, TimeUnit.SECONDS)
        } as PhoneVerificationQuery
    }

    bean {
        object : EmailSyncUpdater {
            private var emailSaved: String = "a@b.com"
            private var verified: Boolean = false
            private var count: Int = 0

            override fun email(): Single<Email> {
                return Single.just(Email(emailSaved, verified)).delay(500, TimeUnit.MILLISECONDS)
            }

            override fun updateEmailAndSync(email: String): Single<Email> {
                verified = count++ >= 3
                return Single.timer(500, TimeUnit.MILLISECONDS)
                    .doOnSuccess {
                        emailSaved = email
                    }
                    .map { Email(email, verified) }
            }

            override fun resendEmail(email: String): Single<Email> {
                return updateEmailAndSync(email)
            }
        } as EmailSyncUpdater
    }

    bean {
        object : PhoneNumberUpdater {
            private var sms: String = ""

            override fun smsNumber(): Single<String> {
                return Single.just(sms).delay(500, TimeUnit.MILLISECONDS)
            }

            override fun updateSms(phoneNumber: PhoneNumber): Single<String> {
                return Single.timer(500, TimeUnit.MILLISECONDS)
                    .doOnSuccess {
                        sms = phoneNumber.sanitized
                        Timber.d("Updated SMS to $sms")
                    }
                    .map { phoneNumber.sanitized }
            }

            override fun verifySms(code: String): Single<String> {
                return smsNumber()
            }
        } as PhoneNumberUpdater
    }

    bean {
        object : NabuToken {
            override fun fetchNabuToken(): Single<NabuOfflineTokenResponse> {
                return Single.just(NabuOfflineTokenResponse("USER123", "TOKEN456"))
            }
        } as NabuToken
    }

    bean {
        object : com.blockchain.notifications.analytics.EventLogger {
            override fun logEvent(loggable: Loggable) {
                Timber.d("Event log: ${loggable.eventName}")
            }
        } as com.blockchain.notifications.analytics.EventLogger
    }

    bean {
        object : LogoutTimer {
            override fun start(context: Context) {}

            override fun stop(context: Context) {}
        } as LogoutTimer
    }

    context("Payload") {

        bean {
            InMemoryMetadataRepository() as MetadataRepository
        }

        factory {
            FakeNabuDataManager() as NabuDataManager
        }
    }
}

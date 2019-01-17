package com.blockchain.koin

import com.blockchain.logging.EventLogger
import com.blockchain.metadata.MetadataWarningLog
import com.blockchain.remoteconfig.RemoteConfig
import com.blockchain.remoteconfig.RemoteConfiguration
import com.blockchain.transactions.ResourceSendFundsResultLocalizer
import com.blockchain.transactions.SendFundsResultLocalizer
import com.blockchain.ui.chooser.AccountChooserPresenter
import com.crashlytics.android.answers.Answers
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.dashboard.AsyncDashboardDataCalculator
import piuk.blockchain.android.ui.dashboard.BalanceUpdater
import piuk.blockchain.android.ui.dashboard.DashboardData
import piuk.blockchain.androidcoreui.BuildConfig
import piuk.blockchain.androidcoreui.utils.logging.AnswersEventLogger
import piuk.blockchain.androidcoreui.utils.logging.Logging
import timber.log.Timber

val coreUiModule = applicationContext {

    context("Payload") {

        factory { BalanceUpdater(get(), get()) }

        factory {
            AsyncDashboardDataCalculator(
                get(),
                get(),
                get("all")
            ) as DashboardData
        }

        factory {
            AccountChooserPresenter(get(), get(), get())
        }
    }

    factory {

        object : MetadataWarningLog {
            override fun logWarning(warning: String) {
                Timber.e(warning)
                val throwable = Throwable(warning)
                Logging.logException(throwable)
                if (BuildConfig.DEBUG) {
                    // we want to know about this on a debug build
                    throw throwable
                }
            }
        } as MetadataWarningLog
    }

    bean {
        val config = FirebaseRemoteConfigSettings.Builder()
            .setDeveloperModeEnabled(BuildConfig.DEBUG)
            .build()
        FirebaseRemoteConfig.getInstance().apply {
            setConfigSettings(config)
        }
    }

    factory { RemoteConfiguration(get()) }
        .bind(RemoteConfig::class)

    factory { ResourceSendFundsResultLocalizer(get()) as SendFundsResultLocalizer }

    factory { Answers.getInstance() }

    factory { AnswersEventLogger(get()) as EventLogger }
}

package com.blockchain.koin

import android.app.Application
import com.blockchain.injection.kycModule
import com.blockchain.koin.modules.apiInterceptorsModule
import com.blockchain.koin.modules.appProperties
import com.blockchain.koin.modules.applicationModule
import com.blockchain.koin.modules.environmentModule
import com.blockchain.koin.modules.features
import com.blockchain.koin.modules.homeBrewModule
import com.blockchain.koin.modules.keys
import com.blockchain.koin.modules.localShapeShift
import com.blockchain.koin.modules.morphMethodModule
import com.blockchain.koin.modules.moshiModule
import com.blockchain.koin.modules.serviceModule
import com.blockchain.koin.modules.shapeShiftModule
import com.blockchain.network.modules.apiModule
import org.koin.android.ext.android.startKoin
import org.koin.log.Logger
import org.koin.standalone.StandAloneContext
import timber.log.Timber

object KoinStarter {

    private lateinit var application: Application

    @JvmStatic
    fun start(application: Application) {
        StandAloneContext.closeKoin()
        application.startKoin(
            application,
            listOf(
                environmentModule,
                walletModule,
                coreModule,
                coreUiModule,
                apiModule,
                apiInterceptorsModule,
                serviceModule,
                applicationModule,
                shapeShiftModule,
                localShapeShift,
                buySellModule,
                moshiModule,
                kycModule,
                morphMethodModule,
                homeBrewModule
            ),
            extraProperties = features + appProperties + keys,
            logger = TimberLogger()
        )
        KoinStarter.application = application
    }
}

private class TimberLogger : Logger {
    override fun debug(msg: String) {
        Timber.d(msg)
    }

    override fun err(msg: String) {
        Timber.e(msg)
    }

    override fun log(msg: String) {
        Timber.i(msg)
    }
}

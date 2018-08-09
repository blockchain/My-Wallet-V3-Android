package com.blockchain.koin

import android.app.Application
import com.blockchain.koin.modules.apiInterceptorsModule
import com.blockchain.koin.modules.environmentModule
import com.blockchain.koin.modules.moshiModule
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
                apiModule,
                apiInterceptorsModule,
                shapeShiftModule,
                buySellModule,
                moshiModule
            ),
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

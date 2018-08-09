package com.blockchain.koin

import android.app.Application
import com.blockchain.koin.modules.apiInterceptors
import com.blockchain.koin.modules.environment
import com.blockchain.koin.modules.shapeShiftModule
import com.blockchain.network.modules.apiModule
import org.koin.android.ext.android.startKoin
import org.koin.log.Logger
import timber.log.Timber

object KoinStarter {

    private lateinit var application: Application

    @JvmStatic
    fun start(application: Application) {
        application.startKoin(
            application,
            listOf(
                environment,
                apiModule,
                apiInterceptors,
                shapeShiftModule
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

package com.blockchain.koin

import android.app.Application
import com.blockchain.network.modules.apiModule
import org.koin.android.ext.android.startKoin
import com.blockchain.koin.modules.apiInterceptors
import com.blockchain.koin.modules.environment
import com.blockchain.koin.modules.shapeShiftModule

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
            )
        )
        KoinStarter.application = application
    }
}

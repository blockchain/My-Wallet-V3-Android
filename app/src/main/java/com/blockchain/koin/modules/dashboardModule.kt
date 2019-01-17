package com.blockchain.koin.modules

import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.dashboard.DashboardPresenter

val dashboardModule = applicationContext {

    context("Payload") {

        factory {
            DashboardPresenter(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
    }
}

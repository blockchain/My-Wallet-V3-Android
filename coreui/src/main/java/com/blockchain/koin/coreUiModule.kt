package com.blockchain.koin

import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.chooser.AccountChooserPresenter

val coreUiModule = applicationContext {

    factory {
        AccountChooserPresenter(get(), get(), get())
    }
}
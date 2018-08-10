package com.blockchain.koin

import org.koin.dsl.module.applicationContext
import com.blockchain.ui.chooser.AccountChooserPresenter

val coreUiModule = applicationContext {

    factory {
        AccountChooserPresenter(get(), get(), get())
    }
}
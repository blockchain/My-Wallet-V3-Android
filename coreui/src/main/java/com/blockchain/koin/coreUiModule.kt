package com.blockchain.koin

import org.koin.dsl.module.applicationContext
import com.blockchain.ui.chooser.AccountChooserPresenter

val coreUiModule = applicationContext {

    context("Payload") {

        factory {
            AccountChooserPresenter(get(), get(), get())
        }
    }
}

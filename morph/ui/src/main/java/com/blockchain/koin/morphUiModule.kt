package com.blockchain.koin

import android.app.Activity
import com.blockchain.morph.ui.homebrew.exchange.ExchangeFragmentConfigurationChangePersistence
import com.blockchain.morph.ui.homebrew.exchange.ExchangeModel
import com.blockchain.morph.ui.homebrew.exchange.confirmation.ExchangeConfirmationPresenter
import com.blockchain.morph.ui.homebrew.exchange.history.TradeHistoryPresenter
import com.blockchain.morph.ui.regulation.stateselection.UsStateSelectionActivity
import com.blockchain.morph.ui.regulation.stateselection.UsStateSelectionActivityStarter
import com.blockchain.morph.ui.regulation.stateselection.UsStateSelectionPresenter
import org.koin.dsl.module.applicationContext
import piuk.blockchain.androidcore.utils.PrefsUtil

val morphUiModule = applicationContext {

    bean {
        object : UsStateSelectionActivityStarter {

            private var requestCode = 55000

            override fun startForResult(parent: Activity): Int {
                val requestCode = requestCode++
                UsStateSelectionActivity.startForResult(
                    parent,
                    requestCode
                )
                return requestCode
            }
        } as UsStateSelectionActivityStarter
    }

    bean { PrefsUtil(get()) }

    context("Payload") {

        factory { UsStateSelectionPresenter(get()) }

        factory { ExchangeConfirmationPresenter(get(), get(), get(), get(), get(), get(), get()) }

        factory { TradeHistoryPresenter(get("nabu"), get("shapeshift"), get()) }

        context("Quotes") {

            factory { ExchangeFragmentConfigurationChangePersistence(get()) }

            factory { ExchangeModel(get(), get(), get()) }
        }
    }

    apply { registerDebug() }
}

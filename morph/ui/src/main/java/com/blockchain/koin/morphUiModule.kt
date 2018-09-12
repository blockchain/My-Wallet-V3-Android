package com.blockchain.koin

import android.app.Activity
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
    }
    apply { registerDebug() }
}
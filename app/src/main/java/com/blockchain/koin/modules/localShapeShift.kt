package com.blockchain.koin.modules

import org.koin.dsl.module.applicationContext
import com.blockchain.morph.ui.detail.TradeDetailPresenter

/**
 * Will move to ShapeShift module with these classes
 */
val localShapeShift = applicationContext {

    context("Payload") {

        factory { TradeDetailPresenter(get(), get()) }
    }
}

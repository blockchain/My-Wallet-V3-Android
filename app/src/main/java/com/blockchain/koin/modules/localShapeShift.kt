package com.blockchain.koin.modules

import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.shapeshift.detail.ShapeShiftDetailPresenter

/**
 * Will move to ShapeShift module with these classes
 */
val localShapeShift = applicationContext {

    context("Payload") {

        factory { ShapeShiftDetailPresenter(get(), get()) }
    }
}

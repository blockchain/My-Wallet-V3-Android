package com.blockchain.koin.modules

import android.app.Activity
import com.blockchain.kycui.navhost.KycNavHostActivity
import com.blockchain.morph.MorphMethodSelector
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.ui.shapeshift.overview.ShapeShiftActivity
import timber.log.Timber

enum class MorphMethodType {
    ShapeShift,
    HomeBrew
}

/**
 * Yields the morph method enum to use.
 * Asynchronous, so you can ask a server or whatever you need to.
 */
interface MorphMethodTypeSelector : MorphMethodSelector<MorphMethodType>

/**
 * Yields a method you can use to launch the correct trading method.
 * Asynchronous, so you can ask a server or whatever you need to.
 */
interface MorphActivityLauncher : MorphMethodSelector<(Activity) -> Unit>

fun MorphActivityLauncher.launchAsync(activity: Activity) {
    getMorphMethod()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ it(activity) }, { Timber.e(it) })
}

val morphMethodModule = applicationContext {

    bean {
        // TODO: We need to choose whether to send users to Legacy SS, or Homebrew/KYC at this point AND-1248
        // These are fixed choices based on build type, but we have ability to inject a more sophisticated and
        // asynchronous implementation here.
        if (BuildConfig.DEBUG) {
            fixedSelector(MorphMethodType.HomeBrew)
        } else {
            fixedSelector(MorphMethodType.ShapeShift)
        }
    }

    bean {
        object : MorphActivityLauncher {
            override fun getMorphMethod() =
                get<MorphMethodTypeSelector>()
                    .getMorphMethod()
                    .map {
                        when (it) {
                            MorphMethodType.ShapeShift -> { activity: Activity -> ShapeShiftActivity.start(activity) }
                            MorphMethodType.HomeBrew -> { activity: Activity -> KycNavHostActivity.start(activity) }
                        }
                    }
        } as MorphActivityLauncher
    }
}

private fun fixedSelector(morphMethodType: MorphMethodType): MorphMethodTypeSelector =
    object : MorphMethodTypeSelector {
        override fun getMorphMethod() = Single.just(morphMethodType)
    }

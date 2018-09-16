package com.blockchain.koin.modules

import android.app.Activity
import android.content.Intent
import android.support.annotation.VisibleForTesting
import com.blockchain.kycui.navhost.KycNavHostActivity
import com.blockchain.kycui.settings.KycStatusHelper
import com.blockchain.kycui.settings.SettingsKycState
import com.blockchain.morph.MorphMethodSelector
import com.blockchain.morph.ui.homebrew.exchange.history.TradeHistoryActivity
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.ui.shapeshift.overview.ShapeShiftActivity
import timber.log.Timber

enum class MorphMethodType {
    ShapeShift,
    HomeBrew,
    Kyc
}

/**
 * Yields the morph method enum to use.
 * Asynchronous, so you can ask a server or whatever you need to.
 */
interface MorphMethodTypeSelector : MorphMethodSelector<MorphMethodType>

/**
 * Yields a method you can use to launch the correct trading method or route to KYC.
 * Asynchronous, so you can ask a server or whatever you need to.
 */
interface MorphActivityLauncher : MorphMethodSelector<(Activity) -> Unit>

fun MorphActivityLauncher.launchAsync(activity: Activity) {
    getMorphMethod()
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe({ it(activity) }, { Timber.e(it) })
}

val morphMethodModule = applicationContext {

    context("Payload") {

        bean {
            dynamicSelector(get())
        }
    }

    bean {
        object : MorphActivityLauncher {
            override fun getMorphMethod() =
                get<MorphMethodTypeSelector>()
                    .getMorphMethod()
                    .map {
                        when (it) {
                            MorphMethodType.ShapeShift -> { activity: Activity ->
                                ShapeShiftActivity.start(activity)
                            }
                            MorphMethodType.HomeBrew -> { activity: Activity ->
                                activity.startActivity(
                                    Intent(activity, TradeHistoryActivity::class.java)
                                )
                            }
                            MorphMethodType.Kyc -> { activity: Activity ->
                                KycNavHostActivity.start(activity)
                            }
                        }
                    }
        } as MorphActivityLauncher
    }
}

@VisibleForTesting
internal fun dynamicSelector(
    kycStatusHelper: KycStatusHelper
): MorphMethodTypeSelector =
    object : MorphMethodTypeSelector {
        override fun getMorphMethod(): Single<MorphMethodType> {
            return kycStatusHelper.getSettingsKycState()
                .map {
                    when (it) {
                        SettingsKycState.Hidden -> return@map MorphMethodType.ShapeShift
                        SettingsKycState.Verified -> return@map MorphMethodType.HomeBrew
                        else -> return@map MorphMethodType.Kyc
                    }
                }
        }
    }
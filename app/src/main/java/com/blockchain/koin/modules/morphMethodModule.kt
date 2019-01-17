package com.blockchain.koin.modules

import android.app.Activity
import android.content.Context
import com.blockchain.activities.StartSwap
import com.blockchain.kycui.navhost.KycNavHostActivity
import com.blockchain.kycui.navhost.models.CampaignType
import com.blockchain.morph.MorphMethodSelector
import com.blockchain.morph.ui.homebrew.exchange.history.TradeHistoryActivity
import io.reactivex.android.schedulers.AndroidSchedulers
import org.koin.dsl.module.applicationContext
import piuk.blockchain.android.kyc.morphmethod.TiersMorphMethodTypeSelectorSelector
import piuk.blockchain.android.kyc.morphmethod.logCalls
import timber.log.Timber

enum class MorphMethodType {
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
            TiersMorphMethodTypeSelectorSelector(get(), get()).logCalls(get())
        }
    }

    bean {
        SwapStarter() as StartSwap
    }

    bean {
        object : MorphActivityLauncher {
            override fun getMorphMethod() =
                get<MorphMethodTypeSelector>()
                    .getMorphMethod()
                    .map { morphMethodType ->
                        when (morphMethodType) {
                            MorphMethodType.HomeBrew -> { activity: Activity ->
                                startSwap(activity)
                            }
                            MorphMethodType.Kyc -> { activity: Activity ->
                                KycNavHostActivity.start(activity, CampaignType.Swap)
                            }
                        }
                    }
        } as MorphActivityLauncher
    }
}

private class SwapStarter : StartSwap {

    override fun startSwapActivity(context: Any) {
        startSwap(context as Context)
    }
}

private fun startSwap(context: Context) {
    TradeHistoryActivity.start(context)
}

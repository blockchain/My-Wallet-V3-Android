package com.blockchain.android.testutils

import com.blockchain.testutils.after
import com.blockchain.testutils.before
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.Schedulers

fun rxInit(block: RxInit.() -> Unit) =
    before {
        RxInit().also(block)
    } after {
        RxAndroidPlugins.reset()
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(null)
        RxJavaPlugins.reset()
    }

class RxInit : com.blockchain.testutils.RxInit() {

    fun main(scheduler: Scheduler) {
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { _ -> Schedulers.trampoline() }
        RxAndroidPlugins.setMainThreadSchedulerHandler { _ -> scheduler }
    }

    fun mainTrampoline() {
        main(Schedulers.trampoline())
    }
}

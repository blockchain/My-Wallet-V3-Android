package piuk.blockchain.android.testutils

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

class RxInit {

    fun computation(scheduler: Scheduler) {
        RxJavaPlugins.setComputationSchedulerHandler { _ -> scheduler }
    }

    fun computationTrampoline() {
        computation(Schedulers.trampoline())
    }

    fun io(scheduler: Scheduler) {
        RxJavaPlugins.setIoSchedulerHandler { _ -> scheduler }
    }

    fun ioTrampoline() {
        io(Schedulers.trampoline())
    }

    fun main(scheduler: Scheduler) {
        RxAndroidPlugins.setMainThreadSchedulerHandler { _ -> scheduler }
    }

    fun mainTrampoline() {
        main(Schedulers.trampoline())
    }

    fun single(scheduler: Scheduler) {
        RxJavaPlugins.setSingleSchedulerHandler { _ -> scheduler }
    }

    fun singleTrampoline() {
        single(Schedulers.trampoline())
    }
}

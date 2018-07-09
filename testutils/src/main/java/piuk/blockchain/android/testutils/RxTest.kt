package piuk.blockchain.android.testutils

import android.support.annotation.CallSuper
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.internal.schedulers.TrampolineScheduler
import io.reactivex.plugins.RxJavaPlugins
import io.reactivex.schedulers.TestScheduler
import org.junit.After
import org.junit.Before

/**
 * Class that forces all Rx observables to be subscribed and observed in the same thread through the
 * same Scheduler that runs immediately. Also exposes a [TestScheduler] for testing of
 * time-based methods.
 */
open class RxTest {

    /**
     * Returns a [TestScheduler] object which allows for easy testing of time-based methods
     * that return [io.reactivex.Observable] objects.
     */
    protected val testScheduler: TestScheduler = TestScheduler()

    @Before
    @CallSuper
    @Throws(Exception::class)
    open fun setUp() {
        RxAndroidPlugins.reset()
        RxJavaPlugins.reset()

        RxAndroidPlugins.setInitMainThreadSchedulerHandler { TrampolineScheduler.instance() }

        RxJavaPlugins.setInitIoSchedulerHandler { TrampolineScheduler.instance() }
        RxJavaPlugins.setInitNewThreadSchedulerHandler { TrampolineScheduler.instance() }
        RxJavaPlugins.setInitSingleSchedulerHandler { TrampolineScheduler.instance() }

        RxJavaPlugins.setComputationSchedulerHandler { testScheduler }

        RxJavaPlugins.setErrorHandler { it.printStackTrace() }
    }

    @After
    @CallSuper
    open fun tearDown() {
        RxAndroidPlugins.reset()
        RxJavaPlugins.reset()
    }
}

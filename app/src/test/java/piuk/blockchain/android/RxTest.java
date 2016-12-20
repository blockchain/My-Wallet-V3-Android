package piuk.blockchain.android;

import android.support.annotation.CallSuper;

import org.junit.After;
import org.junit.Before;

import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.internal.schedulers.TrampolineScheduler;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Created by adambennett on 08/08/2016.
 *
 * Class that forces all Rx observables to be subscribed and observed in the same thread
 * through the same Scheduler that runs immediately.
 */
public class RxTest {

    @Before
    @CallSuper
    public void setUp() throws Exception {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();

        RxAndroidPlugins.setInitMainThreadSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());

        RxJavaPlugins.setInitIoSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
        RxJavaPlugins.setInitComputationSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
        RxJavaPlugins.setInitNewThreadSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
    }

    @After
    @CallSuper
    public void tearDown() throws Exception {
        RxAndroidPlugins.reset();
        RxJavaPlugins.reset();
    }
}

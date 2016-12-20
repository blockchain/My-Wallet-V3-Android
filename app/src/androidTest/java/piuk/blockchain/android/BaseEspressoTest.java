package piuk.blockchain.android;

import android.support.test.InstrumentationRegistry;

import piuk.blockchain.android.util.PrefsUtil;

public class BaseEspressoTest {

    /**
     * Clears application state completely for use between tests. Use alongside <code>new
     * ActivityTestRule<>(Activity.class, false, false)</code> and launch activity manually on setup
     * to avoid Espresso starting your activity automatically.
     */
    protected void clearState() {
        new PrefsUtil(InstrumentationRegistry.getTargetContext()).clear();
    }

    protected void ignoreTapJacking() {
        new PrefsUtil(InstrumentationRegistry.getTargetContext()).setValue("OVERLAY_TRUSTED", true);
    }

}

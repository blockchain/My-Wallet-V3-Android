package piuk.blockchain.androidcoreui.ui.base;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.WindowManager;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.utils.PrefsUtil;
import piuk.blockchain.androidcoreui.ApplicationLifeCycle;
import piuk.blockchain.androidcoreui.injector.CoreInjector;

import javax.inject.Inject;

/**
 * A base Activity for all activities which need auth timeouts & screenshot prevention
 */
@SuppressLint("Registered")
public class BaseAuthActivity extends AppCompatActivity {

    @Inject
    protected PrefsUtil prefsUtil;

    {
        // Init objects first
        CoreInjector.getInstance().getAppComponent().inject(this);
    }

    @CallSuper
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lockScreenOrientation();
    }

    /**
     * Allows you to disable Portrait orientation lock on a per-Activity basis.
     */
    protected void lockScreenOrientation() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    /**
     * Applies the title to the {@link Toolbar} which is then set as the Activity's
     * SupportActionBar.
     *
     * @param toolbar The {@link Toolbar} for the current activity
     * @param title   The title for the page, as a StringRes
     */
    public void setupToolbar(Toolbar toolbar, @StringRes int title) {
        setupToolbar(toolbar, getString(title));
    }

    /**
     * Applies the title to the {@link Toolbar} which is then set as the Activity's
     * SupportActionBar.
     *
     * @param toolbar The {@link Toolbar} for the current activity
     * @param title   The title for the page, as a String
     */
    public void setupToolbar(Toolbar toolbar, String title) {
        toolbar.setTitle(title);
        setSupportActionBar(toolbar);
    }

    /**
     * Applies the title to the Activity's {@link ActionBar}. This method is the fragment equivalent
     * of {@link #setupToolbar(Toolbar, int)}.
     *
     * @param actionBar The {@link ActionBar} for the current activity
     * @param title     The title for the page, as a StringRes
     */
    public void setupToolbar(ActionBar actionBar, @StringRes int title) {
        setupToolbar(actionBar, getString(title));
    }

    /**
     * Applies the title to the Activity's {@link ActionBar}. This method is the fragment equivalent
     * of {@link #setupToolbar(Toolbar, int)}.
     *
     * @param actionBar The {@link ActionBar} for the current activity
     * @param title     The title for the page, as a String
     */
    public void setupToolbar(ActionBar actionBar, String title) {
        actionBar.setTitle(title);
    }

    @CallSuper
    @Override
    protected void onResume() {
        super.onResume();
        stopLogoutTimer();
        ApplicationLifeCycle.getInstance().onActivityResumed();

        if (prefsUtil.getValue(PrefsUtil.KEY_SCREENSHOTS_ENABLED, false) && !enforceFlagSecure()) {
            enableScreenshots();
        } else {
            disallowScreenshots();
        }
    }

    /**
     * Allows us to enable screenshots on all pages, unless this is overridden in an Activity and
     * returns true. Some pages are fine to be screenshot, but this lets us keep it permanently
     * disabled on some more sensitive pages.
     *
     * @return False by default. If false, screenshots & screen recording will be allowed on the
     * page if the user so chooses.
     */
    protected boolean enforceFlagSecure() {
        return false;
    }

    @CallSuper
    @Override
    protected void onPause() {
        super.onPause();
        startLogoutTimer();
        ApplicationLifeCycle.getInstance().onActivityPaused();
    }

    /**
     * Starts the logout timer. Override in an activity if timeout is not needed.
     */
    protected void startLogoutTimer() {
        AccessState.getInstance().startLogoutTimer(this);
    }

    private void stopLogoutTimer() {
        AccessState.getInstance().stopLogoutTimer(this);
    }

    private void disallowScreenshots() {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    private void enableScreenshots() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }
}

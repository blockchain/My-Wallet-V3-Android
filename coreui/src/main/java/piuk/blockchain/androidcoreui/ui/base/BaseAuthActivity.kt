package piuk.blockchain.androidcoreui.ui.base

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.support.annotation.CallSuper
import android.view.WindowManager
import com.blockchain.koin.injectActivity
import com.blockchain.ui.password.SecondPasswordHandler
import org.koin.android.ext.android.inject
import piuk.blockchain.androidcore.data.access.LogoutTimer
import piuk.blockchain.androidcore.utils.PrefsUtil
import piuk.blockchain.androidcoreui.ApplicationLifeCycle

/**
 * A base Activity for all activities which need auth timeouts & screenshot prevention
 */
abstract class BaseAuthActivity : ToolBarActivity() {

    private val logoutTimer: LogoutTimer by inject()

    protected val prefsUtil: PrefsUtil by inject()

    protected val secondPasswordHandler: SecondPasswordHandler by injectActivity()

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lockScreenOrientation()
    }

    /**
     * Allows you to disable Portrait orientation lock on a per-Activity basis.
     */
    protected open fun lockScreenOrientation() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    @CallSuper
    override fun onResume() {
        super.onResume()
        stopLogoutTimer()
        ApplicationLifeCycle.getInstance().onActivityResumed()

        if (prefsUtil.getValue(PrefsUtil.KEY_SCREENSHOTS_ENABLED, false) && !enforceFlagSecure()) {
            enableScreenshots()
        } else {
            disallowScreenshots()
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
    protected open fun enforceFlagSecure(): Boolean {
        return false
    }

    @CallSuper
    override fun onPause() {
        super.onPause()
        startLogoutTimer()
        ApplicationLifeCycle.getInstance().onActivityPaused()
    }

    /**
     * Starts the logout timer. Override in an activity if timeout is not needed.
     */
    protected open fun startLogoutTimer() {
        logoutTimer.start(this)
    }

    private fun stopLogoutTimer() {
        logoutTimer.stop(this)
    }

    private fun disallowScreenshots() {
        window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    private fun enableScreenshots() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}

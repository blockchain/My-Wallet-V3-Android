package piuk.blockchain.androidcoreui.ui.base;

import android.support.annotation.CallSuper;
import android.support.v4.view.ViewPager;
import com.crashlytics.android.answers.ContentViewEvent;
import piuk.blockchain.androidcoreui.BuildConfig;
import piuk.blockchain.androidcoreui.utils.logging.Logging;

/**
 * Logs Fragments that have been visited for statistics purposes using Crashlytics' answers.
 */
public abstract class BaseFragment<VIEW extends View, PRESENTER extends BasePresenter<VIEW>>
        extends BaseMvpFragment<VIEW, PRESENTER> {

    private boolean logged;

    @CallSuper
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        /* Ensure that pages are only logged as being seen if they are actually visible, and only
         * once. This is important for fragments in ViewPagers where they might be instantiated, but
         * not actually visible or being accessed. For example: Swipe to receive.
         *
         *  Note that this isn't triggered if a Fragment isn't in a ViewPager */
        if (isVisibleToUser && !logged) {
            logged = true;
            if (!BuildConfig.DEBUG) {
                Logging.INSTANCE.logContentView(new ContentViewEvent()
                        .putContentName(getClass().getSimpleName()));
            }
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @CallSuper
    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null
                && getView().getParent() != null
                && getView().getParent() instanceof ViewPager) {
            /* In ViewPager, don't log here as Fragment might not be visible. Use setUserVisibleHint
             * to log in these situations. */
        } else {
            if (!logged) {
                logged = true;
                if (!BuildConfig.DEBUG) {
                    Logging.INSTANCE.logContentView(new ContentViewEvent()
                            .putContentName(getClass().getSimpleName()));
                }
            }
        }
    }
}


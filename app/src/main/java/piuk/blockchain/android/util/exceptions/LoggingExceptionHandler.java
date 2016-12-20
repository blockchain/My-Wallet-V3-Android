package piuk.blockchain.android.util.exceptions;

import javax.inject.Inject;

import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.util.AppUtil;

/**
 * Created by adambennett on 10/08/2016.
 */

@SuppressWarnings("WeakerAccess")
public class LoggingExceptionHandler implements Thread.UncaughtExceptionHandler {

    private final Thread.UncaughtExceptionHandler mRootHandler;
    @Inject protected AppUtil mAppUtil;

    public LoggingExceptionHandler() {
        Injector.getInstance().getAppComponent().inject(this);
        mRootHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        mAppUtil.restartApp();

        // Re-throw the exception so that the system can fail as it normally would, and so that
        // Firebase can log the exception automatically
        mRootHandler.uncaughtException(thread, throwable);
    }
}

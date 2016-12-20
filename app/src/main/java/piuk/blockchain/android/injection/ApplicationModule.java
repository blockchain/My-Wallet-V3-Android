package piuk.blockchain.android.injection;

import android.app.Application;
import android.content.Context;

import info.blockchain.api.PersistentUrls;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.util.PrivateKeyFactory;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

/**
 * Created by adambennett on 08/08/2016.
 */

@Module
public class ApplicationModule {

    private final Application mApplication;

    public ApplicationModule(Application application) {
        mApplication = application;
    }

    @Provides
    @Singleton
    protected Context provideApplicationContext() {
        return mApplication;
    }

    @Provides
    @Singleton
    protected PrefsUtil providePrefsUtil() {
        return new PrefsUtil(mApplication);
    }

    @Provides
    @Singleton
    protected AppUtil provideAppUtil() {
        return new AppUtil(mApplication);
    }

    @Provides
    protected AccessState provideAccessState() {
        return AccessState.getInstance();
    }

    @Provides
    protected AESUtilWrapper provideAesUtils() {
        return new AESUtilWrapper();
    }

    @Provides
    protected StringUtils provideStringUtils() {
        return new StringUtils(mApplication);
    }

    @Provides
    protected ExchangeRateFactory provideExchangeRateFactory() {
        return ExchangeRateFactory.getInstance();
    }

    @Provides
    protected MultiAddrFactory provideMultiAddrFactory() {
        return MultiAddrFactory.getInstance();
    }

    @Provides
    protected PrivateKeyFactory privateKeyFactory() {
        return new PrivateKeyFactory();
    }

    @Provides
    protected PersistentUrls providePersistentUrls() {
        return PersistentUrls.getInstance();
    }
}

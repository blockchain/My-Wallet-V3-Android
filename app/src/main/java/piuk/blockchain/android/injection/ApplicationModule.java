package piuk.blockchain.android.injection;

import com.blockchain.koin.KoinDaggerModule;
import com.google.firebase.iid.FirebaseInstanceId;

import android.app.NotificationManager;
import android.content.Context;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.PrivateKeyFactory;

import java.util.Locale;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.notifications.NotificationService;
import piuk.blockchain.android.data.notifications.NotificationTokenManager;
import piuk.blockchain.android.util.PrngHelper;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.data.api.EnvironmentConfig;
import com.blockchain.network.EnvironmentUrls;
import piuk.blockchain.androidcore.data.currency.CurrencyState;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.utils.PrefsUtil;
import piuk.blockchain.androidcore.utils.PrngFixer;

@Module
public class ApplicationModule extends KoinDaggerModule {

    @Provides
    AccessState provideAccessState() {
        return AccessState.getInstance();
    }

    @Provides
    PrivateKeyFactory privateKeyFactory() {
        return new PrivateKeyFactory();
    }

    @Provides
    @Singleton
    NotificationManager provideNotificationManager(Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @Provides
    CurrencyState provideCurrencyState() {
        return CurrencyState.getInstance();
    }

    @Provides
    Locale provideLocale() {
        return Locale.getDefault();
    }

    @Provides
    @Named("explorer-url")
    String provideExplorerUrl(EnvironmentConfig environmentSettings) {
        return environmentSettings.getExplorerUrl();
    }

    @Provides
    protected PayloadManager providePayloadManager() {
        return PayloadManager.getInstance();
    }

    @Provides
    @Singleton
    protected NotificationTokenManager provideNotificationTokenManager(PayloadManager payloadManager,
                                                                       PrefsUtil prefsUtil,
                                                                       RxBus rxBus,
                                                                       WalletApi walletApi) {

        return new NotificationTokenManager(
                new NotificationService(walletApi),
                payloadManager,
                prefsUtil,
                FirebaseInstanceId.getInstance(),
                rxBus);
    }

    @Provides
    protected EnvironmentConfig provideEnvironmentConfig() {
        return get(EnvironmentConfig.class);
    }

    @Provides
    protected EnvironmentUrls provideEnvironmentUrls() {
        return get(EnvironmentUrls.class);
    }

    @Provides
    @Singleton
    protected PrngFixer providePrngFixer(Context context, AccessState accessState) {
        return new PrngHelper(context, accessState);
    }
}

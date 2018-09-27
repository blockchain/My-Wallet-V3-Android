package piuk.blockchain.android.injection;

import android.app.NotificationManager;
import android.content.Context;
import com.blockchain.koin.KoinDaggerModule;
import com.blockchain.koin.modules.MorphActivityLauncher;
import com.blockchain.kyc.datamanagers.nabu.NabuDataManager;
import com.blockchain.kycui.settings.KycStatusHelper;
import com.blockchain.network.EnvironmentUrls;
import com.blockchain.notifications.NotificationTokenManager;
import dagger.Module;
import dagger.Provides;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.PayloadManagerWiper;
import info.blockchain.wallet.util.PrivateKeyFactory;
import piuk.blockchain.android.util.PrngHelper;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.data.api.EnvironmentConfig;
import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager;
import piuk.blockchain.androidcore.data.currency.CurrencyState;
import piuk.blockchain.androidcore.data.ethereum.EthDataManager;
import piuk.blockchain.androidcore.data.fees.FeeDataManager;
import piuk.blockchain.androidcore.data.metadata.MetadataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.payments.SendDataManager;
import piuk.blockchain.androidcore.utils.PrngFixer;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Locale;

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
    NotificationManager provideNotificationManager() {
        return get(NotificationManager.class);
    }

    @Provides
    CurrencyState provideCurrencyState() {
        return get(CurrencyState.class);
    }

    @Provides
    Locale provideLocale() {
        return Locale.getDefault();
    }

    @Provides
    @Named("explorer-url")
    String provideExplorerUrl() {
        return get(String.class, "explorer-url");
    }

    @Provides
    PayloadDataManager providePayloadDataManager() {
        return get(PayloadDataManager.class);
    }

    @Provides
    protected PayloadManager providePayloadManager() {
        return get(PayloadManager.class);
    }

    @Provides
    protected PayloadManagerWiper providePayloadManagerWiper() {
        return get(PayloadManagerWiper.class);
    }

    @Provides
    protected NotificationTokenManager provideNotificationTokenManager() {
        return get(NotificationTokenManager.class);
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

    @Provides
    NabuDataManager provideNabuDataManager() {
        return get(NabuDataManager.class);
    }

    @Provides
    MorphActivityLauncher provideMorphActivityLauncher() {
        return get(MorphActivityLauncher.class);
    }

    @Provides
    KycStatusHelper provideKycStatusHelper() {
        return get(KycStatusHelper.class);
    }

    @Provides
    SendDataManager provideSendDataManager() {
        return get(SendDataManager.class);
    }

    @Provides
    BchDataManager provideBchDataManager() {
        return get(BchDataManager.class);
    }

    @Provides
    EthDataManager provideEthDataManager() {
        return get(EthDataManager.class);
    }

    @Provides
    FeeDataManager provideFeeDataManager() {
        return get(FeeDataManager.class);
    }

    @Provides
    MetadataManager provideMetadataManager() {
        return get(MetadataManager.class);
    }


}

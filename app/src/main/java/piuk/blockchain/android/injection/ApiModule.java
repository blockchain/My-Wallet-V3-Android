package piuk.blockchain.android.injection;

import info.blockchain.api.Notifications;
import info.blockchain.api.PersistentUrls;
import info.blockchain.wallet.payload.PayloadManager;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.OkHttpClient;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.api.ApiInterceptor;
import piuk.blockchain.android.data.notifications.NotificationTokenManager;
import piuk.blockchain.android.data.services.NotificationService;
import piuk.blockchain.android.data.stores.TransactionListStore;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

/**
 * Created by adambennett on 08/08/2016.
 */

@Module
public class ApiModule {

    private static final int API_TIMEOUT = 15;

    @Provides
    protected PayloadManager providePayloadManager() {
        return PayloadManager.getInstance();
    }

    @Provides
    @Singleton
    protected TransactionListStore provideTransactionListStore() {
        return new TransactionListStore();
    }

    @Provides
    @Singleton
    protected NotificationTokenManager provideNotificationTokenManager(AccessState accessState,
                                                                       PayloadManager payloadManager) {

        return new NotificationTokenManager(
                new NotificationService(new Notifications()), accessState, payloadManager);
    }

    @Provides
    @Singleton
    protected OkHttpClient provideOkHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .addInterceptor(new ApiInterceptor())
                .build();
    }

    @Provides
    @Singleton
    protected JacksonConverterFactory provideJacksonConverterFactory() {
        return JacksonConverterFactory.create();
    }

    @Provides
    @Singleton
    @Named("api")
    protected Retrofit provideRetrofitApiInstance(OkHttpClient okHttpClient, JacksonConverterFactory converterFactory) {
        return new Retrofit.Builder()
                .baseUrl(PersistentUrls.getInstance().getCurrentBaseApiUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .build();
    }

    @Provides
    @Singleton
    @Named("server")
    protected Retrofit provideRetrofitBlockchainInstance(OkHttpClient okHttpClient, JacksonConverterFactory converterFactory) {
        return new Retrofit.Builder()
                .baseUrl(PersistentUrls.getInstance().getCurrentBaseServerUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .build();
    }

}

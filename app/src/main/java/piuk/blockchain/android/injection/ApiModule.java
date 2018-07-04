package piuk.blockchain.android.injection;


import android.os.Build;

import com.squareup.moshi.Moshi;

import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.shapeshift.ShapeShiftUrls;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import javax.inject.Named;
import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import okhttp3.CertificatePinner;
import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import piuk.blockchain.androidbuysell.models.coinify.BuyFrequencyAdapter;
import piuk.blockchain.androidbuysell.models.coinify.CannotTradeReasonAdapter;
import piuk.blockchain.androidbuysell.models.coinify.DetailsAdapter;
import piuk.blockchain.androidbuysell.models.coinify.GrantTypeAdapter;
import piuk.blockchain.androidbuysell.models.coinify.MediumAdapter;
import piuk.blockchain.androidbuysell.models.coinify.ReviewStateAdapter;
import piuk.blockchain.androidbuysell.models.coinify.TradeStateAdapter;
import piuk.blockchain.androidbuysell.models.coinify.TransferStateAdapter;
import piuk.blockchain.androidcore.BuildConfig;
import piuk.blockchain.androidcore.data.api.ConnectionApi;
import piuk.blockchain.androidcore.data.api.EnvironmentConfig;
import piuk.blockchain.androidcore.data.api.interceptors.ApiInterceptor;
import piuk.blockchain.androidcore.data.api.interceptors.UserAgentInterceptor;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.utils.SSLVerifyUtil;
import piuk.blockchain.androidcore.utils.TLSSocketFactory;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;
import timber.log.Timber;

@SuppressWarnings("WeakerAccess")
@Module
public class ApiModule {

    private static final int API_TIMEOUT = 30;
    private static final int PING_INTERVAL = 10;

    @Provides
    @Singleton
    protected OkHttpClient provideOkHttpClient() {
        CertificatePinner certificatePinner = new CertificatePinner.Builder()
                .add("api.blockchain.info", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
                .add("blockchain.info", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
                .add("blockchain.com", "sha256/Z87j23nY+/WSTtsgE/O4ZcDVhevBohFPgPMU6rV2iSw=")
                .build();

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectionSpecs(Collections.singletonList(ConnectionSpec.MODERN_TLS))
                .connectTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(API_TIMEOUT, TimeUnit.SECONDS)
                .pingInterval(PING_INTERVAL, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .certificatePinner(certificatePinner)
                // Add logging for debugging purposes
                .addInterceptor(new ApiInterceptor())
                // Add header in all requests
                .addInterceptor(new UserAgentInterceptor(BuildConfig.VERSION_NAME, Build.VERSION.RELEASE));

        /*
          Enable TLS specific version V.1.2
          Issue Details : https://github.com/square/okhttp/issues/1934
         */
        try {
            TLSSocketFactory tlsSocketFactory = new TLSSocketFactory();
            builder.sslSocketFactory(tlsSocketFactory, tlsSocketFactory.systemDefaultTrustManager());
        } catch (KeyManagementException | NoSuchAlgorithmException e) {
            Timber.e(e, "Failed to create Socket connection ");
        }

        return builder.build();
    }

    @Provides
    @Singleton
    protected JacksonConverterFactory provideJacksonConverterFactory() {
        return JacksonConverterFactory.create();
    }

    @Provides
    @Singleton
    protected MoshiConverterFactory provideMoshiConverterFactory() {
        Moshi moshi = new Moshi.Builder()
                .add(new CannotTradeReasonAdapter())
                .add(new ReviewStateAdapter())
                .add(new MediumAdapter())
                .add(new TradeStateAdapter())
                .add(new TransferStateAdapter())
                .add(new DetailsAdapter())
                .add(new GrantTypeAdapter())
                .add(new BuyFrequencyAdapter())
                .build();
        return MoshiConverterFactory.create(moshi);
    }

    @Provides
    @Singleton
    protected RxJava2CallAdapterFactory provideRxJavaCallAdapterFactory() {
        return RxJava2CallAdapterFactory.create();
    }

    @Provides
    @Singleton
    @Named("api")
    protected Retrofit provideRetrofitApiInstance(OkHttpClient okHttpClient,
                                                  JacksonConverterFactory converterFactory,
                                                  RxJava2CallAdapterFactory rxJavaCallFactory,
                                                  EnvironmentConfig environmentSettings) {

        return new Retrofit.Builder()
                .baseUrl(environmentSettings.getApiUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(rxJavaCallFactory)
                .build();
    }

    @Provides
    @Singleton
    @Named("explorer")
    protected Retrofit provideRetrofitExplorerInstance(OkHttpClient okHttpClient,
                                                       JacksonConverterFactory converterFactory,
                                                       RxJava2CallAdapterFactory rxJavaCallFactory,
                                                       EnvironmentConfig environmentSettings) {
        return new Retrofit.Builder()
                .baseUrl(environmentSettings.getExplorerUrl())
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(rxJavaCallFactory)
                .build();
    }

    @Provides
    @Singleton
    protected SSLVerifyUtil provideSSlVerifyUtil(@Named("explorer") Retrofit retrofit,
                                                 RxBus rxBus) {

        return new SSLVerifyUtil(rxBus, new ConnectionApi(retrofit));
    }

    @Provides
    @Singleton
    @Named("shapeshift")
    protected Retrofit provideRetrofitShapeShiftInstance(OkHttpClient okHttpClient,
                                                         JacksonConverterFactory converterFactory,
                                                         RxJava2CallAdapterFactory rxJavaCallFactory) {
        return new Retrofit.Builder()
                .baseUrl(ShapeShiftUrls.SHAPESHIFT_URL)
                .client(okHttpClient)
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(rxJavaCallFactory)
                .build();
    }

    /**
     * This instance converts to Kotlin data classes ONLY; it will break if used to parse data models
     * written with Java + Jackson.
     */
    @Provides
    @Singleton
    @Named("kotlin")
    protected Retrofit provideRetrofitKotlinInstance(OkHttpClient okHttpClient,
                                                      MoshiConverterFactory converterFactory,
                                                      RxJava2CallAdapterFactory rxJavaCallFactory,
                                                      EnvironmentConfig environmentSettings) {
        return new Retrofit.Builder()
                .client(okHttpClient)
                .baseUrl(environmentSettings.getExplorerUrl())
                .addConverterFactory(converterFactory)
                .addCallAdapterFactory(rxJavaCallFactory)
                .build();
    }

    @Provides
    @Singleton
    protected BlockExplorer provideBlockExplorer(@Named("explorer") Retrofit explorerRetrofit,
                                                 @Named("api") Retrofit apiRetrofit) {
        return new BlockExplorer(explorerRetrofit,
                apiRetrofit,
                BlockchainFramework.getApiCode());
    }
}

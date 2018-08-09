package piuk.blockchain.android.injection;

import com.blockchain.koin.KoinDaggerModule;
import com.blockchain.network.EnvironmentUrls;
import com.squareup.moshi.Moshi;
import dagger.Module;
import dagger.Provides;
import info.blockchain.api.blockexplorer.BlockExplorer;
import info.blockchain.wallet.BlockchainFramework;
import okhttp3.OkHttpClient;
import piuk.blockchain.androidbuysell.models.coinify.BuyFrequencyAdapter;
import piuk.blockchain.androidbuysell.models.coinify.CannotTradeReasonAdapter;
import piuk.blockchain.androidbuysell.models.coinify.DetailsAdapter;
import piuk.blockchain.androidbuysell.models.coinify.GrantTypeAdapter;
import piuk.blockchain.androidbuysell.models.coinify.MediumAdapter;
import piuk.blockchain.androidbuysell.models.coinify.ReviewStateAdapter;
import piuk.blockchain.androidbuysell.models.coinify.TradeStateAdapter;
import piuk.blockchain.androidbuysell.models.coinify.TransferStateAdapter;
import piuk.blockchain.androidcore.data.api.ConnectionApi;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.utils.SSLVerifyUtil;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;
import retrofit2.converter.moshi.MoshiConverterFactory;

import javax.inject.Named;
import javax.inject.Singleton;

@SuppressWarnings("WeakerAccess")
@Module
public class ApiModule extends KoinDaggerModule {

    @Provides
    protected OkHttpClient provideOkHttpClient() {
        return get(OkHttpClient.class);
    }

    @Provides
    protected JacksonConverterFactory provideJacksonConverterFactory() {
        return get(JacksonConverterFactory.class);
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
    protected RxJava2CallAdapterFactory provideRxJavaCallAdapterFactory() {
        return get(RxJava2CallAdapterFactory.class);
    }

    @Provides
    @Named("api")
    protected Retrofit provideRetrofitApiInstance() {
        return get(Retrofit.class, "api");
    }

    @Provides
    @Named("explorer")
    protected Retrofit provideRetrofitExplorerInstance() {
        return get(Retrofit.class, "explorer");
    }

    @Provides
    @Singleton
    protected SSLVerifyUtil provideSSlVerifyUtil(@Named("explorer") Retrofit retrofit,
                                                 RxBus rxBus) {

        return new SSLVerifyUtil(rxBus, new ConnectionApi(retrofit));
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
                                                     EnvironmentUrls environmentSettings) {
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

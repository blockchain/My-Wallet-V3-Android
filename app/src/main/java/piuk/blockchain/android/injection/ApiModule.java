package piuk.blockchain.android.injection;

import com.blockchain.koin.KoinDaggerModule;
import dagger.Module;
import dagger.Provides;
import info.blockchain.api.blockexplorer.BlockExplorer;
import okhttp3.OkHttpClient;
import piuk.blockchain.androidcore.data.api.ConnectionApi;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.utils.SSLVerifyUtil;
import retrofit2.Retrofit;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
public class ApiModule extends KoinDaggerModule {

    @Provides
    protected OkHttpClient provideOkHttpClient() {
        return get(OkHttpClient.class);
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
    @Named("kotlin")
    protected Retrofit provideRetrofitKotlinInstance() {
        return get(Retrofit.class, "kotlin");
    }

    @Provides
    protected BlockExplorer provideBlockExplorer() {
        return get(BlockExplorer.class);
    }
}

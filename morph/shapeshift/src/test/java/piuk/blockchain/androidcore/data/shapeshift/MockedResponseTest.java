package piuk.blockchain.androidcore.data.shapeshift;

import info.blockchain.wallet.BlockchainFramework;
import io.reactivex.internal.schedulers.TrampolineScheduler;
import io.reactivex.plugins.RxJavaPlugins;
import okhttp3.OkHttpClient;
import org.junit.After;
import org.junit.Before;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public abstract class MockedResponseTest {

    MockInterceptor mockInterceptor = new MockInterceptor();

    final OkHttpClient okHttpClient = getOkHttpClient();

    @Before
    public void setupRxCalls() {
        RxJavaPlugins.reset();

        RxJavaPlugins.setIoSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
        RxJavaPlugins.setComputationSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
        RxJavaPlugins.setNewThreadSchedulerHandler(schedulerCallable -> TrampolineScheduler.instance());
    }

    @After
    public void tearDownRxCalls() {
        RxJavaPlugins.reset();
        BlockchainFramework.init(null);
    }

    private OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(mockInterceptor)
                .addInterceptor(new ApiInterceptor())
                .build();
    }

    Retrofit getRetrofit(String url, OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }
}
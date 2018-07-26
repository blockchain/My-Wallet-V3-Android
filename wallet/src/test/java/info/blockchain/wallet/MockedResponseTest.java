package info.blockchain.wallet;

import info.blockchain.wallet.api.Environment;
import io.reactivex.Scheduler;
import io.reactivex.functions.Function;
import io.reactivex.internal.schedulers.TrampolineScheduler;
import io.reactivex.plugins.RxJavaPlugins;
import okhttp3.OkHttpClient;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.BitcoinCashMainNetParams;
import org.bitcoinj.params.BitcoinMainNetParams;
import org.junit.After;
import org.junit.Before;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.jackson.JacksonConverterFactory;

public abstract class MockedResponseTest {

    public MockInterceptor mockInterceptor = MockInterceptor.getInstance();

    private final OkHttpClient okHttpClient = getOkHttpClient();

    @Before
    public void initBlockchainFramework() {
        BlockchainFramework.init(frameworkInterface);
    }

    protected FrameworkInterface frameworkInterface = new FrameworkInterface() {

        @Override
        public Retrofit getRetrofitApiInstance() {
            return getRetrofit("https://api.staging.blockchain.info/", okHttpClient);
        }

        @Override
        public Retrofit getRetrofitExplorerInstance() {
            return getRetrofit("https://explorer.staging.blockchain.info/", okHttpClient);
        }

        @Override
        public Environment getEnvironment() {
            return Environment.STAGING;
        }

        @Override
        public NetworkParameters getBitcoinParams() {
            return BitcoinMainNetParams.get();
        }

        @Override
        public NetworkParameters getBitcoinCashParams() {
            return BitcoinCashMainNetParams.get();
        }

        @Override
        public String getApiCode() {
            return null;
        }

        @Override
        public String getDevice() {
            return "UnitTest";
        }

        @Override
        public String getAppVersion() {
            return null;
        }
    };

    @Before
    public void setupRxCalls() {
        RxJavaPlugins.reset();

        RxJavaPlugins.setIoSchedulerHandler(new Function<Scheduler, Scheduler>() {
            @Override
            public Scheduler apply(Scheduler schedulerCallable) {
                return TrampolineScheduler.instance();
            }
        });
        RxJavaPlugins.setComputationSchedulerHandler(new Function<Scheduler, Scheduler>() {
            @Override
            public Scheduler apply(Scheduler schedulerCallable) {
                return TrampolineScheduler.instance();
            }
        });
        RxJavaPlugins.setNewThreadSchedulerHandler(new Function<Scheduler, Scheduler>() {
            @Override
            public Scheduler apply(Scheduler schedulerCallable) {
                return TrampolineScheduler.instance();
            }
        });
    }

    @After
    public void tearDownRxCalls() {
        RxJavaPlugins.reset();
        BlockchainFramework.init(null);
    }

    private OkHttpClient getOkHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(mockInterceptor)//Mock responses
                .addInterceptor(new ApiInterceptor())//Extensive logging
                .build();
    }

    private Retrofit getRetrofit(String url, OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(url)
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .build();
    }
}
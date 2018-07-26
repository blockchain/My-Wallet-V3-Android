package info.blockchain.wallet;

import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.shapeshift.ShapeShiftUrls;
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

import java.util.concurrent.Callable;

public abstract class MockedResponseTest {

    public MockInterceptor mockInterceptor = MockInterceptor.getInstance();

    @Before
    public void initBlockchainFramework() {
        BlockchainFramework.init(frameworkInterface);
    }

    private FrameworkInterface frameworkInterface = new FrameworkInterface() {
        private final OkHttpClient okHttpClient = getOkHttpClient();

        @Override
        public Retrofit getRetrofitApiInstance() {
            return getRetrofit("https://api.staging.blockchain.info/", okHttpClient);
        }

        @Override
        public Retrofit getRetrofitExplorerInstance() {
            return getRetrofit("https://explorer.staging.blockchain.info/", okHttpClient);
        }

        @Override
        public Retrofit getRetrofitShapeShiftInstance() {
            return getRetrofit(ShapeShiftUrls.SHAPESHIFT_URL, okHttpClient);
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

        RxJavaPlugins.setInitIoSchedulerHandler(new Function<Callable<Scheduler>, Scheduler>() {
            @Override
            public Scheduler apply(Callable<Scheduler> schedulerCallable) {
                return TrampolineScheduler.instance();
            }
        });
        RxJavaPlugins.setInitComputationSchedulerHandler(new Function<Callable<Scheduler>, Scheduler>() {
            @Override
            public Scheduler apply(Callable<Scheduler> schedulerCallable) {
                return TrampolineScheduler.instance();
            }
        });
        RxJavaPlugins.setInitNewThreadSchedulerHandler(new Function<Callable<Scheduler>, Scheduler>() {
            @Override
            public Scheduler apply(Callable<Scheduler> schedulerCallable) {
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
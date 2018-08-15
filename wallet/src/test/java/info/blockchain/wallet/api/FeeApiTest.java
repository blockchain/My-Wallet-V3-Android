package info.blockchain.wallet.api;

import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.MockedResponseTest;
import info.blockchain.wallet.api.data.FeeOptions;
import io.reactivex.observers.TestObserver;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class FeeApiTest extends MockedResponseTest {

    private FeeApi feeApi;

    @Before
    public void setup(){
        feeApi = new FeeApi(BlockchainFramework.getRetrofitApiInstance().create(FeeEndpoints.class));
    }

    @Test
    public void getFeeOptions() throws Exception {

        URI uri = getClass().getClassLoader().getResource("fee_options.txt").toURI();
        String feeOptions = new String(Files.readAllBytes(Paths.get(uri)), Charset.forName("utf-8"));

        mockInterceptor.setResponseString(feeOptions);
        mockInterceptor.setResponseCode(200);
        final TestObserver<FeeOptions> testObserver = feeApi.getFeeOptions().test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        FeeOptions feeOptionsList = testObserver.values().get(0);

        Assert.assertEquals(264, feeOptionsList.getPriorityFee());
        Assert.assertEquals(197, feeOptionsList.getRegularFee());
        Assert.assertEquals(396, feeOptionsList.getLimits().getMax());
        Assert.assertEquals(98, feeOptionsList.getLimits().getMin());
    }

    @Test
    public void getEthFeeOptions() throws Exception {

        URI uri = getClass().getClassLoader().getResource("fee_eth_options.txt").toURI();
        String feeOptions = new String(Files.readAllBytes(Paths.get(uri)), Charset.forName("utf-8"));

        mockInterceptor.setResponseString(feeOptions);
        mockInterceptor.setResponseCode(200);
        final TestObserver<FeeOptions> testObserver = feeApi.getEthFeeOptions().test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        FeeOptions feeOptionsList = testObserver.values().get(0);

        Assert.assertEquals(21000, feeOptionsList.getGasLimit());
        Assert.assertEquals(21, feeOptionsList.getPriorityFee());
        Assert.assertEquals(21, feeOptionsList.getRegularFee());
        Assert.assertEquals(21, feeOptionsList.getLimits().getMax());
        Assert.assertEquals(21, feeOptionsList.getLimits().getMin());
    }
}
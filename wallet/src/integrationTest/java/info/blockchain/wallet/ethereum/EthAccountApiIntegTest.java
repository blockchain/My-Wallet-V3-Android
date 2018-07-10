package info.blockchain.wallet.ethereum;

import info.blockchain.wallet.BaseIntegTest;
import info.blockchain.wallet.ethereum.data.EthAddressResponseMap;
import info.blockchain.wallet.ethereum.data.EthTxDetails;

import org.junit.Test;

import java.util.Arrays;

import io.reactivex.observers.TestObserver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EthAccountApiIntegTest extends BaseIntegTest {

    private EthAccountApi accountApi = new EthAccountApi();

    @Test
    public void getEthAddress() {
        final TestObserver<EthAddressResponseMap> testObserver =
                accountApi.getEthAddress(
                        Arrays.asList("0xccc1dd86df371ecc3ea28a6877fd2301a09effd0",
                                "0xF85608F8fe3887Dab333Ec250A972C1DC19C52B3"))
                        .test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertNotNull(testObserver.values().get(0));
        assertEquals(2, testObserver.values().get(0).getEthAddressResponseMap().values().size());
    }

    /**
     * We just ensure that the endpoint is reachable and that form data is in correct format not to cause a 500 error.
     */
    @Test
    public void pushTx() {

        final TestObserver<String> testObserver =
            accountApi.pushTx("0xf86b808504e3b2920082520894ccc1dd86df371ecc3ea28a6877fd2301a09effd08701ad3aca3b0dee801ba039415ed7f464bee5cd36ee46410f8803fc941e7b2a453e62653f6e52fece179fa004c647a056aafe86ecfd50c345a7bb74da12ba716caf8e5043883f38596ed70a").test();

        //Tx already submitted = "message" : "Transaction with the same hash was already imported."
        testObserver.assertErrorMessage("HTTP 400 Bad Request");
    }

    @Test
    public void getTransactionFromHash() {
        // Arrange
        final String hash = "0xcc6952c8f5c6e90d1addcaf3717b6df251982637f0cafc32c7f6348018dd2a7b";
        // Act
        final TestObserver<EthTxDetails> testObserver = accountApi.getTransaction(hash).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        final EthTxDetails ethTxDetails = testObserver.values().get(0);
        assertEquals(hash, ethTxDetails.getHash());
    }
}

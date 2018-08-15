package info.blockchain.wallet;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.WalletExplorerEndpoints;
import org.junit.After;
import org.junit.Before;

public abstract class WalletApiMockedResponseTest extends MockedResponseTest {

    protected WalletApi walletApi;

    @Before
    public void setWalletApiAccess() {
        walletApi = new WalletApi(
                BlockchainFramework.getRetrofitExplorerInstance()
                        .create(WalletExplorerEndpoints.class)
        );
    }

    @After
    public void clearWalletApiAccess() {
        walletApi = null;
    }
}

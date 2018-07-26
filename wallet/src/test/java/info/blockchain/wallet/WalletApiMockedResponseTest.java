package info.blockchain.wallet;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.WalletApiAccess;
import info.blockchain.wallet.api.WalletApiEndpoints;
import info.blockchain.wallet.api.WalletExplorerEndpoints;
import org.junit.After;
import org.junit.Before;

public abstract class WalletApiMockedResponseTest extends MockedResponseTest {

    protected WalletApi walletApi;

    @Before
    public void setWalletApiAccess() {
        walletApi = new WalletApi(
                BlockchainFramework.getRetrofitApiInstance().
                        create(WalletApiEndpoints.class),
                BlockchainFramework.getRetrofitExplorerInstance()
                        .create(WalletExplorerEndpoints.class)
        );
        WalletApiAccess.INSTANCE.setWalletApi(walletApi);
    }

    @After
    public void clearWalletApiAccess() {
        WalletApiAccess.INSTANCE.setWalletApi(null);
        walletApi = null;
    }
}

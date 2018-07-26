package info.blockchain.wallet.api;

/**
 * @deprecated Inject WalletApi directly.
 * TODO: Remove this once as part of AND-1301 and AND-1194 which should remove the last accesses of this.
 */
public enum WalletApiAccess {
    INSTANCE;

    private WalletApi walletApi;

    public WalletApi getWalletApi() {
        if (walletApi == null) {
            throw new RuntimeException("setWalletApi was not called first");
        }
        return walletApi;
    }

    public void setWalletApi(WalletApi walletApi) {
        this.walletApi = walletApi;
    }
}

package info.blockchain.wallet;

import info.blockchain.wallet.api.Environment;
import org.bitcoinj.core.NetworkParameters;
import retrofit2.Retrofit;

public interface FrameworkInterface {

    /**
     * Provides an instance of Retrofit with it's base URL set to {@link
     * info.blockchain.wallet.api.PersistentUrls#API_URL} or its debug/staging/testnet equivalent
     */
    Retrofit getRetrofitApiInstance();

    /**
     * Provides an instance of Retrofit with it's base URL set to {@link
     * info.blockchain.wallet.api.PersistentUrls#EXPLORER_URL} or its debug/staging/testnet equivalent
     */
    Retrofit getRetrofitExplorerInstance();

    /**
     * Provides the currently chosen environment, as dictated by the top-level app
     */
    Environment getEnvironment();

    /**
     * Provides the currently chosen Network Parameters for Bitcoin, as dictated by the top-level app
     */
    NetworkParameters getBitcoinParams();

    /**
     * Provides the currently chosen Network Parameters for Bitcoin Cash, as dictated by the top-level app
     */
    NetworkParameters getBitcoinCashParams();

    /**
     * Provides an ApiCode used for bci platform usage statistics.
     */
    String getApiCode();

    /*
     * Provides device/platform name for analytical use in wallet payload.
     * Flags wallets and addresses as created on a certain platform - for issue debugging.
     */
    String getDevice();

    /*
     * Provides app version for analytical use in wallet payload.
     * Flags wallets and addresses as created on a certain app version - for issue debugging.
     */
    String getAppVersion();
}

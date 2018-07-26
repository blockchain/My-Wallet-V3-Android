package info.blockchain.wallet;

import info.blockchain.wallet.api.Environment;
import org.bitcoinj.core.NetworkParameters;
import retrofit2.Retrofit;

/**
 * Class for initializing an instance of the HDWallet JAR
 *
 * TODO: Remove me and replace with Injection framework
 */
public final class BlockchainFramework {

    private static FrameworkInterface blockchainInterface;

    public static void init(FrameworkInterface frameworkInterface) {
        blockchainInterface = frameworkInterface;
    }

    @Deprecated
    public static Retrofit getRetrofitApiInstance() {
        return blockchainInterface.getRetrofitApiInstance();
    }

    @Deprecated
    public static Retrofit getRetrofitExplorerInstance() {
        return blockchainInterface.getRetrofitExplorerInstance();
    }

    @Deprecated
    public static Environment getEnvironment() {
        return blockchainInterface.getEnvironment();
    }

    @Deprecated
    public static NetworkParameters getBitcoinParams() {
        return blockchainInterface.getBitcoinParams();
    }

    @Deprecated
    public static NetworkParameters getBitcoinCashParams() {
        return blockchainInterface.getBitcoinCashParams();
    }

    public static String getApiCode() {
        return blockchainInterface.getApiCode();
    }

    public static String getDevice() {
        return blockchainInterface.getDevice();
    }

    public static String getAppVersion() {
        return blockchainInterface.getAppVersion();
    }
}

package info.blockchain.wallet.metadata;

import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.FrameworkInterface;
import info.blockchain.wallet.MockInterceptor;
import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.api.PersistentUrls;
import info.blockchain.wallet.bip44.HDWallet;
import info.blockchain.wallet.bip44.HDWalletFactory;
import info.blockchain.wallet.bip44.HDWalletFactory.Language;
import info.blockchain.wallet.contacts.data.PublicContactDetails;
import info.blockchain.wallet.util.MetadataUtil;
import info.blockchain.wallet.util.RestClient;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.params.BitcoinCashMainNetParams;
import org.bitcoinj.params.BitcoinMainNetParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

public final class MetadataTest {

    boolean isEncrypted = false;

    MockInterceptor mockInterceptor;

    @Before
    public void setup() {

        mockInterceptor = MockInterceptor.getInstance();

        //Set environment
        BlockchainFramework.init(new FrameworkInterface() {
            @Override
            public Retrofit getRetrofitApiInstance() {

                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                OkHttpClient okHttpClient = new OkHttpClient.Builder()
                        .addInterceptor(mockInterceptor)//Mock responses
                        .addInterceptor(loggingInterceptor)//Extensive logging
                        .build();

                return RestClient.getRetrofitApiInstance(okHttpClient);
            }

            @Override
            public Retrofit getRetrofitExplorerInstance() {
                return null;
            }

            @Override
            public Environment getEnvironment() {
                return Environment.PRODUCTION;
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
                return null;
            }

            @Override
            public String getDeviceId() {
                return "DeviceId";
            }

            @Override
            public String getAppVersion() {
                return null;
            }
        });
    }

    private HDWallet getWallet() throws Exception {

        return HDWalletFactory
            .restoreWallet(PersistentUrls.getInstance().getBitcoinParams(), Language.US,
                "15e23aa73d25994f1921a1256f93f72c", "", 1);
    }

    @Test
    public void testAddressDerivation() throws Exception {

        String address = "12sC9tqHzAhdoukhCbTnyx2MjYXNXBGHnF";

        mockInterceptor.setResponseString("{\"message\":\"Not Found\"}");
        mockInterceptor.setResponseCode(404);

        DeterministicKey metaDataHDNode = MetadataUtil.deriveMetadataNode(getWallet().getMasterKey());

        Metadata metadata = new Metadata.Builder(metaDataHDNode, 2)
                .setEncrypted(isEncrypted)
                .build();

        Assert.assertTrue(metadata.getAddress().equals(address));
    }

    @Test
    public void testMetadata() throws Exception{

        mockInterceptor.setResponseString("{\"message\":\"Not Found\"}");
        mockInterceptor.setResponseCode(404);//New metadata response

        DeterministicKey metaDataHDNode = MetadataUtil.deriveMetadataNode(getWallet().getMasterKey());

        Metadata metadata = new Metadata.Builder(metaDataHDNode, 2)
                .setEncrypted(isEncrypted)
                .build();

        String msg = "Rage rage";
        mockInterceptor.setResponseString("{\"version\":1,\"payload\":\"UmFnZSByYWdl\",\"signature\":\"HwIx4Cs+1pB+8iCDREC1PiDqkDnEDhfcto6bQjxzo3RrHB562kg8nTjbFoaydlUI6tDkl3WnZahrmFZ8ErNqmBY=\",\"type_id\":1}");
        mockInterceptor.setResponseCode(200);
        metadata.putMetadata(new PublicContactDetails("mock").toJson());

        mockInterceptor.setResponseString("{\"payload\":\"UmFnZSByYWdl\",\"version\":1,\"type_id\":1,\"signature\":\"HwIx4Cs+1pB+8iCDREC1PiDqkDnEDhfcto6bQjxzo3RrHB562kg8nTjbFoaydlUI6tDkl3WnZahrmFZ8ErNqmBY=\",\"created_at\":1480592845000,\"updated_at\":1480592845000,\"address\":\"1ErzrzB1FE1YyQ7LADMzye9J3Q8QeR1mja\"}");
        mockInterceptor.setResponseCode(200);
        String result1 = metadata.getMetadata();

        Assert.assertTrue(msg.equals(result1));

        mockInterceptor.setResponseString("{\"version\":1,\"payload\":\"UmFnZSByYWdlIHNvbWUgbW9yZQ==\",\"signature\":\"H7zIO7fzkb8t+zdbiEzlKt/8InFjH5N2ja+SaJPcAuheP3soAJwxVrnzG0tDQpxyJKSgYn/9il6XsLW3rmm3a+g=\",\"prev_magic_hash\":\"73d03136dfdadf66b4048f938ad8acf6084134a84ac6f542e0144b29999a6836\",\"type_id\":1}");
        mockInterceptor.setResponseCode(200);
        msg = "Rage rage some more";
        metadata.putMetadata(new PublicContactDetails("mock").toJson());

        mockInterceptor.setResponseString("{\"payload\":\"UmFnZSByYWdlIHNvbWUgbW9yZQ==\",\"version\":1,\"type_id\":1,\"signature\":\"H7zIO7fzkb8t+zdbiEzlKt/8InFjH5N2ja+SaJPcAuheP3soAJwxVrnzG0tDQpxyJKSgYn/9il6XsLW3rmm3a+g=\",\"prev_magic_hash\":\"73d03136dfdadf66b4048f938ad8acf6084134a84ac6f542e0144b29999a6836\",\"created_at\":1480592845000,\"updated_at\":1480592845000,\"address\":\"1ErzrzB1FE1YyQ7LADMzye9J3Q8QeR1mja\"}");
        mockInterceptor.setResponseCode(200);
        String result2 = metadata.getMetadata();
        Assert.assertTrue(msg.equals(result2));

        mockInterceptor.setResponseString("{\"status\": \"success\"}");
        mockInterceptor.setResponseCode(200);
        mockInterceptor.setResponseString("");
        metadata.deleteMetadata(msg);

        mockInterceptor.setResponseString("{\"message\":\"Not Found\"}");
        mockInterceptor.setResponseCode(404);
        Assert.assertNull(metadata.getMetadata());
    }
}
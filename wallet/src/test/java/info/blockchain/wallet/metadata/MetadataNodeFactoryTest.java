package info.blockchain.wallet.metadata;

import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.FrameworkInterface;
import info.blockchain.wallet.MockInterceptor;
import info.blockchain.wallet.api.Environment;
import info.blockchain.wallet.util.RestClient;

import java.util.LinkedList;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.HDKeyDerivation;
import org.bitcoinj.params.BitcoinCashMainNetParams;
import org.bitcoinj.params.BitcoinMainNetParams;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;

public final class MetadataNodeFactoryTest {

    String masterKeyHex = "cbf9a5a77a913ee6cbdd560510c8d40020431479edb582e81262cc5e14fdc921";
    String guid = "5381b28a-290b-4ce5-bc7f-11bb40a3600c";
    String sharedKey = "f0a6557e-9421-43c7-8e0c-6aa025110afb";
    String walletPass = "password";

    MockInterceptor mockInterceptor;

    MetadataNodeFactory metadataNodeFactory;

    @Before
    public void setup() throws Exception {

        mockInterceptor = new MockInterceptor();

        BlockchainFramework.init(new FrameworkInterface() {
            @Override
            public Retrofit getRetrofitApiInstance() {

                HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
                loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

                OkHttpClient okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(mockInterceptor)//Mock responses
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

        LinkedList<String> responses = new LinkedList<>();
        responses.add("{\"payload\":\"iJ8bow3OHV3F0vN4hn1UpAI2GJ00362olaiq5LEFgo7aTjdnp4jtsObSqWUmqPWnWNKu06PJ9EVV4hQHTrS+USvqOH9XZz/Xz09Pc+V2BAsmAO49nRXvhSWjuOLieT7PZkvVkC4/y9E4hG+Xe8GwlqrKW7iTiHkjixArLif3qHeO0IudhAdx7ZazSYnIKKmN4HIxFMAqxUgFTmWyQ81pJ4y5Ja0i3HI6OiNXKXTwHisT9kIcQrWLouHUzRTbUeyjgHh5hafqZiQbqAAFcCYH/S71k1bGh3fk7Bx98nK9J3By9Q7SKXT1OPfREpucDryZkrtDxTtscHJrN98EcY6Tc+XzavR+kxlOpPVUbe0Ipbc=\",\"version\":1,\"type_id\":-1,\"signature\":\"H9r2rDp+M06rWJwi+rpK9K5xMxEkofQCA/aTL0CArDMJDC2677jrEB08ODMCDpuwvap3QZxxRLCTnBBUoP0zzgM=\",\"address\":\"17gRsYd7F8C5sqDRdkkzRfAZK84sTRt3vF\",\"created_at\":1501672559000,\"updated_at\":1501672559000}");
        responses.add("{\"message\":\"Not Found\"}");
        responses.add("{\"message\":\"Not Found\"}");
        mockInterceptor.setResponseStringList(responses);
        LinkedList<Integer> responseCodes = new LinkedList<>();
        responseCodes.add(200);
        responseCodes.add(404);
        responseCodes.add(404);
        mockInterceptor.setResponseCodeList(responseCodes);
        metadataNodeFactory = new MetadataNodeFactory(guid, sharedKey, walletPass);
    }

    @Test
    public void testSaveMetadataHdNodes() throws Exception {

        String metadataB58 = "xprv9twfkEhG8UPRWCrX3HnowyQk7vMCVeGY5ZHWsLANZqxqBaiAHPN3MvuuFedVNBhixki7WBvYW8gcg2mCemfV6XNNpCZ31JusEHUhdmdhsvp";
        String sharedMetadataB58 = "xprv9twfkEhLiMZnQ5Gjf8w2f5wn5ASrQ9qMVwLo458shQmfMWhus8oo9aecvsAmSDXfgGbX2xQgeXi9Luj9ao5xGLKGjCj7gKiwvBGnjhdVEji";

        mockInterceptor.setResponseString("{\"status\": \"success\"}");
        metadataNodeFactory.saveMetadataHdNodes(HDKeyDerivation.createMasterPrivateKey(Hex.decode(masterKeyHex)));

        Assert.assertTrue(metadataNodeFactory.getMetadataNode().serializePrivB58(BitcoinMainNetParams.get()).equals(metadataB58));
        Assert.assertTrue(metadataNodeFactory.getSharedMetadataNode().serializePrivB58(BitcoinMainNetParams.get()).equals(sharedMetadataB58));
    }

    @Test
    public void testIsMetadataUsable() throws Exception {
        mockInterceptor.setResponseString("{\"payload\":\"pfLts2mHJeyAE/25ZVw5/811h6CZZr6yY0OoJ8COzt19/IEYRPZgNbgVOK/JBeC8nBKD+05AoZi3taQKVqT3kTF/BWRvbD1W8GTgZm9Y2krq9bHnCpGY69gKJ2bqv++3drv9pH/t9iPdrVZLXesgjDlYyHClnMZV1QC4dHt7cqpt3QYRudZ6SwpUpa167+9Kq/QoYn+5qWECuY7uxZ5vs71p50KF+m4W948Ll3SoPRgRjkfzyGwLe1AJRWrh6PjhCaQ/wR7zlJMZ2F8Gr8B6NwOemdV9rYs8ovZEVDTdtnwHIeiEyWkbXqHmOQf/xZ3dxOCWdTH9Mms7g2RoOtKBE4ffoUlm8J/RGSABi28OJ0M=\",\"version\":1,\"type_id\":-1,\"signature\":\"ICfsnQK9ngPfVbzgmMxt0v/L1LZC2K+76U+a8S7L0AKTXuTBNLY2wH6oZNc7hdlIhwvv9ngOaSgHVmq6y0yIYWk=\",\"prev_magic_hash\":\"736c9d1fd16574752460bf880c917ed1144233ef591212a27a4bdb16f285aa09\",\"address\":\"17gRsYd7F8C5sqDRdkkzRfAZK84sTRt3vF\",\"created_at\":1501672559000,\"updated_at\":1501672602000}");
        Assert.assertTrue(metadataNodeFactory.isMetadataUsable());
    }

    @Test
    public void isLegacySecondPwNodeAvailable_No() throws Exception {
        mockInterceptor.setResponseCode(404);
        mockInterceptor.setResponseString("{\"message\":\"Not Found\"}");
        Assert.assertFalse(metadataNodeFactory.isLegacySecondPwNodeAvailable());
    }

    @Test
    public void isLegacySecondPwNodeAvailable_Yes() throws Exception {

        mockInterceptor.setResponseCode(200);
        mockInterceptor.setResponseString("{\"payload\":\"+5tmcealT370Tg5vrQvMDdwGAZalLuldMBrBqloGXXKgsAEHOU/lp5u2RPAcXf7eL3KY5pm0u5bI+Nl/jGJMKM7gdpnsu2gt0EWfQFh47Bv71syZlZuYqt+b9aAUWQAaqPDmF6lbFe8xR7x327LoeUslCadliSX4vepDFedp+JoahIE2jE2aTNFs1ZN4NRWIbX1GkWXchkEF85G4BPcyfgdFns8vXvqbN5nBs0wLhrXaU+wSrzrRKOhp6YTP+M1KV8Haotv9g5rbCCLlze8xHQBL32130p0QaOVUl+0A3BfUS4HhaSpaGuaISbd9HjLHWdpijXPkd/b4qI5r0oCbm/5CmlIn/uXd3zQvnIvX4pQ=\",\"version\":1,\"type_id\":-1,\"signature\":\"IPf6iER8EdI1nqT2LQGA4m4E3iorK7fnrKnaUQwckzU7EI0L0XegV0Qo3I1uuFoUZnOx42gz7uYuim4jpVp/NYw=\",\"prev_magic_hash\":\"8cd2723102abbbfaeb0bd5f6e848877693156cf60b08ac73799bcb2dd061a751\",\"address\":\"1MnKyRWkoYue7qeAoDDbXDASSqf288nv8n\",\"created_at\":1501675160000,\"updated_at\":1501675233000}");
        Assert.assertTrue(metadataNodeFactory.isLegacySecondPwNodeAvailable());
    }
}
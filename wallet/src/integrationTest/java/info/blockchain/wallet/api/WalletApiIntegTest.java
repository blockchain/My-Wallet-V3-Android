package info.blockchain.wallet.api;

import info.blockchain.wallet.BaseIntegTest;
import info.blockchain.wallet.api.data.Status;
import info.blockchain.wallet.exceptions.ApiException;

import org.junit.Test;
import org.spongycastle.util.encoders.Hex;

import java.security.SecureRandom;

import io.reactivex.observers.TestObserver;
import okhttp3.ResponseBody;
import retrofit2.HttpException;
import retrofit2.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Simple integration test.
 * Ensures endpoints are reachable even if responses return an error body.
 */
public class WalletApiIntegTest extends BaseIntegTest {

    // Unverified details
    private String guid = "cfd055ed-1a7f-4a92-8584-2f4d01365034";
    private String sharedKey = "b4ff6bf5-17a9-4905-b54b-a526816aa100";
    private WalletApi walletApi = new WalletApi();

    @Test
    public void getRandomBytesCall() throws Exception {
        Response<ResponseBody> call = walletApi.getRandomBytesCall().execute();

        assertNotNull(call.body());
        assertNotNull(call.body().string());
    }

    @Test
    public void getRandomBytesObservable() {
        final TestObserver<ResponseBody> testObserver = walletApi.getRandomBytes().test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertNotNull(testObserver.values().get(0));
        assertNotNull(testObserver.values().get(0).toString());
    }

    @Test
    public void updateFirebaseNotificationToken() {
        final TestObserver<ResponseBody> testObserver =
                walletApi.updateFirebaseNotificationToken("", "", "").test();

        testObserver.assertTerminated();
        testObserver.assertNotComplete();
        testObserver.assertError(HttpException.class);
    }

    @Test
    public void registerMdid() {
        final TestObserver<ResponseBody> testObserver =
                walletApi.registerMdid("", "", "").test();

        testObserver.assertTerminated();
        testObserver.assertNotComplete();
        testObserver.assertError(HttpException.class);
    }

    @Test
    public void unregisterMdid() {
        final TestObserver<ResponseBody> testObserver =
                walletApi.unregisterMdid("", "", "").test();

        testObserver.assertTerminated();
        testObserver.assertNotComplete();
        testObserver.assertError(HttpException.class);
    }

    @Test
    public void setAccess() throws Exception {

        byte[] bytes = new byte[16];
        SecureRandom random = new SecureRandom();
        random.nextBytes(bytes);
        String key = new String(Hex.encode(bytes), "UTF-8");
        random.nextBytes(bytes);
        String value = new String(Hex.encode(bytes), "UTF-8");

        final TestObserver<Response<Status>> testObserver =
                walletApi.setAccess(key, value, "1234").test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals(200, testObserver.values().get(0).code());
        assertEquals("Key Saved", testObserver.values().get(0).body().getSuccess());
    }

    @Test
    public void validateAccess() {
        String key = "db2f4184429bf05c1a962384befb8873";

        final TestObserver<Response<Status>> testObserver =
                walletApi.validateAccess(key, "1234").test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertEquals("3236346436663830663565363434383130393262343739613437333763333739",
                testObserver.values().get(0).body().getSuccess());
        assertEquals(200, testObserver.values().get(0).code());
    }

    @Test
    public void fetchWalletData() throws Exception {
        Response<ResponseBody> call = walletApi.fetchWalletData(guid, sharedKey).execute();

        assertNotNull(call.body());
        assertNotNull(call.body().string());
    }

    @Test
    public void fetchEncryptedPayload() {
        final TestObserver<Response<ResponseBody>> testObserver =
                walletApi.fetchEncryptedPayload(guid, "").test();

        testObserver.assertComplete();
        testObserver.assertNoErrors();
        assertNotNull(testObserver.values().get(0));
        assertNotNull(testObserver.values().get(0).toString());
    }

    @Test
    public void fetchPairingEncryptionPasswordCall() throws Exception {
        Response<ResponseBody> call =
                walletApi.fetchPairingEncryptionPasswordCall("").execute();

        assertNotNull(call.errorBody());
        assertNotNull(call.errorBody().string());
    }

    @Test
    public void fetchPairingEncryptionPasswordObservable() {
        final TestObserver<ResponseBody> testObserver =
                walletApi.fetchPairingEncryptionPassword("").test();

        testObserver.assertTerminated();
        testObserver.assertError(HttpException.class);
    }

    @Test
    public void getSignedJsonToken_should_fail_unverified_email() {
        // Arrange

        // Act
        final TestObserver<String> testObserver =
                walletApi.getSignedJsonToken(guid, sharedKey, "coinify").test();
        // Assert
        testObserver.assertError(ApiException.class);
    }

    @Test
    public void getSignedJsonToken_should_be_successful() {
        // Arrange
        final String verifiedGuid = "cc3b7469-2b45-4af6-ac49-480d75a70d0f";
        final String verifiedSharedKey = "7dc0efed-a548-4732-8488-7bbb3f345f9b";
        // Act
        final TestObserver<String> testObserver =
                walletApi.getSignedJsonToken(verifiedGuid, verifiedSharedKey, "coinify").test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }
}

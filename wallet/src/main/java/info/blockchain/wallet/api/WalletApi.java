package info.blockchain.wallet.api;

import info.blockchain.wallet.ApiCode;
import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.api.data.SignedToken;
import info.blockchain.wallet.api.data.Status;
import info.blockchain.wallet.api.data.WalletOptions;
import info.blockchain.wallet.exceptions.ApiException;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.functions.Function;
import okhttp3.ResponseBody;
import org.apache.commons.lang3.StringUtils;
import org.spongycastle.util.encoders.Hex;
import retrofit2.Call;
import retrofit2.Response;

import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WalletApi {

    private final WalletExplorerEndpoints walletServer;
    private final ApiCode apiCode;

    public WalletApi(WalletExplorerEndpoints walletServer, ApiCode apiCode) {
        this.walletServer = walletServer;
        this.apiCode = apiCode;
    }

    private WalletExplorerEndpoints getExplorerInstance() {
        return walletServer;
    }

    public Observable<ResponseBody> updateFirebaseNotificationToken(String token,
                                                                    String guid,
                                                                    String sharedKey) {

        return getExplorerInstance().postToWallet("update-firebase",
                guid,
                sharedKey,
                token,
                token.length(),
                getApiCode());
    }

    public Observable<ResponseBody> registerMdid(String guid,
                                                 String sharedKey,
                                                 String signedGuid) {

        return getExplorerInstance().postToWallet("register-mdid",
                guid, sharedKey, signedGuid, signedGuid.length(),
                getApiCode());
    }

    public Observable<ResponseBody> unregisterMdid(String guid,
                                                   String sharedKey,
                                                   String signedGuid) {

        return getExplorerInstance().postToWallet("unregister-mdid",
                guid, sharedKey, signedGuid, signedGuid.length(),
                getApiCode());
    }

    public Observable<Response<Status>> setAccess(String key, String value, String pin) {
        String hex = Hex.toHexString(value.getBytes());
        return getExplorerInstance().pinStore(key, pin, hex, "put", getApiCode());
    }

    public Observable<Response<Status>> validateAccess(String key, String pin) {
        return getExplorerInstance().pinStore(key, pin, null, "get", getApiCode());
    }

    public Call<ResponseBody> insertWallet(String guid,
                                           String sharedKey,
                                           @Nullable List<String> activeAddressList,
                                           String encryptedPayload,
                                           String newChecksum,
                                           String email,
                                           String device) throws UnsupportedEncodingException {

        String pipedAddresses = null;
        if (activeAddressList != null) {
            pipedAddresses = StringUtils.join(activeAddressList, "|");
        }

        return getExplorerInstance().syncWalletCall("insert",
                guid,
                sharedKey,
                encryptedPayload,
                encryptedPayload.length(),
                URLEncoder.encode(newChecksum, "utf-8"),
                pipedAddresses,
                email,
                device,
                null,
                getApiCode());
    }

    public Call<ResponseBody> updateWallet(String guid,
                                           String sharedKey,
                                           @Nullable List<String> activeAddressList,
                                           String encryptedPayload,
                                           String newChecksum,
                                           String oldChecksum,
                                           String device) throws UnsupportedEncodingException {

        String pipedAddresses = null;
        if (activeAddressList != null) {
            pipedAddresses = StringUtils.join(activeAddressList, "|");
        }

        return getExplorerInstance().syncWalletCall("update",
                guid,
                sharedKey,
                encryptedPayload,
                encryptedPayload.length(),
                URLEncoder.encode(newChecksum, "utf-8"),
                pipedAddresses,
                null,
                device,
                oldChecksum,
                getApiCode());
    }

    public Call<ResponseBody> fetchWalletData(String guid, String sharedKey) {
        return getExplorerInstance().fetchWalletData("wallet.aes.json",
                guid,
                sharedKey,
                "json",
                getApiCode());
    }

    public Observable<ResponseBody> submitTwoFactorCode(String sessionId, String guid, String twoFactorCode) {
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Authorization", "Bearer " + sessionId);
        return getExplorerInstance().submitTwoFactorCode(
                headerMap,
                "get-wallet",
                guid,
                twoFactorCode,
                twoFactorCode.length(),
                "plain",
                getApiCode());
    }

    public Observable<Response<ResponseBody>> getSessionId(String guid) {
        return getExplorerInstance().getSessionId(guid);
    }

    public Observable<Response<ResponseBody>> fetchEncryptedPayload(String guid, String sessionId) {
        return getExplorerInstance().fetchEncryptedPayload(guid,
                "SID=" + sessionId,
                "json",
                false,
                getApiCode());
    }

    public Call<ResponseBody> fetchPairingEncryptionPasswordCall(final String guid) {
        return getExplorerInstance().fetchPairingEncryptionPasswordCall("pairing-encryption-password",
                guid,
                getApiCode());
    }

    public Observable<ResponseBody> fetchPairingEncryptionPassword(final String guid) {
        return getExplorerInstance().fetchPairingEncryptionPassword("pairing-encryption-password",
                guid,
                getApiCode());
    }

    public Observable<Settings> fetchSettings(String method, String guid, String sharedKey) {
        return getExplorerInstance().fetchSettings(method,
                guid,
                sharedKey,
                "plain",
                getApiCode());
    }

    public Observable<ResponseBody> updateSettings(String method, String guid, String sharedKey, String payload) {
        return getExplorerInstance().updateSettings(method,
                guid,
                sharedKey,
                payload,
                payload.length(),
                "plain",
                getApiCode());
    }

    public Observable<WalletOptions> getWalletOptions() {
        return getExplorerInstance().getWalletOptions(getApiCode());
    }

    public Single<String> getSignedJsonToken(String guid, String sharedKey, String partner) {
        return getExplorerInstance().getSignedJsonToken(guid,
                sharedKey,
                "email%7Cwallet_age",
                partner,
                getApiCode())
                .map(new Function<SignedToken, String>() {
                    @Override
                    public String apply(SignedToken signedToken) throws Exception {
                        if (!signedToken.isSuccessful()) {
                            throw new ApiException(signedToken.getError());
                        } else {
                            return signedToken.getToken();
                        }
                    }
                });
    }

    private String getApiCode() {
        return apiCode.getApiCode();
    }
}
package info.blockchain.wallet.api;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.api.data.SignedToken;
import info.blockchain.wallet.api.data.Status;
import info.blockchain.wallet.api.data.WalletOptions;

import java.util.Map;

import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.HeaderMap;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface WalletExplorerEndpoints {

    @FormUrlEncoded
    @POST("wallet")
    Observable<ResponseBody> postToWallet(
            @Field("method") String method,
            @Field("guid") String guid,
            @Field("sharedKey") String sharedKey,
            @Field("payload") String payload,
            @Field("length") int length,
            @Field("api_code") String apiCode);

    @FormUrlEncoded
    @POST("wallet")
    Observable<Settings> fetchSettings(
            @Field("method") String method,
            @Field("guid") String guid,
            @Field("sharedKey") String sharedKey,
            @Field("format") String format,
            @Field("api_code") String apiCode);

    @FormUrlEncoded
    @POST("wallet")
    Observable<ResponseBody> updateSettings(
            @Field("method") String method,
            @Field("guid") String guid,
            @Field("sharedKey") String sharedKey,
            @Field("payload") String payload,
            @Field("length") int length,
            @Field("format") String format,
            @Field("api_code") String apiCode);

    @FormUrlEncoded
    @POST("wallet")
    Call<ResponseBody> fetchWalletData(
            @Field("method") String method,
            @Field("guid") String guid,
            @Field("sharedKey") String sharedKey,
            @Field("format") String format,
            @Field("api_code") String apiCode);

    @FormUrlEncoded
    @POST("wallet")
    Observable<ResponseBody> submitTwoFactorCode(
            @HeaderMap Map<String, String> headers,
            @Field("method") String method,
            @Field("guid") String guid,
            @Field("payload") String twoFactorCode,
            @Field("length") int length,
            @Field("format") String format,
            @Field("api_code") String apiCode);

    @FormUrlEncoded
    @POST("wallet")
    Call<ResponseBody> syncWalletCall(
            @Field("method") String method,
            @Field("guid") String guid,
            @Field("sharedKey") String sharedKey,
            @Field("payload") String payload,
            @Field("length") int length,
            @Field("checksum") String checksum,
            @Field("active") String active,
            @Field("email") String email,
            @Field("device") String device,
            @Field("old_checksum") String old_checksum,
            @Field("api_code") String apiCode);

    @FormUrlEncoded
    @POST("wallet")
    Call<ResponseBody> fetchPairingEncryptionPasswordCall(
            @Field("method") String method,
            @Field("guid") String guid,
            @Field("api_code") String apiCode);

    @FormUrlEncoded
    @POST("wallet")
    Observable<ResponseBody> fetchPairingEncryptionPassword(
            @Field("method") String method,
            @Field("guid") String guid,
            @Field("api_code") String apiCode);

    @GET("wallet/{guid}?format=json&resend_code=false")
    Observable<Response<ResponseBody>> getSessionId(
            @Path("guid") String guid);

    @GET("wallet/{guid}")
    Observable<Response<ResponseBody>> fetchEncryptedPayload(
            @Path("guid") String guid,
            @Header("cookie") String sessionId,
            @Query("format") String format,
            @Query("resend_code") boolean resendCode,
            @Query("api_code") String apiCode);

    @POST("pin-store")
    Observable<Response<Status>> pinStore(
            @Query("key") String key,
            @Query("pin") String pin,
            @Query("value") String value,
            @Query("method") String method,
            @Query("api_code") String apiCode);

    @GET("Resources/wallet-options.json")
    Observable<WalletOptions> getWalletOptions(@Query("api_code") String apiCode);

    @GET("wallet/signed-token")
    Single<SignedToken> getSignedJsonToken(
            @Query("guid") String guid,
            @Query("sharedKey") String sharedKey,
            @Query("fields") String fields,
            @Query("partner") String partner,
            @Query("api_code") String apiCode);
}
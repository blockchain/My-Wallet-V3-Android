package piuk.blockchain.android.util;

import android.content.Context;
import android.util.Log;

import info.blockchain.api.PersistentUrls;

import org.thoughtcrime.ssl.pinning.util.PinningHelper;

import java.io.IOException;
import java.net.URL;

import javax.inject.Inject;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;

import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;
import piuk.blockchain.android.data.connectivity.ConnectivityStatus;
import piuk.blockchain.android.util.annotations.Thunk;

// openssl s_client -showcerts -connect blockchain.info:443

public class SSLVerifyUtil {

    @SuppressWarnings("WeakerAccess")
    @Thunk
    static final String TAG = SSLVerifyUtil.class.getSimpleName();
    @SuppressWarnings("WeakerAccess")
    @Thunk
    static final Subject<SslEvent> mSslPinningSubject = PublishSubject.create();

    private Context context;

    @Inject
    public SSLVerifyUtil(Context context) {
        this.context = context;
    }

    public void validateSSL() {
        if (ConnectivityStatus.hasConnectivity(context)) {
            checkSslStatus(new CertificateCheckInterface() {
                @Override
                public void onSuccess() {
                    Log.i(TAG, "SSL pinning completed successfully");
                    mSslPinningSubject.onNext(SslEvent.Success);
                }

                @Override
                public void onServerDown() {
                    mSslPinningSubject.onNext(SslEvent.ServerDown);
                }

                @Override
                public void onPinningFail() {
                    mSslPinningSubject.onNext(SslEvent.PinningFail);
                }
            });
        } else {
            mSslPinningSubject.onNext(SslEvent.NoConnection);
        }
    }

    public Subject<SslEvent> getSslPinningSubject() {
        return mSslPinningSubject;
    }

    @SuppressWarnings("WeakerAccess")
    public enum SslEvent {
        Success,
        ServerDown,
        PinningFail,
        NoConnection
    }

    interface CertificateCheckInterface {

        void onSuccess();

        void onServerDown();

        void onPinningFail();
    }

    private void checkSslStatus(CertificateCheckInterface listener) {
        getPinnedConnection()
                .subscribe(httpsURLConnection -> {
                    try {
                        byte[] data = new byte[4096];
                        int read = httpsURLConnection.getInputStream().read(data);
                        listener.onSuccess();

                    } catch (IOException e) {
                        e.printStackTrace();

                        if (e instanceof SSLHandshakeException) {
                            listener.onPinningFail();
                        } else {
                            listener.onServerDown();
                        }
                    }
                }, throwable -> {
                    listener.onServerDown();
                });
    }

    private Observable<HttpsURLConnection> getPinnedConnection() {
        return Observable.fromCallable(() -> {
            // DER encoded public key:
            // 30820122300d06092a864886f70d01010105000382010f003082010a0282010100bff56f562096307165320b0f04ff30e3f7d7e7a2813a35c16bfbe549c23f2a5d0388818fc0f9326a9679322fd7a6d4a1f2c4d45129c8641f6a3e7d9175938f050352a1cf09440399a36a358a846e4b5ef43baafbcb6af9f3615a7a49aae497cfeaaeb943e0175bab546abacc60b29c9bb7f588c62ac81e21038e760f044c07fe6d8a1cba4f8b5e9835bb8eddec79d506dc47fd73030630bf1af7bd70352ced281efae1675e70a6918d98645ebc389d2169ff72a82c7ff7a6328f0cd337197d87e208d2bc8cdd21182157fcb12a6db697dbd62b76800debef8feea2da2a5e074feea56af52f4300c17892018f7584eb5d4946c10156a85746ae8eacc5ebe112df0203010001
            // SHA-1 hash of DER encoded public key byte array
            String[] pins = new String[]{"10902ad9c6fb7d84c133b8682a7e7e30a5b6fb90"};
            URL url = new URL(PersistentUrls.getInstance().getDefaultBaseServerUrl());
            return PinningHelper.getPinnedHttpsURLConnection(context, pins, url);
        }).subscribeOn(Schedulers.io());
    }
}
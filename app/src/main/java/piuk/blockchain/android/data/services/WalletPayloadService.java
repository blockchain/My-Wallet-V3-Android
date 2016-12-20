package piuk.blockchain.android.data.services;

import info.blockchain.api.WalletPayload;

import piuk.blockchain.android.data.rxjava.RxUtil;
import io.reactivex.Observable;

public class WalletPayloadService {

    private WalletPayload walletPayload;

    public WalletPayloadService(WalletPayload walletPayload) {
        this.walletPayload = walletPayload;
    }

    /**
     * Get encrypted copy of Payload
     *
     * @param guid          A user's GUID
     * @param sessionId     The session ID
     * @return              {@link Observable<String>} wrapping an encrypted Payload
     */
    public Observable<String> getEncryptedPayload(String guid, String sessionId) {
        return Observable.fromCallable(() -> walletPayload.getEncryptedPayload(guid, sessionId))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Get the current session ID
     *
     * @param guid          A user's GUID
     * @return              {@link Observable<String>} wrapping the session ID
     */
    public Observable<String> getSessionId(String guid) {
        return Observable.fromCallable(() -> walletPayload.getSessionId(guid))
                .compose(RxUtil.applySchedulersToObservable());
    }

    /**
     * Get the encryption password for pairing
     *
     * @param guid          A user's GUID
     * @return              {@link Observable<String>} wrapping the pairing encryption password
     */
    public Observable<String> getPairingEncryptionPassword(String guid) {
        return Observable.fromCallable(() -> walletPayload.getPairingEncryptionPassword(guid))
                .compose(RxUtil.applySchedulersToObservable());
    }
}

package piuk.blockchain.android.data.notifications;

import android.support.annotation.NonNull;
import android.util.Log;

import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;

import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.services.NotificationService;

public class NotificationTokenManager {

    private static final String TAG = NotificationTokenManager.class.getSimpleName();

    private NotificationService notificationService;
    private AccessState accessState;
    private PayloadManager payloadManager;

    public NotificationTokenManager(NotificationService notificationService, AccessState accessState, PayloadManager payloadManager) {
        this.notificationService = notificationService;
        this.accessState = accessState;
        this.payloadManager = payloadManager;
    }

    /**
     * Sends the access token to the update-firebase endpoint once the user is logged in fully, as
     * the token is generally only generated on first install or first start after updating.
     *
     * @param token A Firebase access token
     */
    void storeAndUpdateToken(@NonNull String token) {
        if (accessState.isLoggedIn()) {
            // Send token
            sendFirebaseToken(token);
        } else {
            // Store token and send once login event happens
            accessState.getAuthEventSubject().subscribe(authEvent -> {
                if (authEvent == AccessState.AuthEvent.Login) {
                    // Send token
                    sendFirebaseToken(token);
                }
            }, Throwable::printStackTrace);
        }
    }

    private void sendFirebaseToken(String refreshedToken) {
        Payload payload = payloadManager.getPayload();
        String guid = payload.getGuid();
        String sharedKey = payload.getSharedKey();

        // TODO: 09/11/2016 Decide what to do if sending fails, perhaps retry?
        notificationService.sendNotificationToken(refreshedToken, guid, sharedKey)
                .subscribe(() -> Log.d(TAG, "sendFirebaseToken: success"), Throwable::printStackTrace);
    }

}

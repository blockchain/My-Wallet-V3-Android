package piuk.blockchain.android.data.services;

import info.blockchain.api.Notifications;

import io.reactivex.Completable;
import io.reactivex.schedulers.Schedulers;

public class NotificationService {

    private Notifications notification;

    public NotificationService(Notifications notification) {
        this.notification = notification;
    }

    /**
     * Sends the updated Firebase token to the server along with the GUID and Shared Key
     *
     * @param token     A Firebase notification token
     * @param guid      The user's GUID
     * @param sharedKey The user's shared key
     * @return A {@link Completable}, ie an Observable specifically for methods returning void.
     */
    public Completable sendNotificationToken(String token, String guid, String sharedKey) {
        return Completable.fromCallable(() -> {
            notification.updateFirebaseNotificationToken(token, guid, sharedKey);
            return Void.TYPE;
        }).subscribeOn(Schedulers.io());
    }
}

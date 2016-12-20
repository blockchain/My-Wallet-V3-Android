package piuk.blockchain.android.data.notifications;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import android.util.Log;

public class FcmCallbackService extends FirebaseMessagingService {

    private static final String TAG = FcmCallbackService.class.getSimpleName();

    public FcmCallbackService() {
        Log.d(TAG, "FcmCallbackService: Constructor invoked");
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "onMessageReceived: " + remoteMessage);
        // Here is where we intercept notifications for processing elsewhere
        // TODO: 10/11/2016 Build metadata service
    }
}

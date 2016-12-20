package piuk.blockchain.android.data.notifications;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

import android.util.Log;

import javax.inject.Inject;

import piuk.blockchain.android.injection.Injector;

public class InstanceIdService extends FirebaseInstanceIdService {

    private static final String TAG = InstanceIdService.class.getSimpleName();

    @Inject protected NotificationTokenManager notificationTokenManager;

    public InstanceIdService() {
        Injector.getInstance().getAppComponent().inject(this);
    }

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + refreshedToken);

        if (refreshedToken != null) {
            // FIXME: 15/11/2016 This will need re-enabling once metadata is live
//            notificationTokenManager.storeAndUpdateToken(refreshedToken);
        }
    }
}

package piuk.blockchain.android.data.connectivity;

import android.content.Context;
import android.content.IntentFilter;

import piuk.blockchain.android.util.AndroidUtils;

public enum ConnectivityManager {

    INSTANCE;

    ConnectivityManager() {
        // No-op
    }

    public static ConnectivityManager getInstance() {
        return INSTANCE;
    }

    /**
     * Listens for network connection events using whatever is best practice for the current API level,
     * ie a {@link android.content.BroadcastReceiver} for pre-21, and the {@link
     * android.net.ConnectivityManager.NetworkCallback} for Lollipop and above
     */
    public void registerNetworkListener(Context context) {
        if (AndroidUtils.is21orHigher()) {
            new ConnectionStateMonitor(context).enable();
        } else {
            context.registerReceiver(new NetworkStateReceiver(), new IntentFilter("android.net.conn.CONNECTIVITY_CHANGE"));
        }

    }
}

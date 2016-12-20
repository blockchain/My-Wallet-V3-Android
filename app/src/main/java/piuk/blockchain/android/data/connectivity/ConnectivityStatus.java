package piuk.blockchain.android.data.connectivity;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class ConnectivityStatus {

    ConnectivityStatus() {
    }

    public static boolean hasConnectivity(Context ctx) {
        boolean ret = false;

        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo neti = cm.getActiveNetworkInfo();
            if (neti != null && neti.isConnectedOrConnecting()) {
                ret = true;
            }
        }

        return ret;
    }

}

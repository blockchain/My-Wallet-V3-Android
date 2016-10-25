package piuk.blockchain.android.data.websocket;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;

import info.blockchain.wallet.payload.PayloadManager;

//import android.util.Log;

public class WebSocketService extends android.app.Service {

    public static final String ACTION_INTENT = "info.blockchain.wallet.WebSocketService.SUBSCRIBE_TO_ADDRESS";
    private final IBinder mBinder = new LocalBinder();
    private WebSocketHandler webSocketHandler = null;
    private PayloadManager payloadManager;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {

            if (ACTION_INTENT.equals(intent.getAction())) {
                webSocketHandler.subscribeToAddress(intent.getStringExtra("address"));
                webSocketHandler.subscribeToXpub(intent.getStringExtra("xpub"));
            }
        }
    };

    @Override
    public IBinder onBind(final Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.payloadManager = PayloadManager.getInstance();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        String[] addrs = getAddresses();
        String[] xpubs = getXpubs();

        if (addrs.length > 0 || xpubs.length > 0) {
            webSocketHandler = new WebSocketHandler(getApplicationContext(), payloadManager.getPayload().getGuid(), xpubs, addrs);
            webSocketHandler.start();
        }
    }

    private String[] getXpubs() {

        int nbAccounts = 0;
        if (payloadManager.getPayload().isUpgraded()) {
            try {
                nbAccounts = payloadManager.getPayload().getHdWallet().getAccounts().size();
            } catch (java.lang.IndexOutOfBoundsException e) {
                nbAccounts = 0;
            }
        }

        final String[] xpubs = new String[nbAccounts];
        for (int i = 0; i < nbAccounts; i++) {
            String s = payloadManager.getPayload().getHdWallet().getAccounts().get(i).getXpub();
            if (s != null && s.length() > 0) {
                xpubs[i] = s;
            }
        }

        return xpubs;
    }

    private String[] getAddresses() {

        int nbLegacy = payloadManager.getPayload().getLegacyAddressList().size();
        final String[] addrs = new String[nbLegacy];
        for (int i = 0; i < nbLegacy; i++) {
            String s = payloadManager.getPayload().getLegacyAddressList().get(i).getAddress();
            if (s != null && s.length() > 0) {
                addrs[i] = payloadManager.getPayload().getLegacyAddressList().get(i).getAddress();
            }
        }

        return addrs;
    }

    @Override
    public void onDestroy() {
        if (webSocketHandler != null) webSocketHandler.stop();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
        super.onDestroy();
    }

    public class LocalBinder extends Binder {
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }
}
package piuk.blockchain.android.data.websocket;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import info.blockchain.wallet.payload.PayloadManager;

import javax.inject.Inject;

import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.annotations.Thunk;

public class WebSocketService extends Service {

    public static final String ACTION_INTENT = "info.blockchain.wallet.WebSocketService.SUBSCRIBE_TO_ADDRESS";
    private final IBinder binder = new LocalBinder();
    @Inject protected PayloadManager payloadManager;
    @Inject protected PrefsUtil prefsUtil;
    @Thunk WebSocketHandler webSocketHandler;

    protected BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, final Intent intent) {
            if (intent.getAction().equals(ACTION_INTENT)) {
                if (intent.hasExtra("address")) {
                    webSocketHandler.subscribeToAddress(intent.getStringExtra("address"));
                }
                if (intent.hasExtra("xpub")) {
                    webSocketHandler.subscribeToXpub(intent.getStringExtra("xpub"));
                }
            }
        }
    };

    {
        Injector.getInstance().getAppComponent().inject(this);
    }

    @Override
    public IBinder onBind(final Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        IntentFilter filter = new IntentFilter(ACTION_INTENT);
        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(receiver, filter);

        String[] addrs = getAddresses();
        String[] xpubs = getXpubs();

        webSocketHandler = new WebSocketHandler(
                getApplicationContext(),
                payloadManager,
                new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)),
                payloadManager.getPayload().getGuid(),
                xpubs,
                addrs);

        webSocketHandler.start();
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
        if (webSocketHandler != null) webSocketHandler.stopPermanently();
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(receiver);
        super.onDestroy();
    }

    private class LocalBinder extends Binder {

        LocalBinder() {
            // Empty constructor
        }

        @SuppressWarnings("unused") // Necessary for implementing bound Android Service
        public WebSocketService getService() {
            return WebSocketService.this;
        }
    }
}

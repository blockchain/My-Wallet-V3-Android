package piuk.blockchain.android.data.websocket;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.v4.content.LocalBroadcastManager;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import info.blockchain.wallet.payload.PayloadManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashSet;
import java.util.Timer;
import java.util.TimerTask;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.NotificationsUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class WebSocketHandler {

    private static String guid;
    private static String[] xpubs;
    private static String[] addrs;
    private final long pingInterval = 20000L;//ping pong every 20 seconds
    private final long pongTimeout = 5000L;//pong timeout after 5 seconds
    private WebSocket mConnection;
    private HashSet<String> subHashSet = new HashSet<>();
    private HashSet<String> onChangeHashSet = new HashSet<>();
    private Timer pingTimer;
    private boolean pingPongSuccess = false;
    private PrefsUtil prefsUtil;
    private MonetaryUtil monetaryUtil;
    private PayloadManager payloadManager;
    private Context context;

    public WebSocketHandler(Context ctx, String guid, String[] xpubs, String[] addrs) {
        this.context = ctx;
        this.guid = guid;
        this.xpubs = xpubs;
        this.addrs = addrs;
        this.payloadManager = PayloadManager.getInstance();
        this.prefsUtil = new PrefsUtil(context);
        this.monetaryUtil = new MonetaryUtil(prefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC));
    }

    public void send(String message) {
        //Make sure each message is only sent once per socket lifetime
        if (!subHashSet.contains(message)) {
            try {
                if (mConnection != null && mConnection.isOpen()) {
//                    Log.i("WebSocketHandler", "Websocket subscribe:" +message);
                    mConnection.sendText(message);
                    subHashSet.add(message);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
//            Log.d("WebSocketHandler", "Message sent already: "+message);
        }
    }

    public synchronized void subscribe() {

        if (guid == null) {
            return;
        }
        // send("{\"op\":\"blocks_sub\"}");
        send("{\"op\":\"wallet_sub\",\"guid\":\"" + guid + "\"}");

        for (int i = 0; i < xpubs.length; i++) {
            if (xpubs[i] != null && xpubs[i].length() > 0) {
                send("{\"op\":\"xpub_sub\", \"xpub\":\"" + xpubs[i] + "\"}");
            }
        }

        for (int i = 0; i < addrs.length; i++) {
            if (addrs[i] != null && addrs[i].length() > 0) {
                send("{\"op\":\"addr_sub\", \"addr\":\"" + addrs[i] + "\"}");
            }
        }
    }

    public synchronized void subscribeToXpub(String xpub) {
        if (xpub != null && !xpub.isEmpty()) {
            send("{\"op\":\"xpub_sub\", \"xpub\":\"" + xpub + "\"}");
        }
    }

    public synchronized void subscribeToAddress(String address) {
        if (address != null && !address.isEmpty())
            send("{\"op\":\"addr_sub\", \"addr\":\"" + address + "\"}");
    }

    public void stop() {

        stopPingTimer();

        if (mConnection != null && mConnection.isOpen()) {
            mConnection.disconnect();
        }
    }

    public void start() {
        try {
            stop();
            connect();
            startPingTimer();
        } catch (IOException | com.neovisionaries.ws.client.WebSocketException e) {
            e.printStackTrace();
        }

    }

    private void startPingTimer() {

        pingTimer = new Timer();
        pingTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (mConnection != null) {
                    if (mConnection.isOpen()) {
                        pingPongSuccess = false;
                        mConnection.sendPing();
                        startPongTimer();
                    } else {
                        start();
                    }
                }
            }
        }, pingInterval, pingInterval);
    }

    private void stopPingTimer() {
        if (pingTimer != null) pingTimer.cancel();
    }

    private void startPongTimer() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                if (!pingPongSuccess) {
                    //ping pong unsuccessful after x seconds - restart connection
                    start();
                }
            }
        }, pongTimeout);
    }

    /**
     * Connect to the server.
     */
    private void connect() throws IOException, WebSocketException {
        new ConnectionTask().execute();
    }

    private void updateBalancesAndTransactions() {

        new Thread() {
            public void run() {

                Looper.prepare();

                try {
                    payloadManager.updateBalancesAndTransactions();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                Intent intent = new Intent(BalanceFragment.ACTION_INTENT);
                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);

                Looper.loop();

            }
        }.start();
    }

    private class ConnectionTask extends AsyncTask<Void, Void, Void> {

        protected Void doInBackground(Void... args) {

            try {
                //Seems we make a new connection here, so we should clear our HashSet
//                    Log.d("WebSocketHandler", "Reconnect of websocket..");
                subHashSet.clear();

                mConnection = new WebSocketFactory()
                        .createSocket("wss://ws.blockchain.info/inv")
                        .addHeader("Origin", "https://blockchain.info").recreate()
                        .addListener(new WebSocketAdapter() {

                            @Override
                            public void onPongFrame(WebSocket websocket, WebSocketFrame frame) throws Exception {
                                super.onPongFrame(websocket, frame);
                                pingPongSuccess = true;
                            }

                            public void onTextMessage(WebSocket websocket, String message) {
//                                    Log.d("WebSocket", message);

                                if (guid == null) {
                                    return;
                                }

                                try {
                                    JSONObject jsonObject = null;
                                    try {
                                        jsonObject = new JSONObject(message);
                                    } catch (JSONException je) {
//                                            Log.i("WebSocketHandler", "JSONException:" + je.getMessage());
                                        jsonObject = null;
                                    }

                                    if (jsonObject == null) {
//                                            Log.i("WebSocketHandler", "jsonObject is null");
                                        return;
                                    }

//                                        Log.i("WebSocketHandler", jsonObject.toString());

                                    String op = (String) jsonObject.get("op");
                                    if (op.equals("utx") && jsonObject.has("x")) {

                                        JSONObject objX = (JSONObject) jsonObject.get("x");

                                        long value = 0L;
                                        long total_value = 0L;
                                        long ts = 0L;
                                        String in_addr = null;

                                        if (objX.has("time")) {
                                            ts = objX.getLong("time");
                                        }

                                        if (objX.has("inputs")) {
                                            JSONArray inputArray = (JSONArray) objX.get("inputs");
                                            JSONObject inputObj = null;
                                            for (int j = 0; j < inputArray.length(); j++) {
                                                inputObj = (JSONObject) inputArray.get(j);
                                                if (inputObj.has("prev_out")) {
                                                    JSONObject prevOutObj = (JSONObject) inputObj.get("prev_out");
                                                    if (prevOutObj.has("value")) {
                                                        value = prevOutObj.getLong("value");
                                                    }
                                                    if (prevOutObj.has("xpub")) {
                                                        total_value -= value;
                                                    } else if (prevOutObj.has("addr")) {
                                                        if (payloadManager.getPayload().containsLegacyAddress((String) prevOutObj.get("addr"))) {
                                                            total_value -= value;
                                                        } else if (in_addr == null) {
                                                            in_addr = (String) prevOutObj.get("addr");
                                                        } else {
                                                            ;
                                                        }
                                                    } else {
                                                        ;
                                                    }
                                                }
                                            }
                                        }

                                        if (objX.has("out")) {
                                            JSONArray outArray = (JSONArray) objX.get("out");
                                            JSONObject outObj = null;
                                            for (int j = 0; j < outArray.length(); j++) {
                                                outObj = (JSONObject) outArray.get(j);
                                                if (outObj.has("value")) {
                                                    value = outObj.getLong("value");
                                                }
                                                if (outObj.has("xpub")) {
                                                    total_value += value;
                                                } else if (outObj.has("addr")) {
                                                    if (payloadManager.getPayload().containsLegacyAddress((String) outObj.get("addr"))) {
                                                        total_value += value;
                                                    }
                                                } else {
                                                    ;
                                                }
                                            }
                                        }

                                        String title = context.getString(R.string.app_name);
                                        if (total_value > 0L) {
                                            String marquee = context.getString(R.string.received_bitcoin) + " " + monetaryUtil.getBTCFormat().format((double) total_value / 1e8) + " BTC";
                                            String text = marquee;
                                            if (total_value > 0) {
                                                text += " from " + in_addr;
                                            }

                                            new NotificationsUtil(context).setNotification(title, marquee, text, R.drawable.ic_notification_transparent, R.drawable.ic_launcher, MainActivity.class, 1000);
                                        }

                                        updateBalancesAndTransactions();

                                    } else if (op.equals("on_change")) {

                                        final String localChecksum = payloadManager.getCheckSum();

                                        boolean isSameChecksum = false;
                                        if (jsonObject.has("checksum")) {
                                            final String remoteChecksum = (String) jsonObject.get("checksum");
                                            isSameChecksum = remoteChecksum.equals(localChecksum);
                                        }

                                        if (!onChangeHashSet.contains(message) && !isSameChecksum) {

                                            //Remote update to wallet data detected
                                            if (payloadManager.getTempPassword() != null) {
                                                //Download changed payload
                                                payloadManager.initiatePayload(payloadManager.getPayload().getSharedKey(),
                                                        payloadManager.getPayload().getGuid(),
                                                        payloadManager.getTempPassword(), () -> {
                                                        });
                                                ToastCustom.makeText(context, context.getString(R.string.wallet_updated), ToastCustom.LENGTH_SHORT, ToastCustom.TYPE_GENERAL);
                                                updateBalancesAndTransactions();
                                            }

                                            onChangeHashSet.add(message);
                                        }

                                    } else {
                                        ;
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }

                            }
                        });
                mConnection.connect();

                subscribe();

            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}

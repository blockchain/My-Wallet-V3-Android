package piuk.blockchain.android.data.websocket;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import info.blockchain.api.PersistentUrls;
import info.blockchain.wallet.payload.PayloadManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.data.rxjava.IgnorableDefaultObserver;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.ui.balance.BalanceFragment;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.home.MainActivity;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.NotificationsUtil;
import piuk.blockchain.android.util.annotations.Thunk;


@SuppressWarnings("WeakerAccess")
class WebSocketHandler {

    @Thunk static final String TAG = WebSocketHandler.class.getSimpleName();

    private final static long PING_INTERVAL = 20 * 1000L;
    private final static long RETRY_INTERVAL = 5 * 1000L;

    private boolean stoppedDeliberately = false;
    private String[] xpubs;
    private String[] addrs;
    @Thunk String guid;
    @Thunk WebSocket connection;
    @Thunk HashSet<String> subHashSet = new HashSet<>();
    @Thunk HashSet<String> onChangeHashSet = new HashSet<>();
    @Thunk MonetaryUtil monetaryUtil;
    @Thunk PayloadManager payloadManager;
    @Thunk Context context;
    @Thunk CompositeDisposable compositeDisposable = new CompositeDisposable();

    public WebSocketHandler(Context context,
                            PayloadManager payloadManager,
                            MonetaryUtil monetaryUtil,
                            String guid,
                            String[] xpubs,
                            String[] addrs) {

        this.context = context;
        this.payloadManager = payloadManager;
        this.monetaryUtil = monetaryUtil;
        this.guid = guid;
        this.xpubs = xpubs;
        this.addrs = addrs;
    }

    /**
     * Starts listening for updates to subscribed xpubs and addresses. Will attempt reconnection
     * every 5 seconds if it cannot connect immediately.
     */
    public void start() {
        stop();
        stoppedDeliberately = false;
        connectToWebSocket()
                .doOnError(throwable -> attemptReconnection())
                .subscribe(new IgnorableDefaultObserver<>());
    }

    /**
     * Halts and disconnects the WebSocket service whilst preventing reconnection until {@link
     * #start()} is called
     */
    public void stopPermanently() {
        stoppedDeliberately = true;
        stop();
    }

    private void stop() {
        if (isConnected()) {
            connection.disconnect();
        }
    }

    private void send(String message) {
        // Make sure each message is only sent once per socket lifetime
        if (!subHashSet.contains(message)) {
            try {
                if (isConnected()) {
                    connection.sendText(message);
                    subHashSet.add(message);
                }
            } catch (Exception e) {
                Log.e(TAG, "send: ", e);
            }
        }
    }

    private void subscribe() {
        if (guid == null) {
            return;
        }
        send("{\"op\":\"wallet_sub\",\"guid\":\"" + guid + "\"}");

        for (String xpub : xpubs) {
            if (xpub != null && xpub.length() > 0) {
                send("{\"op\":\"xpub_sub\", \"xpub\":\"" + xpub + "\"}");
            }
        }

        for (String addr : addrs) {
            if (addr != null && addr.length() > 0) {
                send("{\"op\":\"addr_sub\", \"addr\":\"" + addr + "\"}");
            }
        }
    }

    public void subscribeToXpub(String xpub) {
        if (xpub != null && !xpub.isEmpty()) {
            send("{\"op\":\"xpub_sub\", \"xpub\":\"" + xpub + "\"}");
        }
    }

    public void subscribeToAddress(String address) {
        if (address != null && !address.isEmpty()) {
            send("{\"op\":\"addr_sub\", \"addr\":\"" + address + "\"}");
        }
    }

    @Thunk
    void attemptReconnection() {
        if (compositeDisposable.size() == 0 && !stoppedDeliberately) {
            compositeDisposable.add(
                    getReconnectionObservable()
                            .subscribe(
                                    value -> Log.d(TAG, "attemptReconnection: " + value),
                                    throwable -> Log.e(TAG, "attemptReconnection: ", throwable)
                            ));
        }
    }

    private Observable<Long> getReconnectionObservable() {
        return Observable.interval(RETRY_INTERVAL, TimeUnit.MILLISECONDS)
                .takeUntil((ObservableSource<Object>) aLong -> isConnected())
                .doOnNext(tick -> start());
    }

    private boolean isConnected() {
        return connection != null && connection.isOpen();
    }

    @Thunk
    void updateBalancesAndTransactions() {
        updateBalancesAndTxs().subscribe(new IgnorableDefaultObserver<>());
    }

    private Completable updateBalancesAndTxs() {
        return Completable.fromCallable(() -> {
            payloadManager.updateBalancesAndTransactions();
            return Void.TYPE;
        }).doAfterTerminate(() -> {
            Intent intent = new Intent(BalanceFragment.ACTION_INTENT);
            LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    private Completable connectToWebSocket() {
        return Completable.fromCallable(() -> {
            subHashSet.clear();

            connection = new WebSocketFactory()
                    .createSocket(PersistentUrls.getInstance().getCurrentWebsocketUrl())
                    .addHeader("Origin", "https://blockchain.info")
                    .setPingInterval(PING_INTERVAL)
                    .addListener(new WebSocketAdapter() {
                        @Override
                        public void onConnected(WebSocket websocket, Map<String, List<String>> headers) throws Exception {
                            super.onConnected(websocket, headers);
                            compositeDisposable.clear();
                        }

                        @Override
                        public void onTextMessage(WebSocket websocket, String message) {
                            JSONObject jsonObject;
                            try {
                                jsonObject = new JSONObject(message);
                                attemptParseMessage(message, jsonObject);
                            } catch (JSONException je) {
                                Log.e(TAG, "onTextMessage: ", je);
                            }
                        }

                        @Override
                        public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                            super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);
                            attemptReconnection();
                        }

                    }).connect();

            subscribe();
            // Necessary but meaningless return type for Completable
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    @Thunk
    void attemptParseMessage(String message, JSONObject jsonObject) {
        try {
            String op = (String) jsonObject.get("op");
            if (op.equals("utx") && jsonObject.has("x")) {
                JSONObject objX = (JSONObject) jsonObject.get("x");

                long value = 0L;
                long totalValue = 0L;
                String inAddr = null;

                if (objX.has("inputs")) {
                    JSONArray inputArray = (JSONArray) objX.get("inputs");
                    JSONObject inputObj;
                    for (int j = 0; j < inputArray.length(); j++) {
                        inputObj = (JSONObject) inputArray.get(j);
                        if (inputObj.has("prev_out")) {
                            JSONObject prevOutObj = (JSONObject) inputObj.get("prev_out");
                            if (prevOutObj.has("value")) {
                                value = prevOutObj.getLong("value");
                            }
                            if (prevOutObj.has("xpub")) {
                                totalValue -= value;
                            } else if (prevOutObj.has("addr")) {
                                if (payloadManager.getPayload().containsLegacyAddress((String) prevOutObj.get("addr"))) {
                                    totalValue -= value;
                                } else if (inAddr == null) {
                                    inAddr = (String) prevOutObj.get("addr");
                                }
                            }
                        }
                    }
                }

                if (objX.has("out")) {
                    JSONArray outArray = (JSONArray) objX.get("out");
                    JSONObject outObj;
                    for (int j = 0; j < outArray.length(); j++) {
                        outObj = (JSONObject) outArray.get(j);
                        if (outObj.has("value")) {
                            value = outObj.getLong("value");
                        }
                        if (outObj.has("xpub")) {
                            totalValue += value;
                        } else if (outObj.has("addr")) {
                            if (payloadManager.getPayload().containsLegacyAddress((String) outObj.get("addr"))) {
                                totalValue += value;
                            }
                        }
                    }
                }

                String title = context.getString(R.string.app_name);
                if (totalValue > 0L) {
                    String marquee = context.getString(R.string.received_bitcoin) + " " + monetaryUtil.getBTCFormat().format((double) totalValue / 1e8) + " BTC";
                    String text = marquee;
                    if (totalValue > 0) {
                        text += " from " + inAddr;
                    }

                    triggerNotification(title, marquee, text);
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
                    // Remote update to wallet data detected
                    if (payloadManager.getTempPassword() != null) {
                        // Download changed payload
                        //noinspection ThrowableResultOfMethodCallIgnored
                        downloadChangedPayload().blockingGet();
                        showToast().subscribeOn(AndroidSchedulers.mainThread());
                        updateBalancesAndTransactions();
                    }

                    onChangeHashSet.add(message);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "attemptParseMessage: ", e);
        }
    }

    private Completable showToast() {
        return Completable.fromRunnable(
                () -> ToastCustom.makeText(
                        context,
                        context.getString(R.string.wallet_updated),
                        ToastCustom.LENGTH_SHORT,
                        ToastCustom.TYPE_GENERAL));
    }

    private Completable downloadChangedPayload() {
        return Completable.fromCallable(() -> {
            payloadManager.initiatePayload(
                    payloadManager.getPayload().getSharedKey(),
                    payloadManager.getPayload().getGuid(),
                    payloadManager.getTempPassword(), () -> {
                        // No-op, blocking call
                    });
            return Void.TYPE;
        }).compose(RxUtil.applySchedulersToCompletable());
    }

    private void triggerNotification(String title, String marquee, String text) {
        new NotificationsUtil(context).setNotification(
                title,
                marquee,
                text,
                R.drawable.ic_notification_transparent,
                R.drawable.ic_launcher,
                MainActivity.class,
                1000);
    }
}

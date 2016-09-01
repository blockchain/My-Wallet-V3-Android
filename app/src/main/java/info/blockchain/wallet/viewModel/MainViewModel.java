package info.blockchain.wallet.viewModel;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import info.blockchain.api.DynamicFee;
import info.blockchain.api.Unspent;
import info.blockchain.wallet.access.AccessState;
import info.blockchain.wallet.cache.DefaultAccountUnspentCache;
import info.blockchain.wallet.cache.DynamicFeeCache;
import info.blockchain.wallet.connectivity.ConnectivityStatus;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.AppUtil;
import info.blockchain.wallet.util.ExchangeRateFactory;
import info.blockchain.wallet.util.OSUtil;
import info.blockchain.wallet.util.PrefsUtil;
import info.blockchain.wallet.util.RootUtil;
import info.blockchain.wallet.util.WebUtil;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;

import piuk.blockchain.android.di.Injector;

public class MainViewModel implements ViewModel {

    private final String TAG = getClass().getSimpleName();

    private Context context;
    private DataListener dataListener;
    private OSUtil osUtil;
    @Inject protected PrefsUtil prefs;
    @Inject protected AppUtil appUtil;
    @Inject protected PayloadManager payloadManager;

    private long mBackPressed;
    private static final int COOL_DOWN_MILLIS = 2 * 1000;

    public interface DataListener {
        void onRooted();
        void onConnectivityFail();
        void onFetchTransactionsStart();
        void onFetchTransactionCompleted();
        void onScanInput(String strUri);
        void onStartBalanceFragment();
        void onExitConfirmToast();
        void kickToLauncherPage();
    }

    public MainViewModel(Context context, DataListener dataListener) {
        Injector.getInstance().getAppComponent().inject(this);
        this.context = context;
        this.dataListener = dataListener;
        this.osUtil = new OSUtil(context);
        this.appUtil.applyPRNGFixes();

        checkRooted();
        checkConnectivity();
    }

    public PayloadManager getPayloadManager() {
        return payloadManager;
    }

    private void checkRooted(){
        if (new RootUtil().isDeviceRooted() &&
                !prefs.getValue("disable_root_warning", false)) {
            this.dataListener.onRooted();
        }
    }

    private void checkConnectivity(){

        if(ConnectivityStatus.hasConnectivity(context)) {
            preLaunchChecks();
        }else{
            this.dataListener.onConnectivityFail();
        }
    }

    private void preLaunchChecks(){
        exchangeRateThread();

        if (AccessState.getInstance().isLoggedIn()) {
            dataListener.onFetchTransactionsStart();

            new Thread(() -> {
                Looper.prepare();
                cacheDynamicFee();
                cacheDefaultAccountUnspentData();
                Looper.loop();
            }).start();

            new Thread(() -> {

                Looper.prepare();

                try {
                    payloadManager.updateBalancesAndTransactions();
                } catch (Exception e) {
                    e.printStackTrace();
                }

                dataListener.onFetchTransactionCompleted();

                dataListener.onStartBalanceFragment();

                if (prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "").length() > 0) {
                    String strUri = prefs.getValue(PrefsUtil.KEY_SCHEME_URL, "");
                    prefs.setValue(PrefsUtil.KEY_SCHEME_URL, "");
                    dataListener.onScanInput(strUri);
                }

                Looper.loop();
            }).start();
        } else {
            // This should never happen, but handle the scenario anyway by starting the launcher
            // activity, which handles all login/auth/corruption scenarios itself
            dataListener.kickToLauncherPage();
        }
    }

    private void cacheDynamicFee(){
        DynamicFeeCache.getInstance().setSuggestedFee(new DynamicFee().getDynamicFee());
    }

    private void cacheDefaultAccountUnspentData(){

        if(payloadManager.getPayload().getHdWallet() != null) {

            int defaultAccountIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();

            Account defaultAccount = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultAccountIndex);
            String xpub = defaultAccount.getXpub();

            try {
                JSONObject unspentResponse = new Unspent().getUnspentOutputs(xpub);
                if(unspentResponse != null) {
                    DefaultAccountUnspentCache.getInstance().setUnspentApiResponse(xpub, unspentResponse);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void destroy() {
        appUtil.deleteQR();
        context = null;
        dataListener = null;
        stopWebSocketService();
        DynamicFeeCache.getInstance().destroy();
    }

    private void exchangeRateThread() {

        List<String> currencies = Arrays.asList(ExchangeRateFactory.getInstance().getCurrencies());
        String strCurrentSelectedFiat = prefs.getValue(PrefsUtil.KEY_SELECTED_FIAT, "");
        if (!currencies.contains(strCurrentSelectedFiat)) {
            prefs.setValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY);
        }

        new Thread(() -> {
            Looper.prepare();

            String response = null;
            try {
                response = WebUtil.getInstance().getURL(WebUtil.EXCHANGE_URL);

                ExchangeRateFactory.getInstance().setData(response);
                ExchangeRateFactory.getInstance().updateFxPricesForEnabledCurrencies();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Looper.loop();

        }).start();
    }

    public void unpair() {
        payloadManager.wipe();
        MultiAddrFactory.getInstance().wipe();
        prefs.logOut();
        appUtil.restartApp();
    }

    public void onBackPressed() {
        if (mBackPressed + COOL_DOWN_MILLIS > System.currentTimeMillis()) {
            AccessState.getInstance().logout(context);
            return;
        } else {
            dataListener.onExitConfirmToast();
        }

        mBackPressed = System.currentTimeMillis();
    }

    public void startWebSocketService(){
        if (!osUtil.isServiceRunning(info.blockchain.wallet.websocket.WebSocketService.class)) {
            context.startService(new Intent(context, info.blockchain.wallet.websocket.WebSocketService.class));
        }
    }

    public void stopWebSocketService(){
        if (!osUtil.isServiceRunning(info.blockchain.wallet.websocket.WebSocketService.class)) {
            context.stopService(new Intent(context, info.blockchain.wallet.websocket.WebSocketService.class));
        }
    }
}

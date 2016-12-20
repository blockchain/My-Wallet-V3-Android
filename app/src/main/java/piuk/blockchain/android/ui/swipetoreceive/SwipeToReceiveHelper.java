package piuk.blockchain.android.ui.swipetoreceive;

import android.support.annotation.NonNull;
import android.util.Log;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadManager;

import org.bitcoinj.core.AddressFormatException;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Observable;
import piuk.blockchain.android.data.rxjava.RxUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class SwipeToReceiveHelper {

    public static final String KEY_SWIPE_RECEIVE_ADDRESSES = "swipe_receive_addresses";
    public static final String KEY_SWIPE_RECEIVE_ACCOUNT_NAME = "swipe_receive_account_name";
    private static final String TAG = SwipeToReceiveHelper.class.getSimpleName();

    private PayloadManager payloadManager;
    private MultiAddrFactory multiAddrFactory;
    private PrefsUtil prefsUtil;

    public SwipeToReceiveHelper(PayloadManager payloadManager, MultiAddrFactory multiAddrFactory, PrefsUtil prefsUtil) {
        this.payloadManager = payloadManager;
        this.multiAddrFactory = multiAddrFactory;
        this.prefsUtil = prefsUtil;
    }

    /**
     * Derives 5 addresses from the current point on the receive chain. Stores them alongside
     * the account name in SharedPrefs. Only stores addresses if enabled in SharedPrefs.
     */
    public void updateAndStoreAddresses() {
        if (getIfSwipeEnabled()) {
            int numOfAddresses = 5;

            int defaultAccountIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
            String receiveAccountName = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultAccountIndex).getLabel();
            storeAccountName(receiveAccountName);

            StringBuilder stringBuilder = new StringBuilder();

            for (int i = 0; i < numOfAddresses; i++) {
                try {
                    String receiveAddress = payloadManager.getReceiveAddressAtPosition(defaultAccountIndex, i);
                    stringBuilder.append(receiveAddress).append(",");
                } catch (AddressFormatException e) {
                    Log.e(TAG, "updateAndStoreAddresses: ", e);
                }
            }

            storeAddresses(stringBuilder.toString());
        }
    }

    /**
     * Returns the next unused address from the list of 5 stored for swipe to receive. Can return an
     * empty String if no unused addresses are found.
     */
    Observable<String> getNextAvailableAddress() {
        return getBalanceOfAddresses(getReceiveAddresses())
                .map(linkedHashMap -> {
                    for (Map.Entry<String, Long> entry : linkedHashMap.entrySet()) {
                        String address = entry.getKey();
                        Long balance = entry.getValue();
                        if (balance == 0L) {
                            return address;
                        }
                    }
                    return "";
                });
    }

    /**
     * Returns a List of the next 5 available unused (at the time of storage) receive addresses. Can
     * return an empty list.
     */
    @NonNull
    List<String> getReceiveAddresses() {
        String addressString = prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "");
        if (addressString.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.asList(addressString.split(","));
    }

    /**
     * Returns the account name associated with the receive addresses.
     */
    @NonNull
    String getAccountName() {
        return prefsUtil.getValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, "");
    }

    private boolean getIfSwipeEnabled() {
        return prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true);
    }

    private Observable<LinkedHashMap<String, Long>> getBalanceOfAddresses(List<String> addresses) {
        return Observable.fromCallable(() -> multiAddrFactory.getAddressBalanceFromApi(addresses))
                .compose(RxUtil.applySchedulersToObservable());
    }

    private void storeAddresses(String addresses) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_ADDRESSES, addresses);
    }

    private void storeAccountName(String accountName) {
        prefsUtil.setValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, accountName);
    }
}

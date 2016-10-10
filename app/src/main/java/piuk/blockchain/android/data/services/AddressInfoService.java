package piuk.blockchain.android.data.services;

import info.blockchain.api.AddressInfo;
import info.blockchain.wallet.payload.LegacyAddress;

import piuk.blockchain.android.data.rxjava.RxUtil;
import rx.Observable;

public class AddressInfoService {

    public static final String PARAMETER_FINAL_BALANCE = "&limit=0";
    private static final String KEY_FINAL_BALANCE = "final_balance";

    private AddressInfo addressInfo;

    public AddressInfoService(AddressInfo info) {
        addressInfo = info;
    }

    /**
     * Returns the current balance for a given {@link LegacyAddress}
     *
     * @param address   The {@link LegacyAddress} which you want to know the balance of
     * @param parameter A URL parameter - this can allow you to get the balance at a specific point
     *                  in time, or now using {@value PARAMETER_FINAL_BALANCE}
     * @return          An {@link Observable<Long>} wrapping the balance of the address
     */
    public Observable<Long> getAddressBalance(LegacyAddress address, String parameter) {
        return Observable.fromCallable(() -> addressInfo.getAddressInfo(address.getAddress(), parameter))
                .flatMap(response -> Observable.fromCallable(() -> response.getLong(KEY_FINAL_BALANCE)))
                .compose(RxUtil.applySchedulers());
    }
}

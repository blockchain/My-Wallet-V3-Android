package piuk.blockchain.android.ui.swipetoreceive;

import android.graphics.Bitmap;

import javax.inject.Inject;

import io.reactivex.exceptions.Exceptions;
import piuk.blockchain.android.data.datamanagers.ReceiveDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;


@SuppressWarnings("WeakerAccess")
public class SwipeToReceiveViewModel extends BaseViewModel {

    private static final int DIMENSION_QR_CODE = 600;

    private DataListener dataListener;
    @Inject ReceiveDataManager dataManager;
    @Inject SwipeToReceiveHelper swipeToReceiveHelper;

    interface DataListener {

        void displayLoading();

        void displayQrCode(Bitmap bitmap);

        void displayReceiveAddress(String address);

        void displayReceiveAccount(String accountName);

        void showNoAddressesAvailable();

    }

    SwipeToReceiveViewModel(DataListener dataListener) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.dataListener = dataListener;
    }

    @Override
    public void onViewReady() {
        dataListener.displayLoading();

        // Check we actually have addresses stored
        if (swipeToReceiveHelper.getReceiveAddresses().isEmpty()) {
            dataListener.showNoAddressesAvailable();
        } else {
            dataListener.displayReceiveAccount(swipeToReceiveHelper.getAccountName());

            compositeDisposable.add(
                    swipeToReceiveHelper.getNextAvailableAddress()
                            .doOnNext(s -> {
                                if (s.isEmpty()) {
                                    throw Exceptions.propagate(new Throwable(
                                            "Returned address is empty, no more addresses available"));
                                }
                            })
                            .doOnNext(address -> dataListener.displayReceiveAddress(address))
                            .flatMap(address -> dataManager.generateQrCode(address, DIMENSION_QR_CODE))
                            .subscribe(
                                    bitmap -> dataListener.displayQrCode(bitmap),
                                    throwable -> dataListener.showNoAddressesAvailable()));
        }
    }
}

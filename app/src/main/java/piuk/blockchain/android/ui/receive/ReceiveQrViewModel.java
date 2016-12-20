package piuk.blockchain.android.ui.receive;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.data.datamanagers.ReceiveDataManager;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;

@SuppressWarnings("WeakerAccess")
public class ReceiveQrViewModel extends BaseViewModel {

    private static final int DIMENSION_QR_CODE = 600;

    private DataListener dataListener;
    @Inject ReceiveDataManager receiveDataManager;
    @VisibleForTesting String receiveAddressString;

    interface DataListener {

        Intent getPageIntent();

        void finishActivity();

        void showToast(@StringRes int message, @ToastCustom.ToastType String toastType);

        void setAddressLabel(String label);

        void setAddressInfo(String addressInfo);

        void setImageBitmap(Bitmap bitmap);

        void showClipboardWarning(String receiveAddressString);
    }

    ReceiveQrViewModel(DataListener dataListener) {
        this.dataListener = dataListener;
        Injector.getInstance().getDataManagerComponent().inject(this);
    }

    @Override
    public void onViewReady() {
        Intent intent = dataListener.getPageIntent();

        if (intent != null
                && intent.getStringExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS) != null
                && intent.getStringExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL) != null) {

            // Show QR Code
            receiveAddressString = intent.getStringExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS);
            String labelString = intent.getStringExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL);

            dataListener.setAddressInfo(receiveAddressString);
            dataListener.setAddressLabel(labelString);

            compositeDisposable.add(
                    receiveDataManager.generateQrCode("bitcoin:" + receiveAddressString, DIMENSION_QR_CODE)
                            .subscribe(bitmap -> {
                                dataListener.setImageBitmap(bitmap);
                            }, throwable -> {
                                dataListener.showToast(R.string.shortcut_receive_qr_error, ToastCustom.TYPE_ERROR);
                                dataListener.finishActivity();
                            }));

        } else {
            dataListener.finishActivity();
        }
    }

    void onCopyClicked() {
        dataListener.showClipboardWarning(receiveAddressString);
    }
}

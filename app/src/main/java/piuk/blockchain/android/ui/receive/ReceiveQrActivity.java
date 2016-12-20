package piuk.blockchain.android.ui.receive;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatTextView;
import android.widget.ImageView;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.base.BaseAuthActivity;
import piuk.blockchain.android.ui.customviews.ToastCustom;

public class ReceiveQrActivity extends BaseAuthActivity implements ReceiveQrViewModel.DataListener {

    public static final String INTENT_EXTRA_ADDRESS = "extra_address";
    public static final String INTENT_EXTRA_LABEL = "extra_label";

    private ImageView imageView;
    private AppCompatTextView title;
    private AppCompatTextView address;
    private ReceiveQrViewModel viewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receive_qr);

        viewModel = new ReceiveQrViewModel(this);

        imageView = (ImageView) findViewById(R.id.imageview_qr);
        title = (AppCompatTextView) findViewById(R.id.account_name);
        address = (AppCompatTextView) findViewById(R.id.address_info);
        AppCompatButton done = (AppCompatButton) findViewById(R.id.action_done);
        AppCompatButton copyAddress = (AppCompatButton) findViewById(R.id.action_copy);

        viewModel.onViewReady();

        done.setOnClickListener(view -> finish());
        copyAddress.setOnClickListener(view -> viewModel.onCopyClicked());
    }

    @Override
    public void setAddressInfo(String addressInfo) {
        address.setText(addressInfo);
    }

    @Override
    public void setAddressLabel(String label) {
        title.setText(label);
    }

    @Override
    public void showToast(@StringRes int message, @ToastCustom.ToastType String toastType) {
        ToastCustom.makeText(this, getString(message), ToastCustom.LENGTH_SHORT, toastType);
    }

    @Override
    public void setImageBitmap(Bitmap bitmap) {
        imageView.setImageBitmap(bitmap);
    }

    @Override
    public Intent getPageIntent() {
        return getIntent();
    }

    @Override
    public void finishActivity() {
        finish();
    }

    @Override
    public void showClipboardWarning(String receiveAddressString) {
        new AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setTitle(R.string.app_name)
                .setMessage(R.string.receive_address_to_clipboard)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, (dialog, whichButton) -> {
                    ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    ClipData clip = android.content.ClipData.newPlainText("Send address", receiveAddressString);
                    ToastCustom.makeText(this, getString(R.string.copied_to_clipboard), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_GENERAL);
                    clipboard.setPrimaryClip(clip);
                })
                .setNegativeButton(R.string.no, null)
                .show();
    }

    @Override
    protected void startLogoutTimer() {
        // No-op
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        viewModel.destroy();
    }
}

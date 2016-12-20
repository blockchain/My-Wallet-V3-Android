package piuk.blockchain.android.ui.shortcuts;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.Log;

import info.blockchain.wallet.payload.PayloadManager;

import org.bitcoinj.core.AddressFormatException;

import java.util.Arrays;

import piuk.blockchain.android.R;
import piuk.blockchain.android.ui.receive.ReceiveQrActivity;

public class LauncherShortcutHelper {

    private Context context;
    private PayloadManager payloadManager;
    private ShortcutManager shortcutManager;

    public LauncherShortcutHelper(Context context, PayloadManager payloadManager, ShortcutManager shortcutManager) {
        this.context = context;
        this.payloadManager = payloadManager;
        this.shortcutManager = shortcutManager;
    }

    @RequiresApi(api = Build.VERSION_CODES.N_MR1)
    public void generateReceiveShortcuts() {
        try {
            int defaultAccountIndex = payloadManager.getPayload().getHdWallet().getDefaultIndex();
            String receiveAddress = payloadManager.getNextReceiveAddress(defaultAccountIndex);
            String receiveAccountName = payloadManager.getPayload().getHdWallet().getAccounts().get(defaultAccountIndex).getLabel();

            shortcutManager.removeAllDynamicShortcuts();

            Intent copyIntent = new Intent();
            copyIntent.setAction(Intent.ACTION_SEND);
            copyIntent.setType("text/plain");
            copyIntent.putExtra(Intent.EXTRA_TEXT, receiveAddress);

            ShortcutInfo copyShortcut = new ShortcutInfo.Builder(context, "receive")
                    .setShortLabel(context.getString(R.string.shortcut_receive_copy_short))
                    .setLongLabel(context.getString(R.string.shortcut_receive_copy_long))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_receive_copy))
                    .setIntent(copyIntent)
                    .build();

            Intent qrIntent = new Intent(context, ReceiveQrActivity.class);
            qrIntent.setAction(Intent.ACTION_VIEW);
            qrIntent.putExtra(ReceiveQrActivity.INTENT_EXTRA_ADDRESS, receiveAddress);
            qrIntent.putExtra(ReceiveQrActivity.INTENT_EXTRA_LABEL, receiveAccountName);

            ShortcutInfo qrShortcut = new ShortcutInfo.Builder(context, "qr")
                    .setShortLabel(context.getString(R.string.shortcut_receive_qr_short))
                    .setLongLabel(context.getString(R.string.shortcut_receive_qr_long))
                    .setIcon(Icon.createWithResource(context, R.drawable.ic_receive_scan))
                    .setIntent(qrIntent)
                    .build();

            shortcutManager.setDynamicShortcuts(Arrays.asList(copyShortcut, qrShortcut));

        } catch (AddressFormatException e) {
            Log.e(getClass().getSimpleName(), "generateReceiveShortcuts: ", e);
        }
    }

}

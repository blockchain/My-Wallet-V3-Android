package piuk.blockchain.android.ui.pairing;

import android.content.Context;
import android.content.Intent;
import android.os.Looper;

import info.blockchain.api.WalletPayload;
import info.blockchain.wallet.pairing.Pairing;
import info.blockchain.wallet.pairing.PairingQRComponents;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;

import org.spongycastle.util.encoders.Hex;

import javax.inject.Inject;

import piuk.blockchain.android.R;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.ui.auth.PinEntryActivity;
import piuk.blockchain.android.ui.base.BaseViewModel;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;

public class PairingViewModel extends BaseViewModel {

    private Context context;
    @Inject protected AppUtil appUtil;
    @Inject protected PayloadManager payloadManager;

    public PairingViewModel(Context context) {
        Injector.getInstance().getDataManagerComponent().inject(this);
        this.context = context;
    }

    @Override
    public void onViewReady() {
        // No-op
    }

    @Override
    public void destroy() {
        super.destroy();
        context = null;
    }

    public void pairWithQR(String raw) {

        appUtil.clearCredentials();

        new Thread(() -> {
            Looper.prepare();

            Pairing pairing = new Pairing();
            WalletPayload access = new WalletPayload();

            try {

                PairingQRComponents qrComponents = pairing.getQRComponentsFromRawString(raw);
                String encryptionPassword = access.getPairingEncryptionPassword(qrComponents.guid);
                String[] sharedKeyAndPassword = pairing.getSharedKeyAndPassword(qrComponents.encryptedPairingCode, encryptionPassword);

                CharSequenceX password = new CharSequenceX(new String(Hex.decode(sharedKeyAndPassword[1]), "UTF-8"));

                payloadManager.setTempPassword(password);
                appUtil.setSharedKey(sharedKeyAndPassword[0]);

                if (qrComponents.guid != null) {
                    PrefsUtil prefs = new PrefsUtil(context);
                    prefs.setValue(PrefsUtil.KEY_GUID, qrComponents.guid);
                    prefs.setValue(PrefsUtil.KEY_EMAIL_VERIFIED, true);
                    Intent intent = new Intent(context, PinEntryActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                } else {
                    ToastCustom.makeText(context, context.getString(R.string.pairing_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
                    appUtil.clearCredentialsAndRestart();
                }

            } catch (Exception e) {
                ToastCustom.makeText(context, context.getString(R.string.pairing_failed), ToastCustom.LENGTH_LONG, ToastCustom.TYPE_ERROR);
            }

            Looper.loop();
        }).start();
    }
}

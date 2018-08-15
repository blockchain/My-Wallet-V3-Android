package piuk.blockchain.androidcoreui.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;

import info.blockchain.wallet.payload.PayloadManager;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.Lazy;
import info.blockchain.wallet.payload.PayloadManagerWiper;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.utils.PrefsUtil;

@SuppressWarnings("WeakerAccess")
@Singleton
public class AppUtil {

    private static final String REGEX_UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    @Inject PrefsUtil prefs;
    @Inject Lazy<PayloadManagerWiper> payloadManager;
    @Inject Lazy<AccessState> accessState;
    private Context context;

    @Inject
    public AppUtil(Context context,
                   Lazy<PayloadManagerWiper> payloadManager,
                   Lazy<AccessState> accessState,
                   PrefsUtil prefs) {
        this.context = context;
        this.payloadManager = payloadManager;
        this.accessState = accessState;
        this.prefs = prefs;
    }

    public void clearCredentials() {
        payloadManager.get().wipe();
        prefs.clear();
        accessState.get().forgetWallet();
    }

    public void clearCredentialsAndRestart(Class launcherActivity) {
        clearCredentials();
        restartApp(launcherActivity);
    }

    public void restartApp(Class launcherActivity) {
        Intent intent = new Intent(context, launcherActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public void restartAppWithVerifiedPin(Class launcherActivity) {
        Intent intent = new Intent(context, launcherActivity);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("verified", true);
        context.startActivity(intent);
        AccessState.getInstance().logIn();
    }

    public String getReceiveQRFilename() {
        // getExternalCacheDir can return null if permission for write storage not granted
        // or if running on an emulator
        return context.getExternalCacheDir() + File.separator + "qr.png";
    }

    public void deleteQR() {
        // getExternalCacheDir can return null if permission for write storage not granted
        // or if running on an emulator
        File file = new File(context.getExternalCacheDir() + File.separator + "qr.png");
        if (file.exists()) {
            file.delete();
        }
    }

    public boolean isSane() {
        String guid = prefs.getValue(PrefsUtil.KEY_GUID, "");

        if (!guid.matches(REGEX_UUID)) {
            return false;
        }

        String encryptedPassword = prefs.getValue(PrefsUtil.KEY_ENCRYPTED_PASSWORD, "");
        String pinID = prefs.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "");

        return !(encryptedPassword.isEmpty() || pinID.isEmpty());
    }

    public boolean isCameraOpen() {
        Camera camera = null;

        try {
            camera = Camera.open();
        } catch (RuntimeException e) {
            return true;
        } finally {
            if (camera != null) {
                camera.release();
            }
        }

        return false;
    }

    public String getSharedKey() {
        return prefs.getValue(PrefsUtil.KEY_SHARED_KEY, "");
    }

    public void setSharedKey(String sharedKey) {
        prefs.setValue(PrefsUtil.KEY_SHARED_KEY, sharedKey);
    }

    public PackageManager getPackageManager() {
        return context.getPackageManager();
    }
}

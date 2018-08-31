package piuk.blockchain.android;

import android.annotation.SuppressLint;
import com.google.firebase.FirebaseApp;

/**
 * Created by adambennett on 09/08/2016.
 */
@SuppressLint("Registered")
public class BlockchainTestApplication extends BlockchainApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }

    @Override
    protected void checkSecurityProviderAndPatchIfNeeded() {
        // No-op
    }

    @Override
    public String getDeviceId() {
        // Settings.Secure is not mocked by Robolectric, so here we pass an empty identifier.
        return "";
    }
}
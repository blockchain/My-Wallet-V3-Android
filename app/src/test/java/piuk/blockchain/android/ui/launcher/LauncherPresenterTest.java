package piuk.blockchain.android.ui.launcher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.payload.data.Wallet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.R;
import piuk.blockchain.androidcore.data.access.AccessState;
import com.blockchain.notifications.NotificationTokenManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.settings.SettingsDataManager;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.androidcoreui.utils.AppUtil;
import piuk.blockchain.androidcore.utils.PrefsUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by adambennett on 09/08/2016.
 */
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class LauncherPresenterTest {

    private LauncherPresenter subject;

    @Mock private LauncherView launcherActivity;
    @Mock private PrefsUtil prefsUtil;
    @Mock private AppUtil appUtil;
    @Mock private PayloadDataManager payloadDataManager;
    @Mock private SettingsDataManager settingsDataManager;
    @Mock private AccessState accessState;
    @Mock private Intent intent;
    @Mock private Bundle extras;
    @Mock private Wallet wallet;
    @Mock private NotificationTokenManager notificationTokenManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        subject = new LauncherPresenter(appUtil, payloadDataManager, prefsUtil, accessState, settingsDataManager, notificationTokenManager);
        subject.initView(launcherActivity);
    }

    /**
     * Everything is good. Expected output is {@link LauncherActivity#onStartMainActivity()}
     */
    @Test
    public void onViewReadyVerifiedEmailVerified() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(extras.getBoolean(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(appUtil.isSane()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(wallet);
        when(wallet.isUpgraded()).thenReturn(true);
        when(accessState.isLoggedIn()).thenReturn(true);
        when(accessState.isNewlyCreated()).thenReturn(false);
        String guid = "GUID";
        String sharedKey = "SHARED_KEY";
        when(wallet.getGuid()).thenReturn(guid);
        when(wallet.getSharedKey()).thenReturn(sharedKey);
        Settings mockSettings = mock(Settings.class);
        when(settingsDataManager.initSettings(guid, sharedKey)).thenReturn(Observable.just(mockSettings));
        when(mockSettings.isEmailVerified()).thenReturn(true);
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).onStartMainActivity();
    }

    /**
     * Wallet is newly created. Launch onboarding process.
     */
    @Test
    public void onViewReadyNewlyCreated() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(extras.getBoolean(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(appUtil.isSane()).thenReturn(true);
        String guid = "GUID";
        String sharedKey = "SHARED_KEY";
        when(wallet.getGuid()).thenReturn(guid);
        when(wallet.getSharedKey()).thenReturn(sharedKey);
        when(payloadDataManager.getWallet()).thenReturn(wallet);
        Settings mockSettings = mock(Settings.class);
        when(settingsDataManager.initSettings(guid, sharedKey)).thenReturn(Observable.just(mockSettings));
        when(wallet.isUpgraded()).thenReturn(true);
        when(accessState.isLoggedIn()).thenReturn(true);
        when(accessState.isNewlyCreated()).thenReturn(true);
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).onStartOnboarding(false);
        verify(accessState).setIsLoggedIn(true);
    }

    /**
     * Everything is good, email not verified and second launch. Should start email verification nag
     * flow.
     */
    @Test
    public void onViewReadyNonVerifiedEmailNotVerifiedSecondLaunch() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(extras.getBoolean(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(appUtil.isSane()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(wallet);
        when(wallet.isUpgraded()).thenReturn(true);
        when(accessState.isLoggedIn()).thenReturn(true);
        when(accessState.isNewlyCreated()).thenReturn(false);
        String guid = "GUID";
        String sharedKey = "SHARED_KEY";
        when(wallet.getGuid()).thenReturn(guid);
        when(wallet.getSharedKey()).thenReturn(sharedKey);
        Settings mockSettings = mock(Settings.class);
        when(settingsDataManager.initSettings(guid, sharedKey)).thenReturn(Observable.just(mockSettings));
        when(mockSettings.isEmailVerified()).thenReturn(false);
        when(mockSettings.getEmail()).thenReturn("email");
        when(prefsUtil.getValue(PrefsUtil.KEY_APP_VISITS, 0)).thenReturn(1);
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).onStartOnboarding(true);
        verify(accessState).setIsLoggedIn(true);
    }

    /**
     * Everything is good, email not verified but first launch. Should start MainActivity flow.
     */
    @Test
    public void onViewReadyNonVerifiedEmailNotVerifiedFirstLaunch() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(extras.getBoolean(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(appUtil.isSane()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(wallet);
        when(wallet.isUpgraded()).thenReturn(true);
        when(accessState.isLoggedIn()).thenReturn(true);
        when(accessState.isNewlyCreated()).thenReturn(false);
        String guid = "GUID";
        String sharedKey = "SHARED_KEY";
        when(wallet.getGuid()).thenReturn(guid);
        when(wallet.getSharedKey()).thenReturn(sharedKey);
        Settings mockSettings = mock(Settings.class);
        when(settingsDataManager.initSettings(guid, sharedKey)).thenReturn(Observable.just(mockSettings));
        when(mockSettings.isEmailVerified()).thenReturn(false);
        when(mockSettings.getEmail()).thenReturn("email");
        when(prefsUtil.getValue(PrefsUtil.KEY_APP_VISITS, 0)).thenReturn(0);
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).onStartMainActivity();
        verify(accessState).setIsLoggedIn(true);
    }

    /**
     * Everything is good, email not verified and getting {@link Settings} object failed. Should
     * re-request PIN code.
     */
    @Test
    public void onViewReadyNonVerifiedEmailSettingsFailure() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(extras.getBoolean(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(appUtil.isSane()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(wallet);
        when(wallet.isUpgraded()).thenReturn(true);
        when(accessState.isLoggedIn()).thenReturn(true);
        when(accessState.isNewlyCreated()).thenReturn(false);
        String guid = "GUID";
        String sharedKey = "SHARED_KEY";
        when(wallet.getGuid()).thenReturn(guid);
        when(wallet.getSharedKey()).thenReturn(sharedKey);
        when(settingsDataManager.initSettings(guid, sharedKey)).thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
        verify(launcherActivity).onRequestPin();
    }

    /**
     * Bitcoin URI is found, expected to step into Bitcoin branch and call {@link
     * LauncherActivity#onStartMainActivity()}
     */
    @Test
    public void onViewReadyBitcoinUri() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getAction()).thenReturn(Intent.ACTION_VIEW);
        when(intent.getScheme()).thenReturn("bitcoin");
        when(intent.getData()).thenReturn(Uri.parse("bitcoin uri"));
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(extras.getBoolean(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(appUtil.isSane()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(wallet);
        when(wallet.isUpgraded()).thenReturn(true);
        when(accessState.isLoggedIn()).thenReturn(true);
        when(accessState.isNewlyCreated()).thenReturn(false);
        String guid = "GUID";
        String sharedKey = "SHARED_KEY";
        when(wallet.getGuid()).thenReturn(guid);
        when(wallet.getSharedKey()).thenReturn(sharedKey);
        Settings mockSettings = mock(Settings.class);
        when(settingsDataManager.initSettings(guid, sharedKey)).thenReturn(Observable.just(mockSettings));
        when(mockSettings.isEmailVerified()).thenReturn(true);
        // Act
        subject.onViewReady();
        // Assert
        verify(prefsUtil).setValue(PrefsUtil.KEY_SCHEME_URL, "bitcoin uri");
        verify(launcherActivity).onStartMainActivity();
    }

    /**
     * Everything is fine, but PIN not validated. Expected output is {@link
     * LauncherActivity#onRequestPin()}
     */
    @Test
    public void onViewReadyNotVerified() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(appUtil.isSane()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(wallet);
        when(wallet.isUpgraded()).thenReturn(true);
        when(accessState.isLoggedIn()).thenReturn(false);
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).onRequestPin();
    }

    /**
     * Everything is fine, but PIN not validated. However, {@link AccessState} returns logged in.
     * Expected output is {@link LauncherActivity#onStartMainActivity()}
     */
    @Test
    public void onViewReadyPinNotValidatedButLoggedIn() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(appUtil.isSane()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(wallet);
        when(wallet.isUpgraded()).thenReturn(true);
        when(accessState.isLoggedIn()).thenReturn(true);
        when(accessState.isNewlyCreated()).thenReturn(false);
        String guid = "GUID";
        String sharedKey = "SHARED_KEY";
        when(wallet.getGuid()).thenReturn(guid);
        when(wallet.getSharedKey()).thenReturn(sharedKey);
        Settings mockSettings = mock(Settings.class);
        when(settingsDataManager.initSettings(guid, sharedKey)).thenReturn(Observable.just(mockSettings));
        when(mockSettings.isEmailVerified()).thenReturn(true);
        // Act
        subject.onViewReady();
        // Assert
        verify(accessState).setIsLoggedIn(true);
        verify(launcherActivity).onStartMainActivity();
    }

    /**
     * GUID not found, expected output is {@link LauncherActivity#onNoGuid()}
     */
    @Test
    public void onViewReadyNoGuid() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("");
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).onNoGuid();
    }

    /**
     * Pin not found, expected output is {@link LauncherActivity#onRequestPin()}
     */
    @Test
    public void onViewReadyNoPin() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(prefsUtil.getValue(eq(PrefsUtil.KEY_GUID), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.KEY_PIN_IDENTIFIER), anyString())).thenReturn("");
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).onRequestPin();
    }

    /**
     * AppUtil returns not sane. Expected output is {@link LauncherActivity#onCorruptPayload()}
     */
    @Test
    public void onViewReadyNotSane() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(appUtil.isSane()).thenReturn(false);
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).onCorruptPayload();
    }

    /**
     * Everything is fine, but not upgraded. Expected output is {@link
     * LauncherActivity#onRequestUpgrade()}
     */
    @Test
    public void onViewReadyNotUpgraded() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(extras.getBoolean(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(true);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(false);
        when(appUtil.isSane()).thenReturn(true);
        when(payloadDataManager.getWallet()).thenReturn(wallet);
        when(wallet.isUpgraded()).thenReturn(false);
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).onRequestUpgrade();
    }

    /**
     * GUID exists, Shared Key exists but user logged out. Expected output is {@link
     * LauncherActivity#onReEnterPassword()}
     */
    @Test
    public void onViewReadyUserLoggedOut() {
        // Arrange
        when(launcherActivity.getPageIntent()).thenReturn(intent);
        when(intent.getExtras()).thenReturn(extras);
        when(extras.containsKey(LauncherPresenter.INTENT_EXTRA_VERIFIED)).thenReturn(false);
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        when(prefsUtil.getValue(eq(PrefsUtil.LOGGED_OUT), anyBoolean())).thenReturn(true);
        // Act
        subject.onViewReady();
        // Assert
        verify(launcherActivity).onReEnterPassword();
    }

    @Test
    public void clearCredentialsAndRestart() {
        // Arrange

        // Act
        subject.clearCredentialsAndRestart();
        // Assert
        verify(appUtil).clearCredentialsAndRestart(any());
    }

}
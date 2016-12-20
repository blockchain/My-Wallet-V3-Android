package piuk.blockchain.android.ui.settings;

import android.app.Application;
import android.content.Context;

import info.blockchain.api.Settings;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.SettingsDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNotSame;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class SettingsViewModelTest {

    private SettingsViewModel subject;
    @Mock SettingsViewModel.DataListener activity;
    @Mock FingerprintHelper fingerprintHelper;
    @Mock protected SettingsDataManager settingsDataManager;
    @Mock protected PayloadManager payloadManager;
    @Mock protected StringUtils stringUtils;
    @Mock protected PrefsUtil prefsUtil;
    @Mock protected AccessState accessState;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new MockDataManagerModule());

        subject = new SettingsViewModel(activity);
    }

    @Test
    public void onViewReadySuccess() throws Exception {
        // Arrange
        Settings mockSettings = mock(Settings.class);
        when(mockSettings.isNotificationsOn()).thenReturn(true);
        //noinspection unchecked
        when(mockSettings.getNotificationTypes()).thenReturn(new ArrayList() {{
            add(1);
            add(32);
        }});
        when(mockSettings.getSms()).thenReturn("sms");
        when(mockSettings.getEmail()).thenReturn("email");
        when(mockSettings.getPasswordHint1()).thenReturn("hint");
        when(settingsDataManager.updateSettings(anyString(), anyString())).thenReturn(Observable.just(mockSettings));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getGuid()).thenReturn("guid");
        when(mockPayload.getSharedKey()).thenReturn("sharedKey");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(activity).setUpUi();
        assertEquals(mockSettings, subject.settings);
    }

    @Test
    public void onViewReadyFailed() throws Exception {
        // Arrange
        Settings settings = new Settings();
        when(settingsDataManager.updateSettings(anyString(), anyString())).thenReturn(Observable.error(new Throwable()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getGuid()).thenReturn("guid");
        when(mockPayload.getSharedKey()).thenReturn("sharedKey");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(activity).setUpUi();
        assertNotSame(settings, subject.settings);
    }

    @Test
    public void getIfFingerprintHardwareAvailable() throws Exception {
        // Arrange
        when(fingerprintHelper.isHardwareDetected()).thenReturn(true);
        // Act
        boolean value = subject.getIfFingerprintHardwareAvailable();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void getIfFingerprintUnlockEnabled() throws Exception {
        // Arrange
        when(fingerprintHelper.getIfFingerprintUnlockEnabled()).thenReturn(true);
        // Act
        boolean value = subject.getIfFingerprintUnlockEnabled();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void setFingerprintUnlockEnabled() throws Exception {
        // Arrange

        // Act
        subject.setFingerprintUnlockEnabled(false);
        // Assert
        verify(fingerprintHelper).setFingerprintUnlockEnabled(false);
        verify(fingerprintHelper).clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
    }

    @Test
    public void onFingerprintClickedAlreadyEnabled() throws Exception {
        // Arrange
        when(fingerprintHelper.getIfFingerprintUnlockEnabled()).thenReturn(true);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).showDisableFingerprintDialog();
    }

    @Test
    public void onFingerprintClickedNoFingerprintsEnrolled() throws Exception {
        // Arrange
        when(fingerprintHelper.getIfFingerprintUnlockEnabled()).thenReturn(false);
        when(fingerprintHelper.areFingerprintsEnrolled()).thenReturn(false);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).showNoFingerprintsAddedDialog();
    }

    @Test
    public void onFingerprintClickedVerifyPin() throws Exception {
        // Arrange
        when(fingerprintHelper.getIfFingerprintUnlockEnabled()).thenReturn(false);
        when(fingerprintHelper.areFingerprintsEnrolled()).thenReturn(true);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).verifyPinCode();
    }

    @Test
    public void pinCodeValidated() throws Exception {
        // Arrange
        CharSequenceX pincode = new CharSequenceX("");
        // Act
        subject.pinCodeValidatedForFingerprint(pincode);
        // Assert
        verify(activity).showFingerprintDialog(pincode);
    }

    @Test
    public void getBtcUnits() throws Exception {
        // Arrange

        // Act
        CharSequence[] value = subject.getBtcUnits();
        // Assert
        assertNotNull(value);
    }

    @Test
    public void getTempPassword() throws Exception {
        // Arrange
        CharSequenceX password = new CharSequenceX("");
        when(payloadManager.getTempPassword()).thenReturn(password);
        // Act
        CharSequenceX value = subject.getTempPassword();
        // Assert
        assertEquals(password, value);
    }

    @Test
    public void getEmail() throws Exception {
        // Arrange
        String email = "email";
        subject.settings = mock(Settings.class);
        when(subject.settings.getEmail()).thenReturn(email);
        // Act
        String value = subject.getEmail();
        // Assert
        assertEquals(email, value);
    }

    @Test
    public void getSms() throws Exception {
        // Arrange
        String sms = "sms";
        subject.settings = mock(Settings.class);
        when(subject.settings.getSms()).thenReturn(sms);
        // Act
        String value = subject.getSms();
        // Assert
        assertEquals(sms, value);
    }

    @Test
    public void getPasswordHint() throws Exception {
        // Arrange
        String hint = "hint";
        subject.settings = mock(Settings.class);
        when(subject.settings.getPasswordHint1()).thenReturn(hint);
        // Act
        String value = subject.getPasswordHint();
        // Assert
        assertEquals(hint, value);
    }

    @Test
    public void isSmsVerified() throws Exception {
        // Arrange
        subject.settings = mock(Settings.class);
        when(subject.settings.isSmsVerified()).thenReturn(true);
        // Act
        boolean value = subject.isSmsVerified();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void getAuthType() throws Exception {
        // Arrange
        subject.settings = mock(Settings.class);
        when(subject.settings.getAuthType()).thenReturn(-1);
        // Act
        int value = subject.getAuthType();
        // Assert
        assertEquals(-1, value);
    }

    @Test
    public void updateEmailInvalid() throws Exception {
        // Arrange

        // Act
        subject.updateEmail(null);
        // Assert
        verify(activity).setEmailSummary(anyString());
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updatePreferencesString() throws Exception {
        // Arrange
        subject.settings = new Settings();
        // Act
        subject.updatePreferences("key", "value");
        // Assert
        verify(prefsUtil).setValue(anyString(), anyString());
    }

    @Test
    public void updatePreferencesInt() throws Exception {
        // Arrange
        subject.settings = new Settings();
        // Act
        subject.updatePreferences("key", -1);
        // Assert
        verify(prefsUtil).setValue(anyString(), anyInt());
    }

    @Test
    public void updateEmailSuccess() throws Exception {
        // Arrange
        when(settingsDataManager.updateEmail(anyString())).thenReturn(Observable.just(true));
        when(settingsDataManager.updateNotifications(anyInt(), anyBoolean())).thenReturn(Observable.just(true));
        // Act
        subject.updateEmail("email");
        // Assert
        verify(activity).showDialogEmailVerification();
    }

    @Test
    public void updateEmailFailed() throws Exception {
        // Arrange
        when(settingsDataManager.updateEmail(anyString())).thenReturn(Observable.just(false));
        // Act
        subject.updateEmail("email");
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateSmsInvalid() throws Exception {
        // Arrange

        // Act
        subject.updateSms("");
        // Assert
        verify(activity).setSmsSummary(anyString());
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateSmsSuccess() throws Exception {
        // Arrange
        when(settingsDataManager.updateSms(anyString())).thenReturn(Observable.just(true));
        when(settingsDataManager.updateNotifications(anyInt(), anyBoolean())).thenReturn(Observable.just(true));
        // Act
        subject.updateSms("sms");
        // Assert
        verify(activity).showDialogVerifySms();
    }

    @Test
    public void updateSmsFailed() throws Exception {
        // Arrange
        when(settingsDataManager.updateSms(anyString())).thenReturn(Observable.just(false));
        // Act
        subject.updateSms("sms");
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void verifySmsSuccess() throws Exception {
        // Arrange
        when(settingsDataManager.verifySms(anyString())).thenReturn(Observable.just(true));
        when(settingsDataManager.updateNotifications(anyInt(), anyBoolean())).thenReturn(Observable.just(true));
        subject.settings = new Settings();
        // Act
        subject.verifySms("code");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(activity).showDialogSmsVerified();
    }

    @Test
    public void verifySmsFailed() throws Exception {
        // Arrange
        when(settingsDataManager.verifySms(anyString())).thenReturn(Observable.just(false));
        subject.settings = new Settings();
        // Act
        subject.verifySms("code");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        //noinspection WrongConstant
        verify(activity).showWarningDialog(anyInt());
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void verifySmsError() throws Exception {
        // Arrange
        when(settingsDataManager.verifySms(anyString())).thenReturn(Observable.error(new Throwable()));
        subject.settings = new Settings();
        // Act
        subject.verifySms("code");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(activity).showWarningDialog(anyInt());
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateTorSuccess() throws Exception {
        // Arrange
        when(settingsDataManager.updateTor(anyBoolean())).thenReturn(Observable.just(true));
        subject.settings = new Settings();
        // Act
        subject.updateTor(true);
        // Assert
        verify(activity).setTorBlocked(anyBoolean());
    }

    @Test
    public void updateTorFailed() throws Exception {
        // Arrange
        when(settingsDataManager.updateTor(anyBoolean())).thenReturn(Observable.just(false));
        // Act
        subject.updateTor(true);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updatePasswordHintInvalid() throws Exception {
        // Arrange
        String veryLongString = "12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890";
        // Act
        subject.updatePasswordHint(veryLongString);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updatePasswordHintSuccess() throws Exception {
        // Arrange
        when(settingsDataManager.updatePasswordHint(anyString())).thenReturn(Observable.just(true));
        subject.settings = new Settings();
        // Act
        subject.updatePasswordHint("hint");
        // Assert
        verify(activity).setPasswordHintSummary(anyString());
    }

    @Test
    public void updatePasswordHintFailed() throws Exception {
        // Arrange
        when(settingsDataManager.updatePasswordHint(anyString())).thenReturn(Observable.just(false));
        // Act
        subject.updatePasswordHint("hint");
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void update2FaSuccess() throws Exception {
        // Arrange
        when(settingsDataManager.updateTwoFactor(anyInt())).thenReturn(Observable.just(true));
        subject.settings = new Settings();
        // Act
        subject.updateTwoFa(0);
        // Assert
        verify(activity).setTwoFaSummary(anyString());
    }

    @Test
    public void update2FaFailed() throws Exception {
        // Arrange
        when(settingsDataManager.updateTwoFactor(anyInt())).thenReturn(Observable.just(false));
        // Act
        subject.updateTwoFa(0);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateNotificationSuccess() throws Exception {
        // Arrange
        when(settingsDataManager.updateNotifications(anyInt(), anyBoolean())).thenReturn(Observable.just(true));
        subject.settings = new Settings();
        // Act
        subject.updateNotification(0, true);
        // Assert
        verify(activity).setSmsNotificationPref(anyBoolean());
        verify(activity).setEmailNotificationPref(anyBoolean());
    }

    @Test
    public void updateNotificationFailed() throws Exception {
        // Arrange
        when(settingsDataManager.updateNotifications(anyInt(), anyBoolean())).thenReturn(Observable.just(false));
        // Act
        subject.updateNotification(0, true);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void pinCodeValidatedForChange() throws Exception {
        // Arrange

        // Act
        subject.pinCodeValidatedForChange();
        // Assert
        verify(prefsUtil).removeValue(PrefsUtil.KEY_PIN_FAILS);
        verify(prefsUtil).removeValue(PrefsUtil.KEY_PIN_IDENTIFIER);
        verify(activity).goToPinEntryPage();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updatePasswordSuccess() throws Exception {
        // Arrange
        CharSequenceX newPassword = new CharSequenceX("new password");
        CharSequenceX oldPassword = new CharSequenceX("old password");
        when(accessState.getPIN()).thenReturn("1234");
        when(accessState.createPin(any(CharSequenceX.class), anyString())).thenReturn(Observable.just(true));
        when(accessState.syncPayloadToServer()).thenReturn(Observable.just(true));
        // Act
        subject.updatePassword(newPassword, oldPassword);
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(payloadManager).setTempPassword(newPassword);
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
    }

    @Test
    public void updatePasswordFailed() throws Exception {
        // Arrange
        CharSequenceX newPassword = new CharSequenceX("new password");
        CharSequenceX oldPassword = new CharSequenceX("old password");
        when(accessState.getPIN()).thenReturn("1234");
        when(accessState.createPin(any(CharSequenceX.class), anyString())).thenReturn(Observable.just(false));
        // Act
        subject.updatePassword(newPassword, oldPassword);
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(payloadManager).setTempPassword(oldPassword);
        //noinspection WrongConstant
        verify(activity, times(2)).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void updatePasswordError() throws Exception {
        // Arrange
        CharSequenceX newPassword = new CharSequenceX("new password");
        CharSequenceX oldPassword = new CharSequenceX("old password");
        when(accessState.getPIN()).thenReturn("1234");
        when(accessState.createPin(any(CharSequenceX.class), anyString())).thenReturn(Observable.error(new Throwable()));
        // Act
        subject.updatePassword(newPassword, oldPassword);
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(payloadManager).setTempPassword(oldPassword);
        //noinspection WrongConstant
        verify(activity, times(2)).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    private class MockDataManagerModule extends DataManagerModule {
        @Override
        protected FingerprintHelper provideFingerprintHelper(Context applicationContext, PrefsUtil prefsUtil) {
            return fingerprintHelper;
        }

        @Override
        protected SettingsDataManager provideSettingsDataManager() {
            return settingsDataManager;
        }
    }

    private class MockApiModule extends ApiModule {
        @Override
        protected PayloadManager providePayloadManager() {
            return payloadManager;
        }
    }

    private class MockApplicationModule extends ApplicationModule {
        public MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return prefsUtil;
        }

        @Override
        protected AccessState provideAccessState() {
            return accessState;
        }

        @Override
        protected StringUtils provideStringUtils() {
            return stringUtils;
        }
    }
}
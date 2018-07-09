package piuk.blockchain.android.ui.settings;

import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.settings.SettingsManager;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.R;
import piuk.blockchain.android.testutils.RxTest;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.data.auth.AuthDataManager;
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager;
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager;
import piuk.blockchain.android.data.notifications.NotificationTokenManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.settings.SettingsDataManager;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.androidcore.utils.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SettingsPresenterTest extends RxTest {

    private SettingsPresenter subject;
    @Mock private SettingsView activity;
    @Mock private FingerprintHelper fingerprintHelper;
    @Mock private AuthDataManager authDataManager;
    @Mock private SettingsDataManager settingsDataManager;
    @Mock private PayloadManager payloadManager;
    @Mock private PayloadDataManager payloadDataManager;
    @Mock private StringUtils stringUtils;
    @Mock private PrefsUtil prefsUtil;
    @Mock private AccessState accessState;
    @Mock private SwipeToReceiveHelper swipeToReceiveHelper;
    @Mock private NotificationTokenManager notificationTokenManager;
    @Mock private ExchangeRateDataManager exchangeRateDataManager;
    @Mock private CurrencyFormatManager currencyFormatManager;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new SettingsPresenter(fingerprintHelper,
                authDataManager,
                settingsDataManager,
                payloadManager,
                payloadDataManager,
                stringUtils,
                prefsUtil,
                accessState,
                swipeToReceiveHelper,
                notificationTokenManager,
                exchangeRateDataManager,
                currencyFormatManager);
        subject.initView(activity);
    }

    @Test
    public void onViewReadySuccess() throws Exception {
        // Arrange
        Settings mockSettings = mock(Settings.class);
        when(mockSettings.isNotificationsOn()).thenReturn(true);
        //noinspection unchecked
        when(mockSettings.getNotificationsType()).thenReturn(new ArrayList<Integer>() {{
            add(1);
            add(32);
        }});
        when(mockSettings.getSmsNumber()).thenReturn("sms");
        when(mockSettings.getEmail()).thenReturn("email");
        when(settingsDataManager.getSettings()).thenReturn(Observable.just(mockSettings));
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
        when(settingsDataManager.getSettings()).thenReturn(Observable.error(new Throwable()));
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
        when(fingerprintHelper.isFingerprintUnlockEnabled()).thenReturn(true);
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
        when(fingerprintHelper.isFingerprintUnlockEnabled()).thenReturn(true);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).showDisableFingerprintDialog();
    }

    @Test
    public void onFingerprintClickedNoFingerprintsEnrolled() throws Exception {
        // Arrange
        when(fingerprintHelper.isFingerprintUnlockEnabled()).thenReturn(false);
        when(fingerprintHelper.areFingerprintsEnrolled()).thenReturn(false);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).showNoFingerprintsAddedDialog();
    }

    @Test
    public void onFingerprintClickedPinStored() throws Exception {
        // Arrange
        String pinCode = "1234";
        when(fingerprintHelper.isFingerprintUnlockEnabled()).thenReturn(false);
        when(fingerprintHelper.areFingerprintsEnrolled()).thenReturn(true);
        when(accessState.getPIN()).thenReturn(pinCode);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).showFingerprintDialog(captor.capture());
        assertEquals(pinCode, captor.getValue());
    }

    @Test(expected = IllegalStateException.class)
    public void onFingerprintClickedPinNotFound() throws Exception {
        // Arrange
        when(fingerprintHelper.isFingerprintUnlockEnabled()).thenReturn(false);
        when(fingerprintHelper.areFingerprintsEnrolled()).thenReturn(true);
        when(accessState.getPIN()).thenReturn(null);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(fingerprintHelper).isFingerprintUnlockEnabled();
        verify(fingerprintHelper).areFingerprintsEnrolled();
        verify(accessState).getPIN();
    }

    @Test
    public void getTempPassword() throws Exception {
        // Arrange
        String password = "PASSWORD";
        when(payloadManager.getTempPassword()).thenReturn(password);
        // Act
        String value = subject.getTempPassword();
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
        when(subject.settings.getSmsNumber()).thenReturn(sms);
        // Act
        String value = subject.getSms();
        // Assert
        assertEquals(sms, value);
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
    public void updatePreferencesString() throws Exception {
        // Arrange
        subject.settings = new Settings();
        String key = "KEY";
        String value = "VALUE";
        // Act
        subject.updatePreferences(key, value);
        // Assert
        verify(prefsUtil).setValue(key, value);
    }

    @Test
    public void updatePreferencesInt() throws Exception {
        // Arrange
        subject.settings = new Settings();
        String key = "KEY";
        int value = 1337;
        // Act
        subject.updatePreferences(key, value);
        // Assert
        verify(prefsUtil).setValue(key, value);
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void updatePreferencesBoolean() throws Exception {
        // Arrange
        subject.settings = new Settings();
        String key = "KEY";
        boolean value = false;
        // Act
        subject.updatePreferences(key, value);
        // Assert
        verify(prefsUtil).setValue(key, value);
    }

    @Test
    public void updateEmailInvalid() throws Exception {
        // Arrange
        String stringResource = "STRING_RESOURCE";
        when(stringUtils.getString(anyInt())).thenReturn(stringResource);
        // Act
        subject.updateEmail(null);
        // Assert
        verify(activity).setEmailSummary(stringResource);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateEmailSuccess() throws Exception {
        // Arrange
        Settings mockSettings = mock(Settings.class);
        ArrayList<Integer> notifications = new ArrayList<Integer>() {{
            add(SettingsManager.NOTIFICATION_TYPE_EMAIL);
        }};
        when(mockSettings.getNotificationsType()).thenReturn(notifications);
        String email = "EMAIL";
        when(settingsDataManager.updateEmail(email)).thenReturn(Observable.just(mockSettings));
        when(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications))
                .thenReturn(Observable.just(mockSettings));
        // Act
        subject.updateEmail(email);
        // Assert
        verify(settingsDataManager).updateEmail(email);
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications);
        verify(activity).showDialogEmailVerification();
    }

    @Test
    public void updateEmailFailed() throws Exception {
        // Arrange
        String email = "EMAIL";
        when(settingsDataManager.updateEmail(email)).thenReturn(Observable.error(new Throwable()));
        // Act
        subject.updateEmail(email);
        // Assert
        verify(settingsDataManager).updateEmail(email);
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateSmsInvalid() throws Exception {
        // Arrange
        String stringResource = "STRING_RESOURCE";
        when(stringUtils.getString(anyInt())).thenReturn(stringResource);
        // Act
        subject.updateSms("");
        // Assert
        verify(activity).setSmsSummary(stringResource);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateSmsSuccess() throws Exception {
        // Arrange
        Settings mockSettings = mock(Settings.class);
        ArrayList<Integer> notifications = new ArrayList<Integer>() {{
            add(SettingsManager.NOTIFICATION_TYPE_SMS);
        }};
        when(mockSettings.getNotificationsType()).thenReturn(notifications);
        String phoneNumber = "PHONE_NUMBER";
        when(settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.just(mockSettings));
        when(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications))
                .thenReturn(Observable.just(mockSettings));
        // Act
        subject.updateSms(phoneNumber);
        // Assert
        verify(settingsDataManager).updateSms(phoneNumber);
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications);
        verify(activity).showDialogVerifySms();
    }

    @Test
    public void updateSmsFailed() throws Exception {
        // Arrange
        String phoneNumber = "PHONE_NUMBER";
        when(settingsDataManager.updateSms(phoneNumber)).thenReturn(Observable.error(new Throwable()));
        // Act
        subject.updateSms(phoneNumber);
        // Assert
        verify(settingsDataManager).updateSms(phoneNumber);
        verifyNoMoreInteractions(settingsDataManager);
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void verifySmsSuccess() throws Exception {
        // Arrange
        String verificationCode = "VERIFICATION_CODE";
        Settings mockSettings = mock(Settings.class);
        when(settingsDataManager.verifySms(verificationCode)).thenReturn(Observable.just(mockSettings));
        // Act
        subject.verifySms(verificationCode);
        // Assert
        verify(settingsDataManager).verifySms(verificationCode);
        verifyNoMoreInteractions(settingsDataManager);
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(activity).showDialogSmsVerified();
    }

    @Test
    public void verifySmsFailed() throws Exception {
        // Arrange
        String verificationCode = "VERIFICATION_CODE";
        when(settingsDataManager.verifySms(anyString())).thenReturn(Observable.error(new Throwable()));
        subject.settings = mock(Settings.class);
        // Act
        subject.verifySms(verificationCode);
        // Assert
        verify(settingsDataManager).verifySms(verificationCode);
        verifyNoMoreInteractions(settingsDataManager);
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        //noinspection WrongConstant
        verify(activity).showWarningDialog(anyInt());
    }

    @Test
    public void updateTorSuccess() throws Exception {
        // Arrange
        Settings mockSettings = mock(Settings.class);
        when(mockSettings.isBlockTorIps()).thenReturn(true);
        when(settingsDataManager.updateTor(true)).thenReturn(Observable.just(mockSettings));
        subject.settings = mock(Settings.class);
        // Act
        subject.updateTor(true);
        // Assert
        verify(settingsDataManager).updateTor(true);
        verify(activity).setTorBlocked(true);
    }

    @Test
    public void updateTorFailed() throws Exception {
        // Arrange
        when(settingsDataManager.updateTor(true)).thenReturn(Observable.error(new Throwable()));
        subject.settings = mock(Settings.class);
        // Act
        subject.updateTor(true);
        // Assert
        verify(settingsDataManager).updateTor(true);
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void update2FaSuccess() throws Exception {
        // Arrange
        Settings mockSettings = mock(Settings.class);
        int authType = SettingsManager.AUTH_TYPE_YUBI_KEY;
        when(settingsDataManager.updateTwoFactor(authType)).thenReturn(Observable.just(mockSettings));
        subject.settings = mock(Settings.class);
        // Act
        subject.updateTwoFa(authType);
        // Assert
        verify(settingsDataManager).updateTwoFactor(authType);
    }

    @Test
    public void update2FaFailed() throws Exception {
        // Arrange
        int authType = SettingsManager.AUTH_TYPE_YUBI_KEY;
        when(settingsDataManager.updateTwoFactor(authType)).thenReturn(Observable.error(new Throwable()));
        subject.settings = mock(Settings.class);
        // Act
        subject.updateTwoFa(authType);
        // Assert
        verify(settingsDataManager).updateTwoFactor(authType);
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void enableNotificationSuccess() throws Exception {
        // Arrange
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        Settings mockSettingsResponse = mock(Settings.class);
        Settings mockSettings = mock(Settings.class);
        ArrayList<Integer> notifications = new ArrayList<Integer>() {{
            add(SettingsManager.NOTIFICATION_TYPE_NONE);
        }};
        when(mockSettings.getNotificationsType()).thenReturn(notifications);
        subject.settings = mockSettings;
        when(settingsDataManager.enableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL, notifications))
                .thenReturn(Observable.just(mockSettingsResponse));
        // Act
        subject.updateNotification(notificationType, true);
        // Assert
        verify(settingsDataManager).enableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL, notifications);
        verify(payloadDataManager).syncPayloadAndPublicKeys();
        verify(activity).setEmailNotificationPref(anyBoolean());
    }

    @Test
    public void disableNotificationSuccess() throws Exception {
        // Arrange
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        Settings mockSettingsResponse = mock(Settings.class);
        Settings mockSettings = mock(Settings.class);
        ArrayList<Integer> notifications = new ArrayList<Integer>() {{
            add(SettingsManager.NOTIFICATION_TYPE_EMAIL);
        }};
        when(mockSettings.getNotificationsType()).thenReturn(notifications);
        subject.settings = mockSettings;
        when(settingsDataManager.disableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL, notifications))
                .thenReturn(Observable.just(mockSettingsResponse));
        // Act
        subject.updateNotification(notificationType, false);
        // Assert
        verify(settingsDataManager).disableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL, notifications);
        verify(payloadDataManager).syncPayloadWithServer();
        verify(activity).setEmailNotificationPref(anyBoolean());
    }

    @Test
    public void enableNotificationAlreadyEnabled() throws Exception {
        // Arrange
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        Settings mockSettings = mock(Settings.class);
        ArrayList<Integer> notifications = new ArrayList<Integer>() {{
            add(SettingsManager.NOTIFICATION_TYPE_EMAIL);
        }};
        when(mockSettings.getNotificationsType()).thenReturn(notifications);
        when(mockSettings.isNotificationsOn()).thenReturn(true);
        subject.settings = mockSettings;
        // Act
        subject.updateNotification(notificationType, true);
        // Assert
        verifyZeroInteractions(settingsDataManager);
        verify(activity, times(2)).setEmailNotificationPref(anyBoolean());
    }

    @Test
    public void disableNotificationAlreadyDisabled() throws Exception {
        // Arrange
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        Settings mockSettings = mock(Settings.class);
        ArrayList<Integer> notifications = new ArrayList<Integer>() {{
            add(SettingsManager.NOTIFICATION_TYPE_NONE);
        }};
        when(mockSettings.getNotificationsType()).thenReturn(notifications);
        when(mockSettings.isNotificationsOn()).thenReturn(true);
        subject.settings = mockSettings;
        // Act
        subject.updateNotification(notificationType, false);
        // Assert
        verifyZeroInteractions(settingsDataManager);
        verify(activity).setEmailNotificationPref(anyBoolean());
    }

    @Test
    public void enableNotificationFailed() throws Exception {
        // Arrange
        int notificationType = SettingsManager.NOTIFICATION_TYPE_EMAIL;
        Settings mockSettings = mock(Settings.class);
        ArrayList<Integer> notifications = new ArrayList<Integer>() {{
            add(SettingsManager.NOTIFICATION_TYPE_NONE);
        }};
        when(mockSettings.getNotificationsType()).thenReturn(notifications);
        subject.settings = mockSettings;
        when(settingsDataManager.enableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL, notifications))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.updateNotification(notificationType, true);
        // Assert
        verify(settingsDataManager).enableNotification(SettingsManager.NOTIFICATION_TYPE_EMAIL, notifications);
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
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
        String newPassword = "NEW_PASSWORD";
        String oldPassword = "OLD_PASSWORD";
        String pin = "PIN";
        when(accessState.getPIN()).thenReturn(pin);
        when(authDataManager.createPin(newPassword, pin)).thenReturn(Completable.complete());
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        // Act
        subject.updatePassword(newPassword, oldPassword);
        // Assert
        //noinspection ResultOfMethodCallIgnored
        verify(accessState).getPIN();
        verify(authDataManager).createPin(newPassword, pin);
        verify(payloadDataManager).syncPayloadWithServer();
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
    }

    @Test
    public void updatePasswordFailed() throws Exception {
        // Arrange
        String newPassword = "NEW_PASSWORD";
        String oldPassword = "OLD_PASSWORD";
        String pin = "PIN";
        when(accessState.getPIN()).thenReturn(pin);
        when(authDataManager.createPin(newPassword, pin))
                .thenReturn(Completable.error(new Throwable()));
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        // Act
        subject.updatePassword(newPassword, oldPassword);
        // Assert
        //noinspection ResultOfMethodCallIgnored
        verify(accessState).getPIN();
        verify(authDataManager).createPin(newPassword, pin);
        verify(payloadDataManager).syncPayloadWithServer();
        verify(payloadManager).setTempPassword(newPassword);
        verify(payloadManager).setTempPassword(oldPassword);
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        //noinspection WrongConstant
        verify(activity, times(2)).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void storeSwipeToReceiveAddressesSuccessful() throws Exception {
        // Arrange

        // Act
        subject.storeSwipeToReceiveAddresses();
        getTestScheduler().triggerActions();
        // Assert
        verify(swipeToReceiveHelper).updateAndStoreBitcoinAddresses();
        verify(swipeToReceiveHelper).storeEthAddress();
        verifyNoMoreInteractions(swipeToReceiveHelper);
        verify(activity).showProgressDialog(R.string.please_wait);
        verify(activity).hideProgressDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void storeSwipeToReceiveAddressesFailed() throws Exception {
        // Arrange
        doThrow(NullPointerException.class).when(swipeToReceiveHelper).updateAndStoreBitcoinAddresses();
        // Act
        subject.storeSwipeToReceiveAddresses();
        getTestScheduler().triggerActions();
        // Assert
        verify(swipeToReceiveHelper).updateAndStoreBitcoinAddresses();
        verifyNoMoreInteractions(swipeToReceiveHelper);
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void clearSwipeToReceiveData() throws Exception {
        // Arrange

        // Act
        subject.clearSwipeToReceiveData();
        // Assert
        verify(prefsUtil).removeValue(SwipeToReceiveHelper.KEY_SWIPE_RECEIVE_ACCOUNT_NAME);
        verify(prefsUtil).removeValue(SwipeToReceiveHelper.KEY_SWIPE_RECEIVE_ADDRESSES);
        verify(prefsUtil).removeValue(SwipeToReceiveHelper.KEY_SWIPE_RECEIVE_ETH_ADDRESS);
    }

    @Test
    public void enablePushNotifications() {
        // Arrange
        when(notificationTokenManager.enableNotifications()).thenReturn(Completable.complete());

        // Act
        subject.enablePushNotifications();

        // Assert
        verify(activity).setPushNotificationPref(true);
        verify(notificationTokenManager).enableNotifications();
        verifyNoMoreInteractions(notificationTokenManager);
    }

    @Test
    public void disablePushNotifications() {
        // Arrange
        when(notificationTokenManager.disableNotifications()).thenReturn(Completable.complete());

        // Act
        subject.disablePushNotifications();

        // Assert
        verify(activity).setPushNotificationPref(false);
        verify(notificationTokenManager).disableNotifications();
        verifyNoMoreInteractions(notificationTokenManager);
    }
}
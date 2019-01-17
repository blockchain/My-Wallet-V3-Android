package piuk.blockchain.android.ui.settings;

import com.blockchain.kyc.models.nabu.Kyc2TierState;
import com.blockchain.kyc.models.nabu.NabuApiException;
import com.blockchain.kycui.settings.KycStatusHelper;
import com.blockchain.notifications.NotificationTokenManager;
import info.blockchain.wallet.api.data.Settings;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.settings.SettingsManager;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import piuk.blockchain.android.R;
import piuk.blockchain.android.testutils.RxTest;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.androidcore.data.access.AccessState;
import piuk.blockchain.androidcore.data.auth.AuthDataManager;
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager;
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.settings.Email;
import piuk.blockchain.androidcore.data.settings.EmailSyncUpdater;
import piuk.blockchain.androidcore.data.settings.SettingsDataManager;
import piuk.blockchain.androidcore.utils.PrefsUtil;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import retrofit2.Response;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SettingsPresenterTest extends RxTest {

    private SettingsPresenter subject;
    @Mock
    private SettingsView activity;
    @Mock
    private FingerprintHelper fingerprintHelper;
    @Mock
    private AuthDataManager authDataManager;
    @Mock
    private SettingsDataManager settingsDataManager;
    @Mock
    private PayloadManager payloadManager;
    @Mock
    private PayloadDataManager payloadDataManager;
    @Mock
    private StringUtils stringUtils;
    @Mock
    private PrefsUtil prefsUtil;
    @Mock
    private AccessState accessState;
    @Mock
    private SwipeToReceiveHelper swipeToReceiveHelper;
    @Mock
    private NotificationTokenManager notificationTokenManager;
    @Mock
    private ExchangeRateDataManager exchangeRateDataManager;
    @Mock
    private CurrencyFormatManager currencyFormatManager;
    @Mock
    private KycStatusHelper kycStatusHelper;
    @Mock
    private EmailSyncUpdater emailSyncUpdater;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        subject = new SettingsPresenter(fingerprintHelper,
                authDataManager,
                settingsDataManager,
                emailSyncUpdater,
                payloadManager,
                payloadDataManager,
                stringUtils,
                prefsUtil,
                accessState,
                swipeToReceiveHelper,
                notificationTokenManager,
                exchangeRateDataManager,
                currencyFormatManager,
                kycStatusHelper);
        subject.initView(activity);
    }

    @Test
    public void onViewReadySuccess() {
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
        when(settingsDataManager.fetchSettings()).thenReturn(Observable.just(mockSettings));
        when(kycStatusHelper.getSettingsKycState2Tier()).thenReturn(Single.just(Kyc2TierState.Hidden));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(activity).setUpUi();
        assertEquals(mockSettings, subject.settings);
    }

    @Test
    public void onViewReadyFailed() {
        // Arrange
        Settings settings = new Settings();
        when(settingsDataManager.fetchSettings()).thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(activity).setUpUi();
        assertNotSame(settings, subject.settings);
    }

    @Test
    public void onKycStatusClicked_should_launch_homebrew_tier1() {
        assertClickLaunchesKyc(Kyc2TierState.Tier1Approved);
    }

    @Test
    public void onKycStatusClicked_should_launch_homebrew_tier2() {
        assertClickLaunchesKyc(Kyc2TierState.Tier2Approved);
    }

    @Test
    public void onKycStatusClicked_should_launch_kyc_flow_locked() {
        assertClickLaunchesKyc(Kyc2TierState.Locked);
    }

    @Test
    public void onKycStatusClicked_should_launch_kyc_status_tier1_review() {
        assertClickLaunchesKyc(Kyc2TierState.Tier1InReview);
    }

    @Test
    public void onKycStatusClicked_should_launch_kyc_status_tier2_review() {
        assertClickLaunchesKyc(Kyc2TierState.Tier2InReview);
    }

    @Test
    public void onKycStatusClicked_should_launch_kyc_status_tier1_rejected() {
        assertClickLaunchesKyc(Kyc2TierState.Tier1Failed);
    }

    @Test
    public void onKycStatusClicked_should_launch_kyc_status_tier2_rejected() {
        assertClickLaunchesKyc(Kyc2TierState.Tier2Failed);
    }

    private void assertClickLaunchesKyc(Kyc2TierState status) {
        // Arrange
        when(kycStatusHelper.getKyc2TierStatus())
                .thenReturn(Single.just(status));
        // Act
        subject.onKycStatusClicked();
        // Assert
        verify(activity).launchKycFlow();
    }

    @Test
    public void getIfFingerprintHardwareAvailable() {
        // Arrange
        when(fingerprintHelper.isHardwareDetected()).thenReturn(true);
        // Act
        boolean value = subject.getIfFingerprintHardwareAvailable();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void getIfFingerprintUnlockEnabled() {
        // Arrange
        when(fingerprintHelper.isFingerprintUnlockEnabled()).thenReturn(true);
        // Act
        boolean value = subject.getIfFingerprintUnlockEnabled();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void setFingerprintUnlockEnabled() {
        // Arrange

        // Act
        subject.setFingerprintUnlockEnabled(false);
        // Assert
        verify(fingerprintHelper).setFingerprintUnlockEnabled(false);
        verify(fingerprintHelper).clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
    }

    @Test
    public void onFingerprintClickedAlreadyEnabled() {
        // Arrange
        when(fingerprintHelper.isFingerprintUnlockEnabled()).thenReturn(true);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).showDisableFingerprintDialog();
    }

    @Test
    public void onFingerprintClickedNoFingerprintsEnrolled() {
        // Arrange
        when(fingerprintHelper.isFingerprintUnlockEnabled()).thenReturn(false);
        when(fingerprintHelper.areFingerprintsEnrolled()).thenReturn(false);
        // Act
        subject.onFingerprintClicked();
        // Assert
        verify(activity).showNoFingerprintsAddedDialog();
    }

    @Test
    public void onFingerprintClickedPinStored() {
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
    public void onFingerprintClickedPinNotFound() {
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
    public void getTempPassword() {
        // Arrange
        String password = "PASSWORD";
        when(payloadManager.getTempPassword()).thenReturn(password);
        // Act
        String value = subject.getTempPassword();
        // Assert
        assertEquals(password, value);
    }

    @Test
    public void getEmail() {
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
    public void getSms() {
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
    public void isSmsVerified() {
        // Arrange
        subject.settings = mock(Settings.class);
        when(subject.settings.isSmsVerified()).thenReturn(true);
        // Act
        boolean value = subject.isSmsVerified();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void isEmailVerified() {
        // Arrange
        subject.settings = mock(Settings.class);
        when(subject.settings.isEmailVerified()).thenReturn(true);
        // Act
        boolean value = subject.isEmailVerified();
        // Assert
        assertTrue(value);
    }

    @Test
    public void getAuthType() {
        // Arrange
        subject.settings = mock(Settings.class);
        when(subject.settings.getAuthType()).thenReturn(-1);
        // Act
        int value = subject.getAuthType();
        // Assert
        assertEquals(-1, value);
    }

    @Test
    public void updatePreferencesString() {
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
    public void updatePreferencesInt() {
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
    public void updatePreferencesBoolean() {
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
    public void updateEmailInvalid() {
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
    public void updateEmailSuccess() {
        // Arrange
        Settings mockSettings = mock(Settings.class);
        ArrayList<Integer> notifications = new ArrayList<Integer>() {{
            add(SettingsManager.NOTIFICATION_TYPE_EMAIL);
        }};
        when(mockSettings.getNotificationsType()).thenReturn(notifications);
        String email = "EMAIL";
        when(emailSyncUpdater.updateEmailAndSync(email)).thenReturn(Single.just(new Email(email, false)));
        when(settingsDataManager.fetchSettings()).thenReturn(Observable.just(mockSettings));
        when(settingsDataManager.disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications))
                .thenReturn(Observable.just(mockSettings));
        // Act
        subject.updateEmail(email);
        // Assert
        verify(emailSyncUpdater).updateEmailAndSync(email);
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_EMAIL, notifications);
        verify(activity).showDialogEmailVerification();
    }

    @Test
    public void updateEmailFailed() {
        // Arrange
        String email = "EMAIL";
        when(emailSyncUpdater.updateEmailAndSync(email)).thenReturn(Single.error(new Throwable()));
        // Act
        subject.updateEmail(email);
        // Assert
        verify(emailSyncUpdater).updateEmailAndSync(email);
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateSmsInvalid() {
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
    public void updateSmsSuccess() {
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
        when(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.complete());
        // Act
        subject.updateSms(phoneNumber);
        // Assert
        verify(settingsDataManager).updateSms(phoneNumber);
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications);
        verify(activity).showDialogVerifySms();
    }

    @Test
    public void updateSmsSuccess_despiteNumberAlreadyRegistered() {
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
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        NabuApiException error = NabuApiException.Companion.fromResponseBody(Response.error(409, responseBody));
        when(kycStatusHelper.syncPhoneNumberWithNabu())
                .thenReturn(Completable.error(error));
        // Act
        subject.updateSms(phoneNumber);
        // Assert
        verify(settingsDataManager).updateSms(phoneNumber);
        verify(settingsDataManager).disableNotification(Settings.NOTIFICATION_TYPE_SMS, notifications);
        verify(activity).showDialogVerifySms();
    }

    @Test
    public void updateSmsFailed() {
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
    public void verifySmsSuccess() {
        // Arrange
        String verificationCode = "VERIFICATION_CODE";
        Settings mockSettings = mock(Settings.class);
        when(settingsDataManager.verifySms(verificationCode)).thenReturn(Observable.just(mockSettings));
        when(kycStatusHelper.syncPhoneNumberWithNabu()).thenReturn(Completable.complete());
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
    public void verifySmsFailed() {
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
    public void updateTorSuccess() {
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
    public void updateTorFailed() {
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
    public void update2FaSuccess() {
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
    public void update2FaFailed() {
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
    public void enableNotificationSuccess() {
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
    public void disableNotificationSuccess() {
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
    public void enableNotificationAlreadyEnabled() {
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
    public void disableNotificationAlreadyDisabled() {
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
    public void enableNotificationFailed() {
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
    public void pinCodeValidatedForChange() {
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
    public void updatePasswordSuccess() {
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
    public void updatePasswordFailed() {
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
    public void storeSwipeToReceiveAddressesSuccessful() {
        // Arrange
        when(swipeToReceiveHelper.storeAll()).thenReturn(Completable.complete());
        // Act
        subject.storeSwipeToReceiveAddresses();
        getTestScheduler().triggerActions();
        // Assert
        verify(swipeToReceiveHelper).storeAll();
        verifyNoMoreInteractions(swipeToReceiveHelper);
        verify(activity).showProgressDialog(R.string.please_wait);
        verify(activity).hideProgressDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void storeSwipeToReceiveAddressesFailed() {
        // Arrange
        when(swipeToReceiveHelper.storeAll()).thenReturn(Completable.error(new Throwable()));
        // Act
        subject.storeSwipeToReceiveAddresses();
        getTestScheduler().triggerActions();
        // Assert
        verify(swipeToReceiveHelper).storeAll();
        verifyNoMoreInteractions(swipeToReceiveHelper);
        verify(activity).showProgressDialog(anyInt());
        verify(activity).hideProgressDialog();
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void clearSwipeToReceiveData() {
        // Arrange

        // Act
        subject.clearSwipeToReceiveData();
        // Assert
        swipeToReceiveHelper.clearStoredData();
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
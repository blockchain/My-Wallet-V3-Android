package piuk.blockchain.android.ui.auth;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;

import info.blockchain.wallet.exceptions.AccountLockedException;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.HDWallet;
import info.blockchain.wallet.payload.data.Wallet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.spongycastle.crypto.InvalidCipherTextException;

import java.util.ArrayList;
import java.util.Collections;

import javax.inject.Named;

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.SSLVerifyUtil;
import piuk.blockchain.android.util.StringUtils;
import retrofit2.Retrofit;

import static io.reactivex.Observable.just;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_EMAIL;
import static piuk.blockchain.android.ui.auth.CreateWalletFragment.KEY_INTENT_PASSWORD;
import static piuk.blockchain.android.ui.auth.LandingActivity.KEY_INTENT_RECOVERING_FUNDS;
import static piuk.blockchain.android.ui.auth.PinEntryFragment.KEY_VALIDATING_PIN_FOR_RESULT;

@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class PinEntryViewModelTest {

    private PinEntryViewModel subject;

    @Mock private PinEntryViewModel.DataListener activity;
    @Mock private AuthDataManager authDataManager;
    @Mock private AppUtil appUtil;
    @Mock private PrefsUtil prefsUtil;
    @Mock private PayloadManager payloadManager;
    @Mock private StringUtils stringUtils;
    @Mock private FingerprintHelper fingerprintHelper;
    @Mock private AccessState accessState;
    @Mock private SSLVerifyUtil sslVerifyUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new MockDataManagerModule());

        ImageView mockImageView = mock(ImageView.class);
        when(activity.getPinBoxArray())
                .thenReturn(new ImageView[]{mockImageView, mockImageView, mockImageView, mockImageView});
        when(stringUtils.getString(anyInt())).thenReturn("string resource");

        subject = new PinEntryViewModel(activity);
    }

    @Test
    public void onViewReadyEmailAndPasswordInIntentCreateWalletSuccessful() throws Exception {
        // Arrange
        String email = "example@email.com";
        String password = "1234567890";
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_EMAIL, email);
        intent.putExtra(KEY_INTENT_PASSWORD, password);
        when(activity.getPageIntent()).thenReturn(intent);
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(fingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn("");
        when(authDataManager.createHdWallet(anyString(), anyString(), eq(email)))
                .thenReturn(just(new Wallet()));
        // Act
        subject.onViewReady();
        // Assert
        assertEquals(false, subject.allowExit());
        verify(authDataManager).createHdWallet(anyString(), anyString(), eq(email));
        verify(activity).showProgressDialog(anyInt(), anyString());
        verify(activity).dismissProgressDialog();
        verify(prefsUtil).setValue(PrefsUtil.KEY_EMAIL, email);
    }

    @Test
    public void onViewReadyValidatingPinForResult() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra(KEY_VALIDATING_PIN_FOR_RESULT, true);
        when(activity.getPageIntent()).thenReturn(intent);
        // Act
        subject.onViewReady();
        // Assert
        assertEquals(true, subject.isForValidatingPinForResult());
    }

    @Test
    public void onViewReadyEmailAndPasswordInIntentCreateWalletThrowsError() throws Exception {
        // Arrange
        String email = "example@email.com";
        String password = "1234567890";
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_EMAIL, email);
        intent.putExtra(KEY_INTENT_PASSWORD, password);
        when(activity.getPageIntent()).thenReturn(intent);
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(fingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn("");
        when(authDataManager.createHdWallet(anyString(), anyString(), eq(email)))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onViewReady();
        // Assert
        assertEquals(false, subject.allowExit());
        verify(activity).showProgressDialog(anyInt(), anyString());
        verify(activity, times(2)).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
    }

    @Test
    public void onViewReadyRecoveringFunds() throws Exception {
        // Arrange
        String email = "example@email.com";
        String password = "1234567890";
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_EMAIL, email);
        intent.putExtra(KEY_INTENT_PASSWORD, password);
        intent.putExtra(KEY_INTENT_RECOVERING_FUNDS, true);
        when(activity.getPageIntent()).thenReturn(intent);
        when(authDataManager.createHdWallet(anyString(), anyString(), eq(email)))
                .thenReturn(just(new Wallet()));
        // Act
        subject.onViewReady();
        // Assert
        assertEquals(false, subject.allowExit());
        verifyNoMoreInteractions(payloadManager);
    }

    @Test
    public void onViewReadyMaxAttemptsExceeded() throws Exception {
        // Arrange
        when(activity.getPageIntent()).thenReturn(new Intent());
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0)).thenReturn(4);
        when(payloadManager.getPayload()).thenReturn(mock(Wallet.class));
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(fingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn("");
        // Act
        subject.onViewReady();
        // Assert
        assertEquals(true, subject.allowExit());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).showMaxAttemptsDialog();
    }

    @Test
    public void checkFingerprintStatusShouldShowDialog() throws Exception {
        // Arrange
        subject.mValidatingPinForResult = false;
        subject.mRecoveringFunds = false;
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234");
        when(fingerprintHelper.isFingerprintUnlockEnabled()).thenReturn(true);
        when(fingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn(null);
        when(fingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn("");
        // Act
        subject.checkFingerprintStatus();
        // Assert
        verify(activity).showFingerprintDialog(anyString());
    }

    @Test
    public void checkFingerprintStatusDontShow() throws Exception {
        // Arrange
        subject.mValidatingPinForResult = true;
        // Act
        subject.checkFingerprintStatus();
        // Assert
        verify(activity).showKeyboard();
    }

    @Test
    public void canShowFingerprintDialog() throws Exception {
        // Arrange
        subject.mCanShowFingerprintDialog = true;
        // Act
        boolean value = subject.canShowFingerprintDialog();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void loginWithDecryptedPin() throws Exception {
        // Arrange
        String pincode = "1234";
        when(authDataManager.validatePin(pincode)).thenReturn(just("password"));
        // Act
        subject.loginWithDecryptedPin(pincode);
        // Assert
        verify(authDataManager).validatePin(pincode);
        verify(activity).getPinBoxArray();
        assertEquals(false, subject.canShowFingerprintDialog());
    }

    @Test
    public void onDeleteClicked() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "1234";
        // Act
        subject.onDeleteClicked();
        // Assert
        assertEquals("123", subject.mUserEnteredPin);
        verify(activity).getPinBoxArray();
    }

    @Test
    public void padClickedPinAlreadyFourDigits() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "0000";
        // Act
        subject.onPadClicked("0");
        // Assert
        verifyZeroInteractions(activity);
    }

    @Test
    public void padClickedAllZeros() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "000";
        // Act
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(fingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn("");
        subject.onPadClicked("0");
        // Assert
        verify(activity).clearPinBoxes();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        assertEquals("", subject.mUserEnteredPin);
        assertEquals(null, subject.mUserEnteredConfirmationPin);
    }

    @Test
    public void padClickedShowCommonPinWarning() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "123";
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("");
        // Act
        subject.onPadClicked("4");
        // Assert
        verify(activity).showCommonPinWarning(any(DialogButtonCallback.class));
    }

    @Test
    public void padClickedShowCommonPinWarningAndClickRetry() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "123";
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("");
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onPositiveClicked();
            return null;
        }).when(activity).showCommonPinWarning(any(DialogButtonCallback.class));
        // Act
        subject.onPadClicked("4");
        // Assert
        verify(activity).showCommonPinWarning(any(DialogButtonCallback.class));
        verify(activity).clearPinBoxes();
        assertEquals("", subject.mUserEnteredPin);
        assertEquals(null, subject.mUserEnteredConfirmationPin);
    }

    @Test
    public void padClickedShowCommonPinWarningAndClickContinue() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "123";
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("");
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onNegativeClicked();
            return null;
        }).when(activity).showCommonPinWarning(any(DialogButtonCallback.class));
        // Act
        subject.onPadClicked("4");
        // Assert
        verify(activity).showCommonPinWarning(any(DialogButtonCallback.class));
        assertEquals("", subject.mUserEnteredPin);
        assertEquals("1234", subject.mUserEnteredConfirmationPin);
    }

    @Test
    public void padClickedShowPinReuseWarning() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "258";
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("");
        when(accessState.getPIN()).thenReturn("2580");
        // Act
        subject.onPadClicked("0");
        // Assert
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).clearPinBoxes();
    }

    @Test
    public void padClickedVerifyPinValidateCalled() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "133";
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, ""))
                .thenReturn("1234567890");
        when(authDataManager.validatePin(anyString())).thenReturn(just(""));
        // Act
        subject.onPadClicked("7");
        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE);
        verify(activity, times(2)).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).validatePin(anyString());
    }

    @Test
    public void padClickedVerifyPinForResultReturnsValidPassword() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "133";
        subject.mValidatingPinForResult = true;
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, ""))
                .thenReturn("1234567890");
        when(authDataManager.validatePin(anyString())).thenReturn(just(""));
        // Act
        subject.onPadClicked("7");
        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE);
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(activity).dismissProgressDialog();
        verify(authDataManager).validatePin(anyString());
        verify(activity).finishWithResultOk("1337");
    }

    @Test
    public void padClickedVerifyPinValidateCalledReturnsServerError() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "133";
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, ""))
                .thenReturn("1234567890");
        when(authDataManager.validatePin(anyString()))
                .thenReturn(Observable.error(new ServerConnectionException()));
        // Act
        subject.onPadClicked("7");
        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE);
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).validatePin(anyString());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).restartPageAndClearTop();
    }

    @Test
    public void padClickedVerifyPinValidateCalledReturnsInvalidCipherText() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "133";
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, ""))
                .thenReturn("1234567890");
        when(authDataManager.validatePin(anyString())).thenReturn(just(""));
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.error(new InvalidCipherTextException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        // Act
        subject.onPadClicked("7");
        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE);
        verify(activity, times(2)).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).validatePin(anyString());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(prefsUtil).setValue(anyString(), anyInt());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(accessState).setPIN(null);
        verify(appUtil).clearCredentialsAndRestart();
    }

    @Test
    public void padClickedVerifyPinValidateCalledReturnsGenericException() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "133";
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, ""))
                .thenReturn("1234567890");
        when(authDataManager.validatePin(anyString())).thenReturn(just(""));
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.error(new Exception()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        // Act
        subject.onPadClicked("7");
        // Assert
        verify(activity).setTitleVisibility(View.INVISIBLE);
        verify(activity, times(2)).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).validatePin(anyString());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(prefsUtil).setValue(anyString(), anyInt());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(appUtil).clearCredentialsAndRestart();
    }

    @Test
    public void padClickedCreatePinCreateSuccessful() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "133";
        subject.mUserEnteredConfirmationPin = "1337";
        when(payloadManager.getTempPassword()).thenReturn("temp password");
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.complete());
        when(authDataManager.validatePin(anyString())).thenReturn(just("password"));
        // Act
        subject.onPadClicked("7");
        // Assert
        verify(activity, times(2)).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).createPin(anyString(), anyString());
        verify(fingerprintHelper).clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
        verify(fingerprintHelper).setFingerprintUnlockEnabled(false);
    }

    @Test
    public void padClickedCreatePinCreateFailed() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "133";
        subject.mUserEnteredConfirmationPin = "1337";
        when(payloadManager.getTempPassword()).thenReturn("temp password");
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(authDataManager.createPin(anyString(), anyString()))
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.onPadClicked("7");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).createPin(anyString(), anyString());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(prefsUtil).clear();
        verify(appUtil).restartApp();
    }

    @Test
    public void padClickedCreatePinWritesNewConfirmationValue() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "133";
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.complete());
        // Act
        subject.onPadClicked("7");
        // Assert
        assertEquals("1337", subject.mUserEnteredConfirmationPin);
        assertEquals("", subject.mUserEnteredPin);
    }

    @Test
    public void padClickedCreatePinMismatched() throws Exception {
        // Arrange
        subject.mUserEnteredPin = "133";
        subject.mUserEnteredConfirmationPin = "1234";
        when(prefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(authDataManager.createPin(anyString(), anyString())).thenReturn(Completable.complete());
        // Act
        subject.onPadClicked("7");
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).dismissProgressDialog();
    }

    @Test
    public void clearPinBoxes() throws Exception {
        // Arrange

        // Act
        subject.clearPinBoxes();
        // Assert
        verify(activity).clearPinBoxes();
        assertEquals("", subject.mUserEnteredPin);
    }

    @Test
    public void validatePasswordSuccessful() throws Exception {
        // Arrange
        String password = "1234567890";
        when(authDataManager.updatePayload(anyString(), anyString(), eq(password)))

                .thenReturn(Completable.complete());
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        // Act
        subject.validatePassword(password);
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), eq(password));
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(prefsUtil, times(2)).removeValue(anyString());
        verify(activity).restartPageAndClearTop();
    }

    @Test
    public void validatePasswordThrowsGenericException() throws Exception {
        // Arrange
        String password = "1234567890";
        when(authDataManager.updatePayload(anyString(), anyString(), eq(password)))
                .thenReturn(Completable.error(new Throwable()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        // Act
        subject.validatePassword(password);
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), eq(password));
        verify(activity, times(2)).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).showValidationDialog();
    }

    @Test
    public void validatePasswordThrowsServerConnectionException() throws Exception {
        // Arrange
        String password = "1234567890";
        when(authDataManager.updatePayload(anyString(), anyString(), eq(password)))
                .thenReturn(Completable.error(new ServerConnectionException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        // Act
        subject.validatePassword(password);
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), eq(password));
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
    }

    @Test
    public void validatePasswordThrowsHDWalletExceptionException() throws Exception {
        // Arrange
        String password = "1234567890";
        when(authDataManager.updatePayload(anyString(), anyString(), eq(password)))
                .thenReturn(Completable.error(new HDWalletException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        // Act
        subject.validatePassword(password);
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), eq(password));
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(appUtil).restartApp();
    }

    @Test
    public void validatePasswordThrowsAccountLockedException() throws Exception {
        // Arrange
        String password = "1234567890";
        when(authDataManager.updatePayload(anyString(), anyString(), eq(password)))
                .thenReturn(Completable.error(new AccountLockedException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        // Act
        subject.validatePassword(password);
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), eq(password));
        verify(activity).dismissProgressDialog();
        verify(activity).showAccountLockedDialog();
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updatePayloadInvalidCredentialsException() throws Exception {
        // Arrange
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.error(new InvalidCredentialsException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.updatePayload("");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(activity).goToPasswordRequiredActivity();
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updatePayloadServerConnectionException() throws Exception {
        // Arrange
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.error(new ServerConnectionException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.updatePayload("");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updatePayloadDecryptionException() throws Exception {
        // Arrange
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.error(new DecryptionException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.updatePayload("");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(activity).goToPasswordRequiredActivity();
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updatePayloadPayloadExceptionException() throws Exception {
        // Arrange
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.error(new PayloadException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.updatePayload("");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(appUtil).restartApp();
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updatePayloadHDWalletException() throws Exception {
        // Arrange
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.error(new HDWalletException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.updatePayload("");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(appUtil).restartApp();
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updatePayloadVersionNotSupported() throws Exception {
        // Arrange
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.error(new UnsupportedVersionException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.updatePayload("");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(activity).showWalletVersionNotSupportedDialog(isNull());
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updatePayloadAccountLocked() throws Exception {
        // Arrange
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.error(new AccountLockedException()));
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.updatePayload("");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(activity).showAccountLockedDialog();
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updatePayloadSuccessfulSetLabels() throws Exception {
        // Arrange
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.complete());
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        HDWallet mockHdWallet = mock(HDWallet.class);
        when(mockPayload.getHdWallets()).thenReturn(Collections.singletonList(mockHdWallet));
        Account mockAccount = mock(Account.class);
        when(mockAccount.getLabel()).thenReturn(null);
        ArrayList<Account> accountsList = new ArrayList<>();
        accountsList.add(mockAccount);
        when(mockHdWallet.getAccounts()).thenReturn(accountsList);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(appUtil.isNewlyCreated()).thenReturn(true);
        // Act
        subject.updatePayload("");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(appUtil).setSharedKey(anyString());
        verify(payloadManager, times(5)).getPayload();
        verify(stringUtils).getString(anyInt());
        verify(activity).dismissProgressDialog();
        assertEquals(true, subject.mCanShowFingerprintDialog);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updatePayloadSuccessfulUpgradeWallet() throws Exception {
        // Arrange
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.complete());
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(false);
        when(appUtil.isNewlyCreated()).thenReturn(false);
        // Act
        subject.updatePayload("");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(appUtil).setSharedKey(anyString());
        verify(activity).goToUpgradeWalletActivity();
        verify(activity).dismissProgressDialog();
        assertEquals(true, subject.mCanShowFingerprintDialog);
    }

    @SuppressLint("VisibleForTests")
    @Test
    public void updatePayloadSuccessfulVerifyPin() throws Exception {
        // Arrange
        when(authDataManager.updatePayload(anyString(), anyString(), anyString()))
                .thenReturn(Completable.complete());
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("prefs string");
        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(appUtil.isNewlyCreated()).thenReturn(false);
        // Act
        subject.updatePayload("");
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull());
        verify(authDataManager).updatePayload(anyString(), anyString(), anyString());
        verify(appUtil).setSharedKey(anyString());
        verify(appUtil).restartAppWithVerifiedPin();
        verify(activity).dismissProgressDialog();
        assertEquals(true, subject.mCanShowFingerprintDialog);
    }

    @Test
    public void incrementFailureCount() throws Exception {
        // Arrange

        // Act
        subject.incrementFailureCountAndRestart();
        // Assert
        verify(prefsUtil).getValue(anyString(), anyInt());
        verify(prefsUtil).setValue(anyString(), anyInt());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).restartPageAndClearTop();
    }

    @Test
    public void resetApp() throws Exception {
        // Arrange

        // Act
        subject.resetApp();
        // Assert
        verify(appUtil).clearCredentialsAndRestart();
    }

    @Test
    public void allowExit() throws Exception {
        // Arrange

        // Act
        boolean allowExit = subject.allowExit();
        // Assert
        assertEquals(subject.bAllowExit, allowExit);
    }

    @Test
    public void isCreatingNewPin() throws Exception {
        // Arrange
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("");
        // Act
        boolean creatingNewPin = subject.isCreatingNewPin();
        // Assert
        assertEquals(true, creatingNewPin);
    }

    @Test
    public void isNotCreatingNewPin() throws Exception {
        // Arrange
        when(prefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        // Act
        boolean creatingNewPin = subject.isCreatingNewPin();
        // Assert
        assertEquals(false, creatingNewPin);
    }

    @Test
    public void getAppUtil() throws Exception {
        // Arrange

        // Act
        AppUtil util = subject.getAppUtil();
        // Assert
        assertEquals(util, appUtil);
    }

    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected AppUtil provideAppUtil() {
            return appUtil;
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return prefsUtil;
        }

        @Override
        protected StringUtils provideStringUtils() {
            return stringUtils;
        }

        @Override
        protected AccessState provideAccessState() {
            return accessState;
        }
    }

    private class MockApiModule extends ApiModule {

        @Override
        protected PayloadManager providePayloadManager() {
            return payloadManager;
        }

        @Override
        protected SSLVerifyUtil provideSSlVerifyUtil(@Named("explorer") Retrofit retrofit,
                                                     RxBus rxBus) {
            return sslVerifyUtil;
        }
    }

    private class MockDataManagerModule extends DataManagerModule {

        @Override
        protected AuthDataManager provideAuthDataManager(PayloadDataManager payloadDataManager,
                                                         PrefsUtil prefsUtil,
                                                         AppUtil appUtil,
                                                         AccessState accessState,
                                                         StringUtils stringUtils,
                                                         AESUtilWrapper aesUtilWrapper,
                                                         RxBus rxBus) {
            return authDataManager;
        }

        @Override
        protected FingerprintHelper provideFingerprintHelper(Context applicationContext, PrefsUtil prefsUtil) {
            return fingerprintHelper;
        }
    }

}
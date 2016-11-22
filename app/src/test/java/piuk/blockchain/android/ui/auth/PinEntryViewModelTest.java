package piuk.blockchain.android.ui.auth;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.TextView;

import info.blockchain.wallet.exceptions.AccountLockedException;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.exceptions.UnsupportedVersionException;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
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

import io.reactivex.Completable;
import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
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
import piuk.blockchain.android.util.StringUtils;

import static io.reactivex.Observable.just;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
import static piuk.blockchain.android.ui.auth.PinEntryActivity.KEY_VALIDATING_PIN_FOR_RESULT;

@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class PinEntryViewModelTest {

    private PinEntryViewModel mSubject;

    @Mock private PinEntryActivity mActivity;
    @Mock private AuthDataManager mAuthDataManager;
    @Mock private AppUtil mAppUtil;
    @Mock private PrefsUtil mPrefsUtil;
    @Mock private PayloadManager mPayloadManager;
    @Mock private StringUtils mStringUtils;
    @Mock private FingerprintHelper mFingerprintHelper;
    @Mock private AccessState mAccessState;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new MockDataManagerModule());

        TextView mockTextView = mock(TextView.class);
        when(mActivity.getPinBoxArray()).thenReturn(new TextView[]{mockTextView, mockTextView, mockTextView, mockTextView});

        mSubject = new PinEntryViewModel(mActivity);
    }

    @Test
    public void onViewReadyEmailAndPasswordInIntentCreateWalletSuccessful() throws Exception {
        // Arrange
        String email = "example@email.com";
        String password = "1234567890";
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_EMAIL, email);
        intent.putExtra(KEY_INTENT_PASSWORD, password);
        when(mActivity.getPageIntent()).thenReturn(intent);
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn(new CharSequenceX(""));
        when(mAuthDataManager.createHdWallet(anyString(), anyString())).thenReturn(just(new Payload()));
        // Act
        mSubject.onViewReady();
        // Assert
        assertEquals(false, mSubject.allowExit());
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mActivity).dismissProgressDialog();
        verify(mPrefsUtil).setValue(PrefsUtil.KEY_EMAIL, email);
        verify(mPayloadManager).setEmail(email);
        verify(mPayloadManager).setTempPassword(new CharSequenceX(password));
    }

    @Test
    public void onViewReadyValidatingPinForResult() throws Exception {
        // Arrange
        Intent intent = new Intent();
        intent.putExtra(KEY_VALIDATING_PIN_FOR_RESULT, true);
        when(mActivity.getPageIntent()).thenReturn(intent);
        // Act
        mSubject.onViewReady();
        // Assert
        assertEquals(true, mSubject.isForValidatingPinForResult());
    }

    @Test
    public void onViewReadyEmailAndPasswordInIntentCreateWalletThrowsError() throws Exception {
        // Arrange
        String email = "example@email.com";
        String password = "1234567890";
        Intent intent = new Intent();
        intent.putExtra(KEY_INTENT_EMAIL, email);
        intent.putExtra(KEY_INTENT_PASSWORD, password);
        when(mActivity.getPageIntent()).thenReturn(intent);
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn(new CharSequenceX(""));
        when(mAuthDataManager.createHdWallet(anyString(), anyString())).thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.onViewReady();
        // Assert
        assertEquals(false, mSubject.allowExit());
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mActivity, times(2)).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
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
        when(mActivity.getPageIntent()).thenReturn(intent);
        when(mAuthDataManager.createHdWallet(anyString(), anyString())).thenReturn(just(new Payload()));
        // Act
        mSubject.onViewReady();
        // Assert
        assertEquals(false, mSubject.allowExit());
        verify(mPrefsUtil).setValue(PrefsUtil.KEY_EMAIL, email);
        verify(mPayloadManager).setEmail(email);
        verify(mPayloadManager).setTempPassword(new CharSequenceX(password));
        verifyNoMoreInteractions(mPayloadManager);
    }

    @Test
    public void onViewReadyMaxAttemptsExceeded() throws Exception {
        // Arrange
        when(mActivity.getPageIntent()).thenReturn(new Intent());
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_FAILS, 0)).thenReturn(4);
        when(mPayloadManager.getPayload()).thenReturn(mock(Payload.class));
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn(new CharSequenceX(""));
        // Act
        mSubject.onViewReady();
        // Assert
        assertEquals(true, mSubject.allowExit());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).showMaxAttemptsDialog();
    }

    @Test
    public void checkFingerprintStatusShouldShowDialog() throws Exception {
        // Arrange
        mSubject.mValidatingPinForResult = false;
        mSubject.mRecoveringFunds = false;
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234");
        when(mFingerprintHelper.getIfFingerprintUnlockEnabled()).thenReturn(true);
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn(null);
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn(new CharSequenceX(""));
        // Act
        mSubject.checkFingerprintStatus();
        // Assert
        verify(mActivity).showFingerprintDialog(any(CharSequenceX.class));
    }

    @Test
    public void checkFingerprintStatusDontShow() throws Exception {
        // Arrange
        mSubject.mValidatingPinForResult = true;
        // Act
        mSubject.checkFingerprintStatus();
        // Assert
        verify(mActivity).showKeyboard();
    }

    @Test
    public void canShowFingerprintDialog() throws Exception {
        // Arrange
        mSubject.mCanShowFingerprintDialog = true;
        // Act
        boolean value = mSubject.canShowFingerprintDialog();
        // Assert
        assertEquals(true, value);
    }

    @Test
    public void loginWithDecryptedPin() throws Exception {
        // Arrange
        CharSequenceX pincode = new CharSequenceX("1234");
        when(mAuthDataManager.validatePin(pincode.toString())).thenReturn(just(new CharSequenceX("password")));
        // Act
        mSubject.loginWithDecryptedPin(pincode);
        // Assert
        verify(mActivity).getPinBoxArray();
        assertEquals(false, mSubject.canShowFingerprintDialog());
    }

    @Test
    public void onDeleteClicked() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "1234";
        // Act
        mSubject.onDeleteClicked();
        // Assert
        assertEquals("123", mSubject.mUserEnteredPin);
        verify(mActivity).getPinBoxArray();
    }

    @Test
    public void padClickedPinAlreadyFourDigits() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "0000";
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("0");
        mSubject.padClicked(mockView);
        // Assert
        verifyZeroInteractions(mActivity);
    }

    @Test
    public void padClickedAllZeros() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "000";
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("0");
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mFingerprintHelper.getEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE)).thenReturn(new CharSequenceX(""));
        mSubject.padClicked(mockView);
        // Assert
        verify(mActivity).clearPinBoxes();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        assertEquals("", mSubject.mUserEnteredPin);
        assertEquals(null, mSubject.mUserEnteredConfirmationPin);
    }

    @Test
    public void padClickedShowCommonPinWarning() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "123";
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("4");
        mSubject.padClicked(mockView);
        // Assert
        verify(mActivity).showCommonPinWarning(any(DialogButtonCallback.class));
    }

    @Test
    public void padClickedShowCommonPinWarningAndClickRetry() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "123";
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onPositiveClicked();
            return null;
        }).when(mActivity).showCommonPinWarning(any(DialogButtonCallback.class));
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("4");
        mSubject.padClicked(mockView);
        // Assert
        verify(mActivity).showCommonPinWarning(any(DialogButtonCallback.class));
        verify(mActivity).clearPinBoxes();
        assertEquals("", mSubject.mUserEnteredPin);
        assertEquals(null, mSubject.mUserEnteredConfirmationPin);
    }

    @Test
    public void padClickedShowCommonPinWarningAndClickContinue() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "123";
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onNegativeClicked();
            return null;
        }).when(mActivity).showCommonPinWarning(any(DialogButtonCallback.class));
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("4");
        mSubject.padClicked(mockView);
        // Assert
        verify(mActivity).showCommonPinWarning(any(DialogButtonCallback.class));
        assertEquals("", mSubject.mUserEnteredPin);
        assertEquals("1234", mSubject.mUserEnteredConfirmationPin);
    }

    @Test
    public void padClickedShowPinReuseWarning() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "258";
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        when(mAccessState.getPIN()).thenReturn("2580");
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("0");
        mSubject.padClicked(mockView);
        // Assert
        verify(mActivity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(mActivity).clearPinBoxes();
    }

    @Test
    public void padClickedVerifyPinValidateCalled() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234567890");
        when(mAuthDataManager.validatePin(anyString())).thenReturn(just(new CharSequenceX("")));
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("7");
        mSubject.padClicked(mockView);
        // Assert
        verify(mActivity).setTitleVisibility(View.INVISIBLE);
        verify(mActivity, times(2)).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).validatePin(anyString());
    }

    @Test
    public void padClickedVerifyPinForResultReturnsValidPassword() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        mSubject.mValidatingPinForResult = true;
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234567890");
        when(mAuthDataManager.validatePin(anyString())).thenReturn(just(new CharSequenceX("")));
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("7");
        mSubject.padClicked(mockView);
        // Assert
        verify(mActivity).setTitleVisibility(View.INVISIBLE);
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mActivity).dismissProgressDialog();
        verify(mAuthDataManager).validatePin(anyString());
        verify(mActivity).finishWithResultOk("1337");
    }

    @Test
    public void padClickedVerifyPinValidateCalledReturnsErrorIncrementsFailureCount() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("1234567890");
        when(mAuthDataManager.validatePin(anyString())).thenReturn(Observable.error(new InvalidCredentialsException()));
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("7");
        mSubject.padClicked(mockView);
        // Assert
        verify(mActivity).setTitleVisibility(View.INVISIBLE);
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).validatePin(anyString());
        verify(mPrefsUtil).setValue(anyString(), anyInt());
        verify(mPrefsUtil).getValue(anyString(), anyInt());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).restartPageAndClearTop();
    }

    @Test
    public void padClickedCreatePinCreateSuccessful() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        mSubject.mUserEnteredConfirmationPin = "1337";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mAuthDataManager.createPin(any(CharSequenceX.class), anyString())).thenReturn(just(true));
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("7");
        mSubject.padClicked(mockView);
        // Assert
        verify(mActivity, times(2)).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).createPin(any(CharSequenceX.class), anyString());
        verify(mFingerprintHelper).clearEncryptedData(PrefsUtil.KEY_ENCRYPTED_PIN_CODE);
        verify(mFingerprintHelper).setFingerprintUnlockEnabled(false);
    }

    @Test
    public void padClickedCreatePinCreateFailed() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        mSubject.mUserEnteredConfirmationPin = "1337";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mAuthDataManager.createPin(any(CharSequenceX.class), anyString())).thenReturn(just(false));
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("7");
        mSubject.padClicked(mockView);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).createPin(any(CharSequenceX.class), anyString());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mPrefsUtil).clear();
        verify(mAppUtil).restartApp();
    }

    @Test
    public void padClickedCreatePinWritesNewConfirmationValue() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mAuthDataManager.createPin(any(CharSequenceX.class), anyString())).thenReturn(just(true));
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("7");
        mSubject.padClicked(mockView);
        // Assert
        assertEquals("1337", mSubject.mUserEnteredConfirmationPin);
        assertEquals("", mSubject.mUserEnteredPin);
    }

    @Test
    public void padClickedCreatePinMismatched() throws Exception {
        // Arrange
        mSubject.mUserEnteredPin = "133";
        mSubject.mUserEnteredConfirmationPin = "1234";
        when(mPrefsUtil.getValue(PrefsUtil.KEY_PIN_IDENTIFIER, "")).thenReturn("");
        when(mAuthDataManager.createPin(any(CharSequenceX.class), anyString())).thenReturn(just(true));
        // Act
        View mockView = mock(View.class);
        when(mockView.getTag()).thenReturn("7");
        mSubject.padClicked(mockView);
        // Assert
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).dismissProgressDialog();
    }

    @Test
    public void clearPinBoxes() throws Exception {
        // Arrange

        // Act
        mSubject.clearPinBoxes();
        // Assert
        verify(mActivity).clearPinBoxes();
        assertEquals("", mSubject.mUserEnteredPin);
    }

    @Test
    public void validatePasswordSuccessful() throws Exception {
        // Arrange
        CharSequenceX password = new CharSequenceX("1234567890");
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.complete());
        // Act
        mSubject.validatePassword(password);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        verify(mPayloadManager).setTempPassword(new CharSequenceX(""));
        verify(mActivity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mPrefsUtil, times(2)).removeValue(anyString());
        verify(mActivity).restartPageAndClearTop();
    }

    @Test
    public void validatePasswordThrowsGenericException() throws Exception {
        // Arrange
        CharSequenceX password = new CharSequenceX("1234567890");
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.error(new Throwable()));
        // Act
        mSubject.validatePassword(password);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        verify(mPayloadManager).setTempPassword(new CharSequenceX(""));
        verify(mActivity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).showValidationDialog();
    }

    @Test
    public void validatePasswordThrowsServerConnectionException() throws Exception {
        // Arrange
        CharSequenceX password = new CharSequenceX("1234567890");
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.error(new ServerConnectionException()));
        // Act
        mSubject.validatePassword(password);
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        verify(mPayloadManager).setTempPassword(new CharSequenceX(""));
        verify(mActivity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
    }

    @Test
    public void updatePayloadInvalidCredentialsException() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.error(new InvalidCredentialsException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload(new CharSequenceX(""));
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        verify(mActivity).goToPasswordRequiredActivity();
    }

    @Test
    public void updatePayloadServerConnectionException() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.error(new ServerConnectionException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload(new CharSequenceX(""));
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
    }

    @Test
    public void updatePayloadDecryptionException() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.error(new DecryptionException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload(new CharSequenceX(""));
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        verify(mActivity).goToPasswordRequiredActivity();
    }

    @Test
    public void updatePayloadPayloadExceptionException() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.error(new PayloadException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload(new CharSequenceX(""));
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mAppUtil).restartApp();
    }

    @Test
    public void updatePayloadHDWalletException() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.error(new HDWalletException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload(new CharSequenceX(""));
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mAppUtil).restartApp();
    }

    @Test
    public void updatePayloadVersionNotSupported() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.error(new UnsupportedVersionException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload(new CharSequenceX(""));
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        verify(mActivity).showWalletVersionNotSupportedDialog(anyString());
    }

    @Test
    public void updatePayloadAccountLocked() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.error(new AccountLockedException()));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.updatePayload(new CharSequenceX(""));
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        verify(mActivity).showAccountLockedDialog();
    }

    @Test
    public void updatePayloadSuccessfulSetLabels() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.complete());
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        HDWallet mockHdWallet = mock(HDWallet.class);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        Account mockAccount = mock(Account.class);
        when(mockAccount.getLabel()).thenReturn(null);
        ArrayList<Account> accountsList = new ArrayList<>();
        accountsList.add(mockAccount);
        when(mockHdWallet.getAccounts()).thenReturn(accountsList);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mAppUtil.isNewlyCreated()).thenReturn(true);
        // Act
        mSubject.updatePayload(new CharSequenceX(""));
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        verify(mAppUtil).setSharedKey(anyString());
        verify(mPayloadManager, times(5)).getPayload();
        verify(mStringUtils).getString(anyInt());
        verify(mActivity).dismissProgressDialog();
        assertEquals(true, mSubject.mCanShowFingerprintDialog);
    }

    @Test
    public void updatePayloadSuccessfulUpgradeWallet() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.complete());
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(false);
        when(mAppUtil.isNewlyCreated()).thenReturn(false);
        // Act
        mSubject.updatePayload(new CharSequenceX(""));
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        verify(mAppUtil).setSharedKey(anyString());
        verify(mActivity).goToUpgradeWalletActivity();
        verify(mActivity).dismissProgressDialog();
        assertEquals(true, mSubject.mCanShowFingerprintDialog);
    }

    @Test
    public void updatePayloadSuccessfulVerifyPin() throws Exception {
        // Arrange
        when(mAuthDataManager.updatePayload(anyString(), anyString(), any(CharSequenceX.class))).thenReturn(Completable.complete());
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getSharedKey()).thenReturn("1234567890");
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mAppUtil.isNewlyCreated()).thenReturn(false);
        // Act
        mSubject.updatePayload(new CharSequenceX(""));
        // Assert
        verify(mActivity).showProgressDialog(anyInt(), anyString());
        verify(mAuthDataManager).updatePayload(anyString(), anyString(), any(CharSequenceX.class));
        verify(mAppUtil).setSharedKey(anyString());
        verify(mAppUtil).restartAppWithVerifiedPin();
        verify(mActivity).dismissProgressDialog();
        assertEquals(true, mSubject.mCanShowFingerprintDialog);
    }

    @Test
    public void incrementFailureCount() throws Exception {
        // Arrange

        // Act
        mSubject.incrementFailureCountAndRestart();
        // Assert
        verify(mPrefsUtil).getValue(anyString(), anyInt());
        verify(mPrefsUtil).setValue(anyString(), anyInt());
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).restartPageAndClearTop();
    }

    @Test
    public void resetApp() throws Exception {
        // Arrange

        // Act
        mSubject.resetApp();
        // Assert
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    @Test
    public void allowExit() throws Exception {
        // Arrange

        // Act
        boolean allowExit = mSubject.allowExit();
        // Assert
        assertEquals(mSubject.bAllowExit, allowExit);
    }

    @Test
    public void isCreatingNewPin() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("");
        // Act
        boolean creatingNewPin = mSubject.isCreatingNewPin();
        // Assert
        assertEquals(true, creatingNewPin);
    }

    @Test
    public void isNotCreatingNewPin() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");
        // Act
        boolean creatingNewPin = mSubject.isCreatingNewPin();
        // Assert
        assertEquals(false, creatingNewPin);
    }

    @Test
    public void getAppUtil() throws Exception {
        // Arrange

        // Act
        AppUtil util = mSubject.getAppUtil();
        // Assert
        assertEquals(util, mAppUtil);
    }

    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected AppUtil provideAppUtil() {
            return mAppUtil;
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return mPrefsUtil;
        }

        @Override
        protected StringUtils provideStringUtils() {
            return mStringUtils;
        }

        @Override
        protected AccessState provideAccessState() {
            return mAccessState;
        }
    }

    private class MockApiModule extends ApiModule {

        @Override
        protected PayloadManager providePayloadManager() {
            return mPayloadManager;
        }
    }

    private class MockDataManagerModule extends DataManagerModule {

        @Override
        protected AuthDataManager provideAuthDataManager(PayloadManager payloadManager,
                                                         PrefsUtil prefsUtil,
                                                         AppUtil appUtil,
                                                         AESUtilWrapper aesUtilWrapper,
                                                         AccessState accessState,
                                                         StringUtils stringUtils) {
            return mAuthDataManager;
        }

        @Override
        protected FingerprintHelper provideFingerprintHelper(Context applicationContext, PrefsUtil prefsUtil) {
            return mFingerprintHelper;
        }
    }

}
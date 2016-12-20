package piuk.blockchain.android.ui.auth;

import android.app.Application;

import info.blockchain.api.WalletPayload;
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

import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by adambennett on 10/08/2016.
 */
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class PasswordRequiredViewModelTest extends RxTest {

    private PasswordRequiredViewModel mSubject;

    @Mock private PasswordRequiredActivity mActivity;
    @Mock protected AppUtil mAppUtil;
    @Mock protected PrefsUtil mPrefsUtil;
    @Mock protected AuthDataManager mAuthDataManager;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new ApiModule(),
                new MockDataManagerModule());

        mSubject = new PasswordRequiredViewModel(mActivity);
    }

    /**
     * Password is missing, should trigger {@link PasswordRequiredActivity#restartPage()}
     */
    @Test
    public void onContinueClickedNoPassword() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("");
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).restartPage();
    }

    /**
     * Password is correct, should trigger {@link PasswordRequiredActivity#goToPinPage()}
     */
    @Test
    public void onContinueClickedCorrectPassword() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.startPollingAuthStatus(anyString())).thenReturn(Observable.just("1234567890"));
        doAnswer(invocation -> {
            ((AuthDataManager.DecryptPayloadListener) invocation.getArguments()[3]).onSuccess();
            return null;
        }).when(mAuthDataManager).attemptDecryptPayload(
                any(CharSequenceX.class), anyString(), anyString(), any(AuthDataManager.DecryptPayloadListener.class));

        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).goToPinPage();
    }

    /**
     * PayloadManager returns a pairing failure, should trigger {@link PasswordRequiredActivity#showToast(int, String)} ()}
     */
    @Test
    public void onContinueClickedPairingFailure() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.startPollingAuthStatus(anyString())).thenReturn(Observable.just("1234567890"));
        doAnswer(invocation -> {
            ((AuthDataManager.DecryptPayloadListener) invocation.getArguments()[3]).onPairFail();
            return null;
        }).when(mAuthDataManager).attemptDecryptPayload(
                any(CharSequenceX.class), anyString(), anyString(), any(AuthDataManager.DecryptPayloadListener.class));

        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
    }

    /**
     * PayloadManager returns wallet creation failure, should trigger {@link PasswordRequiredActivity#showToast(int, String)} ()}
     */
    @Test
    public void onContinueClickedCreateFailure() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.startPollingAuthStatus(anyString())).thenReturn(Observable.just("1234567890"));
        doAnswer(invocation -> {
            ((AuthDataManager.DecryptPayloadListener) invocation.getArguments()[3]).onPairFail();
            return null;
        }).when(mAuthDataManager).attemptDecryptPayload(
                any(CharSequenceX.class), anyString(), anyString(), any(AuthDataManager.DecryptPayloadListener.class));

        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
    }

    /**
     * PayloadManager returns auth failure, should trigger {@link PasswordRequiredActivity#showToast(int, String)} ()}
     */
    @Test
    public void onContinueClickedAuthFailure() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.startPollingAuthStatus(anyString())).thenReturn(Observable.just("1234567890"));
        doAnswer(invocation -> {
            ((AuthDataManager.DecryptPayloadListener) invocation.getArguments()[3]).onAuthFail();
            return null;
        }).when(mAuthDataManager).attemptDecryptPayload(
                any(CharSequenceX.class), anyString(), anyString(), any(AuthDataManager.DecryptPayloadListener.class));

        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
    }

    /**
     * PayloadManager returns a fatal error, should trigger {@link AppUtil#clearCredentialsAndRestart()}
     */
    @Test
    public void onContinueClickedFatalError() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.startPollingAuthStatus(anyString())).thenReturn(Observable.just("1234567890"));
        doAnswer(invocation -> {
            ((AuthDataManager.DecryptPayloadListener) invocation.getArguments()[3]).onFatalError();
            return null;
        }).when(mAuthDataManager).attemptDecryptPayload(
                any(CharSequenceX.class), anyString(), anyString(), any(AuthDataManager.DecryptPayloadListener.class));

        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    /**
     * {@link AuthDataManager#getSessionId(String)} throws exception. Should restart
     * the app view {@link AppUtil#clearCredentialsAndRestart()}
     */
    @Test
    public void onContinueClickedSessionIdFailure() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.error(new Throwable()));

        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    /**
     * {@link AuthDataManager#getEncryptedPayload(String, String)} throws exception. Should restart
     * the app via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @Test
    public void onContinueClickedEncryptedPayloadFailure() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.error(new Throwable()));

        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mActivity).dismissProgressDialog();
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String)}} returns Access Required. Should restart
     * the app via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @Test
    public void onContinueClickedWaitingForAuthRequired() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(WalletPayload.KEY_AUTH_REQUIRED));
        when(mAuthDataManager.startPollingAuthStatus(anyString())).thenReturn(Observable.just(WalletPayload.KEY_AUTH_REQUIRED));
        when(mAuthDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String)}} returns payload. Should attempt to
     * decrypt the payload.
     */
    @Test
    public void onContinueClickedWaitingForAuthSuccess() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(WalletPayload.KEY_AUTH_REQUIRED));
        when(mAuthDataManager.startPollingAuthStatus(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        // Act
        mSubject.onContinueClicked();
        // Assert
        verify(mAuthDataManager).attemptDecryptPayload(any(), anyString(), anyString(), any());
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String)}} returns an error. Should restart the app
     * via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @Test
    public void onContinueClickedWaitingForAuthFailure() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(WalletPayload.KEY_AUTH_REQUIRED));
        when(mAuthDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        when(mAuthDataManager.startPollingAuthStatus(anyString())).thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String)}} counts down to zero. Should restart the app
     * via {@link AppUtil#clearCredentialsAndRestart()}
     */
    @Test
    public void onContinueClickedWaitingForAuthCountdownComplete() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(WalletPayload.KEY_AUTH_REQUIRED));
        when(mAuthDataManager.createCheckEmailTimer()).thenReturn(Observable.just(0));
        when(mAuthDataManager.startPollingAuthStatus(anyString())).thenReturn(Observable.just("1234567890"));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
        verify(mActivity).resetPasswordField();
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    /**
     * {@link AuthDataManager#createCheckEmailTimer()}} returns Throwable. Should show error toast.
     */
    @Test
    public void onContinueClickedWaitingForAuthCountdownError() throws Exception {
        // Arrange
        when(mActivity.getPassword()).thenReturn("1234567890");
        when(mPrefsUtil.getValue(anyString(), anyString())).thenReturn("1234567890");

        when(mAuthDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(mAuthDataManager.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(WalletPayload.KEY_AUTH_REQUIRED));
        when(mAuthDataManager.createCheckEmailTimer()).thenReturn(Observable.error(new Throwable()));
        when(mAuthDataManager.startPollingAuthStatus(anyString())).thenReturn(Observable.just("1234567890"));
        // Act
        mSubject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), anyString());
    }

    @Test
    public void onProgressCancelled() throws Exception {
        // Arrange

        // Act
        mSubject.onProgressCancelled();
        // Assert
        assertFalse(mSubject.mWaitingForAuth);
        assertEquals(0, mSubject.compositeDisposable.size());
    }

    @Test
    public void onForgetWalletClickedShowWarningAndContinue() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onPositiveClicked();
            return null;
        }).when(mActivity).showForgetWalletWarning(any(DialogButtonCallback.class));
        // Act
        mSubject.onForgetWalletClicked();
        // Assert
        verify(mActivity).showForgetWalletWarning(any(DialogButtonCallback.class));
        verify(mAppUtil).clearCredentialsAndRestart();
    }

    @Test
    public void onForgetWalletClickedShowWarningAndDismiss() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onNegativeClicked();
            return null;
        }).when(mActivity).showForgetWalletWarning(any(DialogButtonCallback.class));
        // Act
        mSubject.onForgetWalletClicked();
        // Assert
        verify(mActivity).showForgetWalletWarning(any(DialogButtonCallback.class));
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void getAppUtil() throws Exception {
        // Arrange

        // Act
        AppUtil util = mSubject.getAppUtil();
        // Assert
        assertEquals(util, mAppUtil);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange

        // Act
        mSubject.onViewReady();
        // Assert
        assertTrue(true);
    }

    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return mPrefsUtil;
        }

        @Override
        protected AppUtil provideAppUtil() {
            return mAppUtil;
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
    }
}
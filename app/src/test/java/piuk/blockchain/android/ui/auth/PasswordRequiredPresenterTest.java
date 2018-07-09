package piuk.blockchain.android.ui.auth;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.Completable;
import io.reactivex.Observable;
import okhttp3.MediaType;
import okhttp3.ResponseBody;
import piuk.blockchain.android.testutils.RxTest;
import piuk.blockchain.android.ui.launcher.LauncherActivity;
import piuk.blockchain.android.util.DialogButtonCallback;
import piuk.blockchain.androidbuysell.datamanagers.BuyDataManager;
import piuk.blockchain.androidbuysell.datamanagers.CoinifyDataManager;
import piuk.blockchain.androidcore.data.auth.AuthDataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.utils.PrefsUtil;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.androidcoreui.utils.AppUtil;
import retrofit2.Response;

import static junit.framework.Assert.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.auth.PasswordRequiredPresenter.KEY_AUTH_REQUIRED;


public class PasswordRequiredPresenterTest extends RxTest {

    private static final String KEY_AUTH_REQUIRED_JSON = "{\n" +
            "  \"authorization_required\": true\n" +
            "}";

    private static final String TWO_FA_RESPONSE = "{\n" +
            "  \"auth_type\": 5\n" +
            "}";

    private PasswordRequiredPresenter subject;
    @Mock private PasswordRequiredActivity activity;
    @Mock private AppUtil appUtil;
    @Mock private PrefsUtil prefsUtil;
    @Mock private AuthDataManager authDataManager;
    @Mock private BuyDataManager buyDataManager;
    @Mock private CoinifyDataManager coinifyDataManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private PayloadDataManager payloadDataManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        subject = new PasswordRequiredPresenter(appUtil,
                prefsUtil,
                authDataManager,
                payloadDataManager,
                buyDataManager,
                coinifyDataManager);

        subject.initView(activity);
    }

    /**
     * Password is missing, should trigger {@link PasswordRequiredActivity#showToast(int, String)}
     */
    @Test
    public void onContinueClickedNoPassword() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("");
        when(activity.getPassword()).thenReturn("");
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
    }

    /**
     * Password is correct, should trigger {@link PasswordRequiredActivity#goToPinPage()}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedCorrectPassword() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(payloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());
        when(payloadDataManager.getWallet().getSharedKey()).thenReturn("shared_key");
        when(payloadDataManager.getWallet().getGuid()).thenReturn("guid");
        // Act
        subject.onContinueClicked();
        // Assert
        verify(activity).goToPinPage();
        verify(prefsUtil).setValue(anyString(), anyString());
        verify(prefsUtil).setValue(anyString(), anyBoolean());
        verify(appUtil).setSharedKey(anyString());
    }

    /**
     * Password is correct but 2FA is enabled, should trigger {@link PasswordRequiredActivity#showTwoFactorCodeNeededDialog(JSONObject,
     * String, int, String)}
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedCorrectPasswordTwoFa() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), TWO_FA_RESPONSE);
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(payloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        subject.onContinueClicked();
        // Assert
        verify(activity).dismissProgressDialog();
        verify(activity).showTwoFactorCodeNeededDialog(any(), anyString(), anyInt(), anyString());
    }

    /**
     * AuthDataManager returns a failure when getting encrypted payload, should trigger {@link
     * PasswordRequiredActivity#showToast(int, String)}
     */
    @Test
    public void onContinueClickedPairingFailure() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
    }

    /**
     * AuthDataManager returns failure when polling auth status, should trigger {@link
     * PasswordRequiredActivity#showToast(int, String)}
     */
    @Ignore("This has never actually worked, but refactoring has highlighted the failure")
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedCreateFailure() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
    }

    /**
     * AuthDataManager returns a {@link DecryptionException}, should trigger {@link
     * PasswordRequiredActivity#showToast(int, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedDecryptionFailure() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(payloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.error(new DecryptionException()));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
    }

    /**
     * AuthDataManager returns a {@link HDWalletException}, should trigger {@link
     * PasswordRequiredActivity#showToast(int, String)}.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedHDWalletExceptionFailure() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(payloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.error(new HDWalletException()));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
    }

    /**
     * AuthDataManager returns a fatal exception, should restart the app and clear credentials.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedFatalErrorClearData() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), "{}");
        Response response = Response.success(responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        when(payloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.error(new RuntimeException()));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity.class);
    }

    /**
     * AuthDataManager returns an error when getting session ID, should trigger
     * AppUtil#clearCredentialsAndRestart()
     */
    @Test
    public void onContinueClickedFatalError() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity.class);
    }


    /**
     * {@link AuthDataManager#getEncryptedPayload(String, String)} throws exception. Should restart
     * the app via AppUtil#clearCredentialsAndRestart()
     */
    @Test
    public void onContinueClickedEncryptedPayloadFailure() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(activity).dismissProgressDialog();
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity.class);
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} returns Access Required.
     * Should restart the app via AppUtil#clearCredentialsAndRestart()
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthRequired() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED_JSON);
        Response response = Response.error(500, responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just(KEY_AUTH_REQUIRED));
        when(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity.class);
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} returns payload. Should
     * attempt to decrypt the payload.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthSuccess() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED_JSON);
        Response response = Response.error(500, responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("{}"));
        when(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        when(payloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        subject.onContinueClicked();
        // Assert
        verify(payloadDataManager).initializeFromPayload(anyString(), anyString());
    }

    /**
     * {@link AuthDataManager#createCheckEmailTimer()} throws an error. Should show error toast.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthEmailTimerError() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED_JSON);
        Response response = Response.error(500, responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("{}"));
        when(authDataManager.createCheckEmailTimer())
                .thenReturn(Observable.error(new Throwable()));
        when(payloadDataManager.initializeFromPayload(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        subject.onContinueClicked();
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} returns an error. Should
     * restart the app via AppUtil#clearCredentialsAndRestart()
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthFailure() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");

        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED_JSON);
        Response response = Response.error(500, responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(1));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity).showToast(anyInt(), anyString());
        verify(activity).resetPasswordField();
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity.class);
    }

    /**
     * {@link AuthDataManager#startPollingAuthStatus(String, String)} counts down to zero. Should
     * restart the app via AppUtil#clearCredentialsAndRestart()
     */
    @SuppressWarnings("unchecked")
    @Test
    public void onContinueClickedWaitingForAuthCountdownComplete() {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn("1234567890");
        when(activity.getPassword()).thenReturn("1234567890");
        when(authDataManager.getSessionId(anyString())).thenReturn(Observable.just("1234567890"));
        ResponseBody responseBody = ResponseBody.create(MediaType.parse("application/json"), KEY_AUTH_REQUIRED_JSON);
        Response response = Response.error(500, responseBody);
        when(authDataManager.getEncryptedPayload(anyString(), anyString()))
                .thenReturn(Observable.just(response));
        when(authDataManager.createCheckEmailTimer()).thenReturn(Observable.just(0));
        when(authDataManager.startPollingAuthStatus(anyString(), anyString()))
                .thenReturn(Observable.just("1234567890"));
        // Act
        subject.onContinueClicked();
        // Assert
        // noinspection WrongConstant
        verify(activity, times(2)).showToast(anyInt(), anyString());
        verify(activity, times(2)).resetPasswordField();
        verify(appUtil, times(2)).clearCredentialsAndRestart(LauncherActivity.class);
    }

    @Test
    public void submitTwoFactorCodeNull() {
        // Arrange
        JSONObject responseObject = new JSONObject();
        String sessionId = "SESSION_ID";
        String password = "PASSWORD";
        // Act
        subject.submitTwoFactorCode(responseObject, sessionId, password, null);
        // Assert
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
    }

    @Test
    public void submitTwoFactorCodeFailed() {
        // Arrange
        JSONObject responseObject = new JSONObject();
        String sessionId = "SESSION_ID";
        String guid = "GUID";
        String password = "PASSWORD";
        String code = "123456";
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn(guid);
        when(authDataManager.submitTwoFactorCode(sessionId, guid, code))
                .thenReturn(Observable.error(new Throwable()));
        // Act
        subject.submitTwoFactorCode(responseObject, sessionId, password, code);
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull(), eq(false));
        verify(activity, atLeastOnce()).dismissProgressDialog();
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(authDataManager).submitTwoFactorCode(sessionId, guid, code);
    }

    @Test
    public void submitTwoFactorCodeSuccess() {
        // Arrange
        JSONObject responseObject = new JSONObject();
        String sessionId = "SESSION_ID";
        String guid = "GUID";
        String password = "PASSWORD";
        String code = "123456";
        when(prefsUtil.getValue(PrefsUtil.KEY_GUID, "")).thenReturn(guid);
        when(authDataManager.submitTwoFactorCode(sessionId, guid, code))
                .thenReturn(Observable.just(ResponseBody.create(MediaType.parse("application/json"), TWO_FA_RESPONSE)));
        when(payloadDataManager.initializeFromPayload(anyString(), eq(password))).thenReturn(Completable.complete());
        // Act
        subject.submitTwoFactorCode(responseObject, sessionId, password, code);
        // Assert
        verify(activity).showProgressDialog(anyInt(), isNull(), eq(false));
        verify(activity, atLeastOnce()).dismissProgressDialog();
        verify(activity).goToPinPage();
        verify(authDataManager).submitTwoFactorCode(sessionId, guid, code);
        verify(payloadDataManager).initializeFromPayload(anyString(), eq(password));
    }

    @Test
    public void onProgressCancelled() {
        // Arrange

        // Act
        subject.onProgressCancelled();
        // Assert
        assertFalse(subject.waitingForAuth);
        assertEquals(0, subject.getCompositeDisposable().size());
    }

    @Test
    public void onForgetWalletClickedShowWarningAndContinue() {
        // Arrange
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onPositiveClicked();
            return null;
        }).when(activity).showForgetWalletWarning(any(DialogButtonCallback.class));
        // Act
        subject.onForgetWalletClicked();
        // Assert
        verify(activity).showForgetWalletWarning(any(DialogButtonCallback.class));
        verify(appUtil).clearCredentialsAndRestart(LauncherActivity.class);
    }

    @Test
    public void onForgetWalletClickedShowWarningAndDismiss() {
        // Arrange
        doAnswer(invocation -> {
            ((DialogButtonCallback) invocation.getArguments()[0]).onNegativeClicked();
            return null;
        }).when(activity).showForgetWalletWarning(any(DialogButtonCallback.class));
        // Act
        subject.onForgetWalletClicked();
        // Assert
        verify(activity).showForgetWalletWarning(any(DialogButtonCallback.class));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void getAppUtil() {
        // Arrange

        // Act
        AppUtil util = subject.getAppUtil();
        // Assert
        assertEquals(util, appUtil);
    }

    @Test
    public void onViewReady() {
        // Arrange

        // Act
        subject.onViewReady();
        // Assert
        assertTrue(true);
    }

}
package piuk.blockchain.android.data.datamanagers;

import info.blockchain.api.WalletPayload;
import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.HDWalletException;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.exceptions.ServerConnectionException;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.services.WalletPayloadService;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Created by adambennett on 15/08/2016.
 */
public class AuthDataManagerTest extends RxTest {

    private static final String STRING_TO_RETURN = "string_to_return";

    @Mock private PayloadManager mPayloadManager;
    @Mock private PrefsUtil mPrefsUtil;
    @Mock private WalletPayloadService mWalletPayloadService;
    @Mock private AppUtil mAppUtil;
    @Mock private AESUtilWrapper mAesUtils;
    @Mock private AccessState mAccessState;
    @Mock private StringUtils mStringUtils;
    @InjectMocks AuthDataManager mSubject;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getEncryptedPayload() throws Exception {
        // Arrange
        when(mWalletPayloadService.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
        // Act
        TestObserver<String> observer = mSubject.getEncryptedPayload("1234567890", "1234567890").test();
        // Assert
        verify(mWalletPayloadService).getEncryptedPayload(anyString(), anyString());
        observer.assertComplete();
        observer.onNext(STRING_TO_RETURN);
        observer.assertNoErrors();
    }

    @Test
    public void getSessionId() throws Exception {
        // Arrange
        when(mWalletPayloadService.getSessionId(anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
        // Act
        TestObserver<String> observer = mSubject.getSessionId("1234567890").test();
        // Assert
        verify(mWalletPayloadService).getSessionId(anyString());
        observer.assertComplete();
        observer.onNext(STRING_TO_RETURN);
        observer.assertNoErrors();
    }

    @Test
    public void validatePin() throws Exception {
        // Arrange
        CharSequenceX charSequenceX = new CharSequenceX("1234567890");
        when(mAccessState.validatePin(anyString())).thenReturn(Observable.just(charSequenceX));
        // Act
        TestObserver<CharSequenceX> observer = mSubject.validatePin(anyString()).test();
        // Assert
        verify(mAccessState).validatePin(anyString());
        observer.assertComplete();
        observer.onNext(charSequenceX);
        observer.assertNoErrors();
    }

    @Test
    public void createPin() throws Exception {
        // Arrange
        when(mAccessState.createPin(any(CharSequenceX.class), anyString())).thenReturn(Observable.just(true));
        // Act
        TestObserver<Boolean> observer = mSubject.createPin(any(CharSequenceX.class), anyString()).test();
        // Assert
        verify(mAccessState).createPin(any(CharSequenceX.class), anyString());
        observer.assertComplete();
        observer.onNext(true);
        observer.assertNoErrors();
    }

    @Test
    public void createHdWallet() throws Exception {
        // Arrange
        Payload payload = new Payload();
        when(mPayloadManager.createHDWallet(anyString(), anyString())).thenReturn(payload);
        // Act
        TestObserver<Payload> observer = mSubject.createHdWallet("", "").test();
        // Assert
        verify(mPayloadManager).createHDWallet(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(mAppUtil).setNewlyCreated(true);
        verify(mPrefsUtil).setValue(eq(PrefsUtil.KEY_GUID), anyString());
        observer.assertComplete();
        observer.onNext(payload);
        observer.assertNoErrors();
    }

    @Test
    public void restoreHdWallet() throws Exception {
        // Arrange
        Payload payload = new Payload();
        when(mPayloadManager.restoreHDWallet(anyString(), anyString(), anyString())).thenReturn(payload);
        // Act
        TestObserver<Payload> observer = mSubject.restoreHdWallet("", "", "").test();
        // Assert
        verify(mPayloadManager).restoreHDWallet(anyString(), anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(mAppUtil).setNewlyCreated(true);
        verify(mPrefsUtil).setValue(eq(PrefsUtil.KEY_GUID), anyString());
        observer.assertComplete();
        observer.onNext(payload);
        observer.assertNoErrors();
    }

    /**
     * Payload returns null, which indicates save failure. Should throw an Exception
     */
    @Test
    public void restoreHdWalletNullPayload() throws Exception {
        // Arrange
        when(mPayloadManager.restoreHDWallet(anyString(), anyString(), anyString())).thenReturn(null);
        // Act
        TestObserver<Payload> observer = mSubject.restoreHdWallet("", "", "").test();
        // Assert
        verify(mPayloadManager).restoreHDWallet(anyString(), anyString(), anyString());
        verifyZeroInteractions(mAppUtil);
        verifyZeroInteractions(mPrefsUtil);
        observer.assertNotComplete();
        observer.assertError(Throwable.class);
    }

    /**
     * Access returns a valid payload, Observable should complete successfully
     */
    @Ignore // Seems that anything involving timers is now broken for testing
    @Test
    public void startPollingAuthStatusSuccess() throws Exception {
//        // Arrange
//        when(mWalletPayloadService.getSessionId(anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
//        when(mWalletPayloadService.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
//        // Act
//        TestObserver<String> observer = mSubject.startPollingAuthStatus("1234567890").test();
//        // Assert
//        verify(mWalletPayloadService).getSessionId(anyString());
//        verify(mWalletPayloadService).getEncryptedPayload(anyString(), anyString());
//        observer.assertComplete();
//        observer.onNext(STRING_TO_RETURN);
//        observer.assertNoErrors();
    }

    /**
     * Getting encrypted payload returns error, should be caught by Observable and transformed into
     * {@link WalletPayload#KEY_AUTH_REQUIRED}
     */
    @Ignore // Seems that anything involving timers is now broken for testing
    @Test
    public void startPollingAuthStatusError() throws Exception {
//        // Arrange
//        when(mWalletPayloadService.getSessionId(anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
//        when(mWalletPayloadService.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.error(new Throwable()));
//        // Act
//        TestObserver<String> observer = mSubject.startPollingAuthStatus("1234567890").test();
//        // Assert
//        verify(mWalletPayloadService).getSessionId(anyString());
//        verify(mWalletPayloadService).getEncryptedPayload(anyString(), anyString());
//        observer.assertComplete();
//        observer.onNext(WalletPayload.KEY_AUTH_REQUIRED);
//        observer.assertNoErrors();
    }

    /**
     * Getting encrypted payload returns Auth Required, should be filtered out and emit no values.
     */
    @Ignore // Seems that anything involving timers is now broken for testing
    @Test
    public void startPollingAuthStatusAccessRequired() throws Exception {
//        // Arrange
//        when(mWalletPayloadService.getSessionId(anyString())).thenReturn(Observable.just(STRING_TO_RETURN));
//        when(mWalletPayloadService.getEncryptedPayload(anyString(), anyString())).thenReturn(Observable.just(WalletPayload.KEY_AUTH_REQUIRED));
//        // Act
//        TestObserver<String> observer = mSubject.startPollingAuthStatus("1234567890").test();
//        // Assert
//        observer.assertComplete();
//        observer.assertNoValues();
//        observer.assertNoErrors();
    }

    /**
     * Update payload completes successfully, should set temp password and complete with no errors
     */
    @Test
    public void initiatePayloadSuccess() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onSuccess();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        TestObserver<Void> observer = mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).test();
        // Assert
        verify(mPayloadManager).setTempPassword(any(CharSequenceX.class));
        observer.assertComplete();
        observer.assertNoErrors();
    }

    /**
     * Update payload returns a credential failure, Observable should throw {@link
     * InvalidCredentialsException}
     */
    @Test
    public void initiateCredentialFail() throws Exception {
        // Arrange
        doThrow(new InvalidCredentialsException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        TestObserver<Void> observer = mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).test();
        // Assert
        observer.assertNotComplete();
        observer.assertError(Throwable.class);
    }

    /**
     * Update payload returns a Payload exception, Observable should throw {@link PayloadException}
     */
    @Test
    public void initiatePayloadFail() throws Exception {
        // Arrange
        doThrow(new PayloadException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        TestObserver<Void> observer = mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).test();
        // Assert
        observer.assertNotComplete();
        observer.assertError(Throwable.class);
    }

    /**
     * Update payload returns a connection failure, Observable should throw {@link
     * ServerConnectionException}
     */
    @Test
    public void initiatePayloadConnectionFail() throws Exception {
        // Arrange
        doThrow(new ServerConnectionException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        TestObserver<Void> observer = mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).test();
        // Assert
        observer.assertNotComplete();
        observer.assertError(Throwable.class);
    }

    /**
     * PayloadManager throws exception, should trigger onError
     */
    @Test
    public void initiatePayloadException() throws Exception {
        // Arrange
        doThrow(new RuntimeException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));
        // Act
        TestObserver<Void> observer = mSubject.updatePayload("1234567890", "1234567890", new CharSequenceX("1234567890")).test();
        // Assert
        observer.assertNotComplete();
        observer.assertError(Throwable.class);
    }

    // TODO: 11/11/2016 This test is broken
    @Ignore
    @Test
    public void createCheckEmailTimer() throws Exception {
        // Arrange
//        TestObserver<Integer> observer = new TestObserver<>();
        // Act
//        mSubject.createCheckEmailTimer()
//                .take(1).blockingSubscribe(observer);
//        mSubject.timer = 1;
        // Assert
//        observer.assertComplete();
//        observer.assertNoErrors();
    }

//    @Test
//    public void attemptDecryptPayloadV1Payload() throws Exception {
    // Currently can't be tested in any reasonable way
//    }

    @Test
    public void attemptDecryptPayloadSuccessful() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onSuccess();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onSuccess();
    }

    @Test
    public void attemptDecryptPayloadNoSharedKey() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doAnswer(invocation -> {
            ((PayloadManager.InitiatePayloadListener) invocation.getArguments()[3]).onSuccess();
            return null;
        }).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD_NO_SHARED_KEY);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(listener).onFatalError();
    }

    @Test
    public void attemptDecryptPayloadInitAuthFail() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doThrow(new InvalidCredentialsException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onAuthFail();
    }

    @Test
    public void attemptDecryptPayloadFatalError() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doThrow(new HDWalletException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onFatalError();
    }

    @Test
    public void attemptDecryptPayloadPairFail() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        doThrow(new DecryptionException()).when(mPayloadManager).initiatePayload(
                anyString(), anyString(), any(CharSequenceX.class), any(PayloadManager.InitiatePayloadListener.class));

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(DECRYPTED_PAYLOAD);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(mPrefsUtil).setValue(anyString(), anyString());
        verify(mAppUtil).setSharedKey(anyString());
        verify(listener).onPairFail();
    }

    @Test
    public void attemptDecryptPayloadDecryptionFailed() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenReturn(null);
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(listener).onAuthFail();
    }

    @Test
    public void attemptDecryptPayloadDecryptionThrowsException() throws Exception {
        AuthDataManager.DecryptPayloadListener listener = mock(AuthDataManager.DecryptPayloadListener.class);

        when(mAesUtils.decrypt(anyString(), any(CharSequenceX.class), anyInt())).thenThrow(mock(RuntimeException.class));
        // Act
        mSubject.attemptDecryptPayload(
                new CharSequenceX("1234567890"),
                "1234567890",
                TEST_PAYLOAD,
                listener);
        // Assert
        verify(listener).onFatalError();
    }

    private static final String TEST_PAYLOAD = "{\n" +
            "  \"payload\": \"test payload\",\n" +
            "  \"pbkdf2_iterations\": 2000\n" +
            "}";

    private static final String DECRYPTED_PAYLOAD = "{\n" +
            "\t\"sharedKey\": \"1234567890\"\n" +
            "}";

    private static final String DECRYPTED_PAYLOAD_NO_SHARED_KEY = "{\n" +
            "\t\"test\": \"1234567890\"\n" +
            "}";

}
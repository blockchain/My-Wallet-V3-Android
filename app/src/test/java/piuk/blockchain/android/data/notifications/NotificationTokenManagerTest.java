package piuk.blockchain.android.data.notifications;

import com.google.firebase.iid.FirebaseInstanceId;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Wallet;
import io.reactivex.Completable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.testutils.RxTest;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.utils.PrefsUtil;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class NotificationTokenManagerTest extends RxTest{

    private NotificationTokenManager subject;
    @Mock private NotificationService notificationService;
    @Mock private PayloadManager payloadManager;
    @Mock private PrefsUtil prefsUtil;
    @Mock private FirebaseInstanceId firebaseInstanceId;
    @Mock private RxBus rxBus;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new NotificationTokenManager(notificationService, payloadManager, prefsUtil, firebaseInstanceId, rxBus);
    }

    @Test
    public void storeAndUpdateToken_disabledNotifications() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)).thenReturn(false);
        when(payloadManager.getPayload()).thenReturn(null);

        // Act
        subject.storeAndUpdateToken("token");
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        verify(prefsUtil).setValue(PrefsUtil.KEY_FIREBASE_TOKEN, "token");
        verifyNoMoreInteractions(prefsUtil);
    }

    @Test
    public void storeAndUpdateToken_enabledNotifications_notSignedIn() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)).thenReturn(true);
        when(payloadManager.getPayload()).thenReturn(null);

        // Act
        subject.storeAndUpdateToken("token");
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        verify(prefsUtil).setValue(PrefsUtil.KEY_FIREBASE_TOKEN, "token");
        verifyNoMoreInteractions(prefsUtil);
    }

    @Test
    public void storeAndUpdateToken_enabledNotifications_signedIn() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)).thenReturn(true);

        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getGuid()).thenReturn("guid");
        when(mockPayload.getSharedKey()).thenReturn("sharedKey");
        when(payloadManager.getPayload()).thenReturn(mockPayload);

        when(notificationService.sendNotificationToken(anyString(), anyString(), anyString())).thenReturn(Completable.complete());

        // Act
        subject.storeAndUpdateToken("token");
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        verify(prefsUtil).setValue(PrefsUtil.KEY_FIREBASE_TOKEN, "token");
        verify(notificationService).sendNotificationToken("token","guid","sharedKey");
        verifyNoMoreInteractions(prefsUtil);
    }

    @Test
    public void enableNotifications_requestToken() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "")).thenReturn("");

        when(firebaseInstanceId.getToken()).thenReturn("token");

        // Act
        subject.enableNotifications();
        // Assert
        verify(prefsUtil).setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    public void enableNotifications_storedToken() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "")).thenReturn("token");
        when(prefsUtil.getValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true)).thenReturn(true);

        Wallet mockPayload = mock(Wallet.class);
        when(mockPayload.getGuid()).thenReturn("guid");
        when(mockPayload.getSharedKey()).thenReturn("sharedKey");
        when(payloadManager.getPayload()).thenReturn(mockPayload);

        when(notificationService.sendNotificationToken(anyString(), anyString(), anyString())).thenReturn(Completable.complete());

        // Act
        TestObserver<Void> testObservable = subject.enableNotifications().test();
        // Assert
        testObservable.assertComplete();
        testObservable.assertNoErrors();
        verify(prefsUtil).setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, true);
        verify(payloadManager, atLeastOnce()).getPayload();
        verify(notificationService).sendNotificationToken(anyString(), anyString(), anyString());
        verifyNoMoreInteractions(notificationService);
    }

    @Test
    public void disableNotifications() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_FIREBASE_TOKEN, "")).thenReturn("");

        // Act
        TestObserver<Void> testObservable = subject.disableNotifications().test();
        // Assert
        testObservable.assertComplete();
        testObservable.assertNoErrors();
        verify(prefsUtil).setValue(PrefsUtil.KEY_PUSH_NOTIFICATION_ENABLED, false);
        verify(firebaseInstanceId).deleteInstanceId();
        verifyNoMoreInteractions(notificationService);
    }
}
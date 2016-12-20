package piuk.blockchain.android.data.services;

import info.blockchain.api.Settings;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;

public class SettingsServiceTest extends RxTest {

    private SettingsService subject;
    @Mock Settings settings;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new SettingsService(settings);
    }

    @Test
    public void updateEmail() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((Settings.ResultListener) invocation.getArguments()[1]).onSuccess();
            return null;
        }).when(settings).setEmail(
                anyString(), any(Settings.ResultListener.class));
        // Act
        TestObserver<Boolean> observer = subject.updateEmail("email").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void updateSms() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((Settings.ResultListener) invocation.getArguments()[1]).onSuccess();
            return null;
        }).when(settings).setSms(
                anyString(), any(Settings.ResultListener.class));
        // Act
        TestObserver<Boolean> observer = subject.updateSms("sms").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void verifySms() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((Settings.ResultListener) invocation.getArguments()[1]).onSuccess();
            return null;
        }).when(settings).verifySms(
                anyString(), any(Settings.ResultListener.class));
        // Act
        TestObserver<Boolean> observer = subject.verifySms("code").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void updateTor() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((Settings.ResultListener) invocation.getArguments()[1]).onSuccess();
            return null;
        }).when(settings).setTorBlocked(
                anyBoolean(), any(Settings.ResultListener.class));
        // Act
        TestObserver<Boolean> observer = subject.updateTor(true).test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void updatePasswordHint() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((Settings.ResultListener) invocation.getArguments()[1]).onSuccess();
            return null;
        }).when(settings).setPasswordHint1(
                anyString(), any(Settings.ResultListener.class));
        // Act
        TestObserver<Boolean> observer = subject.updatePasswordHint("code").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void disableNotificationsFailed() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((Settings.ResultListener) invocation.getArguments()[1]).onFail();
            return null;
        }).when(settings).disableNotification(
                anyInt(), any(Settings.ResultListener.class));
        // Act
        TestObserver<Boolean> observer = subject.disableNotifications(0).test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(false, observer.values().get(0));
    }

    @Test
    public void enableNotificationsBadRequest() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((Settings.ResultListener) invocation.getArguments()[1]).onBadRequest();
            return null;
        }).when(settings).enableNotification(
                anyInt(), any(Settings.ResultListener.class));
        // Act
        TestObserver<Boolean> observer = subject.enableNotifications(0).test();
        // Assert
        observer.assertNotComplete();
        observer.assertNoValues();
        observer.assertError(Throwable.class);
    }

    @Test
    public void updateTwoFactor() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((Settings.ResultListener) invocation.getArguments()[1]).onSuccess();
            return null;
        }).when(settings).setAuthType(
                anyInt(), any(Settings.ResultListener.class));
        // Act
        TestObserver<Boolean> observer = subject.updateTwoFactor(0).test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

}
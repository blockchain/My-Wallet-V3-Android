package piuk.blockchain.android.data.datamanagers;

import info.blockchain.api.Settings;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.services.SettingsService;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SettingsDataManagerTest extends RxTest {

    private SettingsDataManager subject;
    @Mock SettingsService settingsService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new SettingsDataManager(settingsService);
    }

    @Test
    public void updateSettings() throws Exception {
        // Arrange
        Settings settings = new Settings();
        when(settingsService.updateSettings(anyString(), anyString())).thenReturn(Observable.just(settings));
        // Act
        TestObserver<Settings> observer = subject.updateSettings("guid", "sharedKey").test();
        // Assert
        verify(settingsService).updateSettings(anyString(), anyString());
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(settings, observer.values().get(0));
    }

    @Test
    public void updateEmail() throws Exception {
        // Arrange
        when(settingsService.updateEmail(anyString())).thenReturn(Observable.just(true));
        // Act
        TestObserver<Boolean> observer = subject.updateEmail("email").test();
        // Assert
        verify(settingsService).updateEmail(anyString());
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void updateSms() throws Exception {
        // Arrange
        when(settingsService.updateSms(anyString())).thenReturn(Observable.just(true));
        // Act
        TestObserver<Boolean> observer = subject.updateSms("sms").test();
        // Assert
        verify(settingsService).updateSms(anyString());
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void verifySms() throws Exception {
        // Arrange
        when(settingsService.verifySms(anyString())).thenReturn(Observable.just(true));
        // Act
        TestObserver<Boolean> observer = subject.verifySms("code").test();
        // Assert
        verify(settingsService).verifySms(anyString());
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void updateTor() throws Exception {
        // Arrange
        when(settingsService.updateTor(anyBoolean())).thenReturn(Observable.just(true));
        // Act
        TestObserver<Boolean> observer = subject.updateTor(true).test();
        // Assert
        verify(settingsService).updateTor(anyBoolean());
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void updatePasswordHint() throws Exception {
        // Arrange
        when(settingsService.updatePasswordHint(anyString())).thenReturn(Observable.just(true));
        // Act
        TestObserver<Boolean> observer = subject.updatePasswordHint("hint").test();
        // Assert
        verify(settingsService).updatePasswordHint(anyString());
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void updateNotificationsEnabled() throws Exception {
        // Arrange
        when(settingsService.enableNotifications(anyInt())).thenReturn(Observable.just(true));
        // Act
        TestObserver<Boolean> observer = subject.updateNotifications(0, true).test();
        // Assert
        verify(settingsService).enableNotifications(anyInt());
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void updateNotificationsDisabled() throws Exception {
        // Arrange
        when(settingsService.disableNotifications(anyInt())).thenReturn(Observable.just(true));
        // Act
        TestObserver<Boolean> observer = subject.updateNotifications(0, false).test();
        // Assert
        verify(settingsService).disableNotifications(anyInt());
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void updateTwoFactor() throws Exception {
        // Arrange
        when(settingsService.updateTwoFactor(anyInt())).thenReturn(Observable.just(true));
        // Act
        TestObserver<Boolean> observer = subject.updateTwoFactor(0).test();
        // Assert
        verify(settingsService).updateTwoFactor(anyInt());
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

}
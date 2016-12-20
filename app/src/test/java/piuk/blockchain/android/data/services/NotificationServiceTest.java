package piuk.blockchain.android.data.services;

import info.blockchain.api.Notifications;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;

public class NotificationServiceTest extends RxTest {

    private NotificationService subject;
    @Mock Notifications notifications;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new NotificationService(notifications);
    }

    @Test
    public void sendNotificationToken() throws Exception {
        // Arrange

        // Act
        TestObserver<Void> observer = subject.sendNotificationToken("", "", "").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
    }

}
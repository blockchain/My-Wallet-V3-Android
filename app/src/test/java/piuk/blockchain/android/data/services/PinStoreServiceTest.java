package piuk.blockchain.android.data.services;

import info.blockchain.api.PinStore;
import info.blockchain.wallet.exceptions.InvalidCredentialsException;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class PinStoreServiceTest extends RxTest {

    private PinStoreService subject;
    @Mock PinStore pinStore;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new PinStoreService(pinStore);
    }

    @Test
    public void setAccessKeySuccess() throws Exception {
        // Arrange
        when(pinStore.setAccess(anyString(), anyString(), anyString())).thenReturn(new JSONObject(SUCCESS_RESPONSE));
        // Act
        TestObserver<Boolean> observer = subject.setAccessKey("", "", "").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0));
    }

    @Test
    public void setAccessKeyFailed() throws Exception {
        // Arrange
        when(pinStore.setAccess(anyString(), anyString(), anyString())).thenReturn(new JSONObject());
        // Act
        TestObserver<Boolean> observer = subject.setAccessKey("", "", "").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(false, observer.values().get(0));
    }

    @Test
    public void validateAccess() throws Exception {
        // Arrange
        JSONObject value = new JSONObject(SUCCESS_RESPONSE);
        when(pinStore.validateAccess(anyString(), anyString())).thenReturn(value);
        // Act
        TestObserver<JSONObject> observer = subject.validateAccess("", "").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(value, observer.values().get(0));
    }

    @Test
    public void validateAccessException() throws Exception {
        // Arrange
        when(pinStore.validateAccess(anyString(), anyString())).thenThrow(Exception.class);
        // Act
        TestObserver<JSONObject> observer = subject.validateAccess("", "").test();
        // Assert
        observer.assertNotComplete();
        observer.assertError(Throwable.class);
    }

    @Test
    public void validateAccessInvalidCredentials() throws Exception {
        // Arrange
        when(pinStore.validateAccess(anyString(), anyString())).thenThrow(new Exception("Incorrect PIN"));
        // Act
        TestObserver<JSONObject> observer = subject.validateAccess("", "").test();
        // Assert
        observer.assertNotComplete();
        observer.assertError(InvalidCredentialsException.class);
    }

    private static final String SUCCESS_RESPONSE = "{\n" +
            "  \"success\": true\n" +
            "}";

}
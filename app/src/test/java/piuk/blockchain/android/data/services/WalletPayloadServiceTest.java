package piuk.blockchain.android.data.services;

import info.blockchain.api.WalletPayload;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class WalletPayloadServiceTest extends RxTest {

    private static final String STRING_TO_RETURN = "string_to_return";

    private WalletPayloadService subject;
    @Mock WalletPayload walletPayload;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new WalletPayloadService(walletPayload);
    }

    @Test
    public void getEncryptedPayload() throws Exception {
        // Arrange
        when(walletPayload.getEncryptedPayload(anyString(), anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        TestObserver<String> observer = subject.getEncryptedPayload("guid", "1234567890").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(STRING_TO_RETURN, observer.values().get(0));
    }

    @Test
    public void getSessionId() throws Exception {
        // Arrange
        when(walletPayload.getSessionId(anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        TestObserver<String> observer = subject.getSessionId("guid").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(STRING_TO_RETURN, observer.values().get(0));
    }

    @Test
    public void getPairingEncryptionPassword() throws Exception {
        // Arrange
        when(walletPayload.getPairingEncryptionPassword(anyString())).thenReturn(STRING_TO_RETURN);
        // Act
        TestObserver<String> observer = subject.getPairingEncryptionPassword("guid").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(STRING_TO_RETURN, observer.values().get(0));
    }

}
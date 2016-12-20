package piuk.blockchain.android.data.services;

import info.blockchain.api.Unspent;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.UnspentOutputs;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UnspentServiceTest extends RxTest {

    private UnspentService subject;
    @Mock Unspent unspent;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new UnspentService(unspent);
    }

    @Test
    public void getUnspentOutputsSuccess() throws Exception {
        // Arrange
        when(unspent.getUnspentOutputs(anyString())).thenReturn(new JSONObject());
        Payment mockPayment = mock(Payment.class);
        UnspentOutputs mockOutputs = mock(UnspentOutputs.class);
        when(mockPayment.getCoins(any(JSONObject.class))).thenReturn(mockOutputs);
        // Act
        TestObserver<UnspentOutputs> observer = subject.getUnspentOutputs("legacy", mockPayment).test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(mockOutputs, observer.values().get(0));
    }

    @Test
    public void getUnspentOutputsFailure() throws Exception {
        // Arrange
        when(unspent.getUnspentOutputs(anyString())).thenReturn(null);
        Payment mockPayment = mock(Payment.class);
        // Act
        TestObserver<UnspentOutputs> observer = subject.getUnspentOutputs("legacy", mockPayment).test();
        // Assert
        observer.assertNotComplete();
        observer.assertNoValues();
        observer.assertError(Throwable.class);
    }

}
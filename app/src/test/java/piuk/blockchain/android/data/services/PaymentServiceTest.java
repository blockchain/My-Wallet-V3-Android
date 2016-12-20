package piuk.blockchain.android.data.services;

import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.SpendableUnspentOutputs;

import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

public class PaymentServiceTest extends RxTest {

    private PaymentService subject;
    @Mock Payment payment;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new PaymentService(payment);
    }

    @Test
    public void submitPaymentSuccess() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((Payment.SubmitPaymentListener) invocation.getArguments()[6]).onSuccess("hash");
            return null;
        }).when(payment).submitPayment(
                any(SpendableUnspentOutputs.class),
                anyListOf(ECKey.class),
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class),
                any(Payment.SubmitPaymentListener.class));
        // Act
        TestObserver<String> observer = subject.submitPayment(mock(
                SpendableUnspentOutputs.class),
                mock(List.class),
                "",
                "",
                mock(BigInteger.class),
                mock(BigInteger.class)).test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals("hash", observer.values().get(0));
    }

    @Test
    public void submitPaymentFailure() throws Exception {
        // Arrange
        doAnswer(invocation -> {
            ((Payment.SubmitPaymentListener) invocation.getArguments()[6]).onFail("error");
            return null;
        }).when(payment).submitPayment(
                any(SpendableUnspentOutputs.class),
                anyListOf(ECKey.class),
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class),
                any(Payment.SubmitPaymentListener.class));
        // Act
        TestObserver<String> observer = subject.submitPayment(mock(
                SpendableUnspentOutputs.class),
                mock(List.class),
                "",
                "",
                mock(BigInteger.class),
                mock(BigInteger.class)).test();
        // Assert
        observer.assertNotComplete();
        observer.assertNoValues();
        observer.assertError(Throwable.class);
    }

    @Test
    public void submitPaymentError() throws Exception {
        // Arrange
        doThrow(new RuntimeException()).when(payment).submitPayment(
                any(SpendableUnspentOutputs.class),
                anyListOf(ECKey.class),
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class),
                any(Payment.SubmitPaymentListener.class));
        // Act
        TestObserver<String> observer = subject.submitPayment(mock(
                SpendableUnspentOutputs.class),
                mock(List.class),
                "",
                "",
                mock(BigInteger.class),
                mock(BigInteger.class)).test();
        // Assert
        observer.assertNotComplete();
        observer.assertNoValues();
        observer.assertError(Throwable.class);
    }

}
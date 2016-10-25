package piuk.blockchain.android.data.services;

import info.blockchain.api.TransactionDetails;
import info.blockchain.wallet.transaction.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import piuk.blockchain.android.RxTest;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionDetailsServiceTest extends RxTest {

    private TransactionDetailsService subject;
    @Mock TransactionDetails transactionDetails;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new TransactionDetailsService(transactionDetails);
    }

    @Test
    public void getTransactionDetailsFromHash() throws Exception {
        // Arrange
        TestSubscriber<Transaction> subscriber = new TestSubscriber<>();
        Transaction mockTransaction = mock(Transaction.class);
        when(transactionDetails.getTransactionDetails(anyString())).thenReturn(mockTransaction);
        // Act
        subject.getTransactionDetailsFromHash("hash").toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
        assertEquals(mockTransaction, subscriber.getOnNextEvents().get(0));
    }

}
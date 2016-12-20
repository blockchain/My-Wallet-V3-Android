package piuk.blockchain.android.data.services;

import info.blockchain.api.TransactionDetails;
import info.blockchain.wallet.transaction.Transaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;

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
        Transaction mockTransaction = mock(Transaction.class);
        when(transactionDetails.getTransactionDetails(anyString())).thenReturn(mockTransaction);
        // Act
        TestObserver<Transaction> observer = subject.getTransactionDetailsFromHash("hash").test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(mockTransaction, observer.values().get(0));
    }

}
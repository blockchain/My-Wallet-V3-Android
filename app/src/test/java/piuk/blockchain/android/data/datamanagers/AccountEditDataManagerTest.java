package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.payment.data.SpendableUnspentOutputs;
import info.blockchain.wallet.payment.data.SuggestedFee;
import info.blockchain.wallet.payment.data.UnspentOutputs;

import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.services.PaymentService;
import piuk.blockchain.android.data.services.UnspentService;
import piuk.blockchain.android.ui.send.PendingTransaction;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccountEditDataManagerTest extends RxTest {

    private AccountEditDataManager subject;
    @Mock PayloadManager payloadManager;
    @Mock UnspentService unspentService;
    @Mock PaymentService paymentService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new AccountEditDataManager(unspentService, paymentService, payloadManager);
    }

    @Test
    public void getPendingTransactionForLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        Payment payment = new Payment();
        SuggestedFee suggestedFee = new SuggestedFee();
        suggestedFee.defaultFeePerKb = BigInteger.valueOf(100);
        DynamicFeeCache.getInstance().setSuggestedFee(suggestedFee);
        Payload mockPayload = mock(Payload.class, RETURNS_DEEP_STUBS);
        when(mockPayload.getHdWallet().getDefaultIndex()).thenReturn(0);
        when(mockPayload.getHdWallet().getAccounts().get(anyInt())).thenReturn(mock(Account.class));
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(payloadManager.getNextReceiveAddress(anyInt())).thenReturn("address");
        when(unspentService.getUnspentOutputs(anyString(), any(Payment.class))).thenReturn(Observable.just(mock(UnspentOutputs.class)));
        // Act
        TestObserver<PendingTransaction> observer = subject.getPendingTransactionForLegacyAddress(legacyAddress, payment).test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(PendingTransaction.class, observer.values().get(0).getClass());
    }

    @Test
    public void submitPayment() throws Exception {
        // Arrange
        when(paymentService.submitPayment(
                any(SpendableUnspentOutputs.class),
                anyListOf(ECKey.class),
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class))).thenReturn(Observable.just("hash"));
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
    public void syncPayloadWithServer() throws Exception {
        // Arrange
        when(payloadManager.savePayloadToServer()).thenReturn(true);
        // Act
        TestObserver<Boolean> observer = subject.syncPayloadWithServer().test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
        assertEquals(true, observer.values().get(0).booleanValue());
    }

    @Test
    public void updateBalancesAndTransactions() throws Exception {
        // Arrange

        // Act
        TestObserver<Void> observer = subject.updateBalancesAndTransactions().test();
        // Assert
        observer.assertComplete();
        observer.assertNoErrors();
    }

}
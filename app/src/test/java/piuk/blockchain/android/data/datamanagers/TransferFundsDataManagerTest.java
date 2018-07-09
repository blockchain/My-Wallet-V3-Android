package piuk.blockchain.android.data.datamanagers;

import info.blockchain.api.data.UnspentOutputs;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;
import info.blockchain.wallet.payment.SpendableUnspentOutputs;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.ECKey;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.testutils.RxTest;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.payments.SendDataManager;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.send.PendingTransaction;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransferFundsDataManagerTest extends RxTest {

    private TransferFundsDataManager subject;
    @Mock private SendDataManager sendDataManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private PayloadDataManager payloadDataManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) private DynamicFeeCache dynamicFeeCache;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        subject = new TransferFundsDataManager(payloadDataManager, sendDataManager, dynamicFeeCache);
    }

    @Test
    public void getTransferableFundTransactionListForDefaultAccount() throws Exception {
        // Arrange
        LegacyAddress legacyAddress1 = new LegacyAddress();
        legacyAddress1.setAddress("address");
        legacyAddress1.setPrivateKey("");
        List<LegacyAddress> legacyAddresses = Arrays.asList(legacyAddress1, legacyAddress1, legacyAddress1);
        when(dynamicFeeCache.getBtcFeeOptions().getRegularFee()).thenReturn(1L);
        when(payloadDataManager.getWallet().getLegacyAddressList()).thenReturn(legacyAddresses);
        when(payloadDataManager.getAddressBalance(anyString())).thenReturn(BigInteger.TEN);
        UnspentOutputs unspentOutputs = mock(UnspentOutputs.class);
        when(unspentOutputs.getNotice()).thenReturn(null);
        when(sendDataManager.getUnspentOutputs(anyString())).thenReturn(Observable.just(unspentOutputs));
        SpendableUnspentOutputs spendableUnspentOutputs = new SpendableUnspentOutputs();
        spendableUnspentOutputs.setAbsoluteFee(BigInteger.TEN);
        when(sendDataManager.getSpendableCoins(any(UnspentOutputs.class), any(BigInteger.class), any(BigInteger.class)))
                .thenReturn(spendableUnspentOutputs);
        when(sendDataManager.getMaximumAvailable(unspentOutputs, BigInteger.valueOf(1_000L)))
                .thenReturn(Pair.of(BigInteger.valueOf(1_000_000L), BigInteger.TEN));
        // Act
        TestObserver<Triple<List<PendingTransaction>, Long, Long>> testObserver =
                subject.getTransferableFundTransactionListForDefaultAccount().test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void sendPaymentSuccess() throws Exception {
        // Arrange
        when(sendDataManager.submitBtcPayment(
                any(SpendableUnspentOutputs.class),
                anyList(),
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class))).thenReturn(Observable.just("hash"));

        PendingTransaction transaction1 = new PendingTransaction();
        transaction1.sendingObject = new ItemAccount("", "", null, null, null);
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("");
        transaction1.sendingObject.setAccountObject(legacyAddress);
        transaction1.bigIntAmount = new BigInteger("1000000");
        transaction1.bigIntFee = new BigInteger("100");
        transaction1.unspentOutputBundle = new SpendableUnspentOutputs();

        List<PendingTransaction> pendingTransactions = Arrays.asList(transaction1, transaction1, transaction1);
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        when(payloadDataManager.getNextReceiveAddress(anyInt())).thenReturn(Observable.just("address"));
        when(payloadDataManager.getAddressECKey(any(LegacyAddress.class), anyString()))
                .thenReturn(mock(ECKey.class));
        when(payloadDataManager.getWallet().getHdWallets().get(0).getAccounts().get(anyInt()))
                .thenReturn(mock(Account.class));
        // Act
        TestObserver<String> testObserver =
                subject.sendPayment(pendingTransactions, "password").test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValues("hash", "hash", "hash");
    }

    @Test
    public void sendPaymentError() throws Exception {
        // Arrange
        when(sendDataManager.submitBtcPayment(
                any(SpendableUnspentOutputs.class),
                anyList(),
                anyString(),
                anyString(),
                any(BigInteger.class),
                any(BigInteger.class))).thenReturn(Observable.error(new Throwable()));

        PendingTransaction transaction1 = new PendingTransaction();
        transaction1.sendingObject = new ItemAccount("", "", null, null, null);
        LegacyAddress legacyAddress = new LegacyAddress();
        legacyAddress.setAddress("");
        transaction1.sendingObject.setAccountObject(legacyAddress);
        transaction1.bigIntAmount = new BigInteger("1000000");
        transaction1.bigIntFee = new BigInteger("100");
        transaction1.unspentOutputBundle = new SpendableUnspentOutputs();

        List<PendingTransaction> pendingTransactions = Arrays.asList(transaction1, transaction1, transaction1);
        when(payloadDataManager.syncPayloadWithServer()).thenReturn(Completable.complete());
        when(payloadDataManager.getNextReceiveAddress(anyInt())).thenReturn(Observable.just("address"));
        when(payloadDataManager.getAddressECKey(any(LegacyAddress.class), anyString()))
                .thenReturn(mock(ECKey.class));
        when(payloadDataManager.getWallet().getHdWallets().get(0).getAccounts().get(anyInt()))
                .thenReturn(mock(Account.class));
        // Act
        TestObserver<String> testObserver =
                subject.sendPayment(pendingTransactions, "password").test();
        // Assert
        testObserver.assertNotComplete();
        testObserver.assertError(Throwable.class);
        testObserver.assertNoValues();
    }

}
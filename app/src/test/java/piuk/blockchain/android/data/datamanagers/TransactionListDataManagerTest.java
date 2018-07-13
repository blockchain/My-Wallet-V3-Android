package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.ethereum.data.EthLatestBlock;
import info.blockchain.wallet.ethereum.data.EthTransaction;
import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payload.data.Account;
import info.blockchain.wallet.payload.data.LegacyAddress;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.NoSuchElementException;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.testutils.RxTest;
import piuk.blockchain.android.data.bitcoincash.BchDataManager;
import piuk.blockchain.androidcore.data.currency.CryptoCurrency;
import piuk.blockchain.androidcore.data.currency.CurrencyState;
import piuk.blockchain.android.data.ethereum.EthDataManager;
import piuk.blockchain.androidcore.data.ethereum.models.CombinedEthModel;
import piuk.blockchain.androidcore.data.transactions.TransactionListStore;
import piuk.blockchain.androidcore.data.transactions.models.BtcDisplayable;
import piuk.blockchain.androidcore.data.transactions.models.Displayable;
import piuk.blockchain.android.ui.account.ItemAccount;

import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TransactionListDataManagerTest extends RxTest {

    @Mock private PayloadManager payloadManager;
    @Mock private EthDataManager ethDataManager;
    @Mock private BchDataManager bchDataManager;
    @Mock private CurrencyState currencyState;
    private TransactionListStore transactionListStore;
    private TransactionListDataManager subject;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        transactionListStore = new TransactionListStore();

        subject = new TransactionListDataManager(
                payloadManager,
                ethDataManager,
                bchDataManager,
                transactionListStore,
                currencyState);
    }

    @Test
    public void fetchTransactionsAccountTagAll() throws Exception {
        // Arrange
        Account account = new Account();
        TransactionSummary summary = new TransactionSummary();
        summary.setConfirmations(3);
        summary.setDirection(TransactionSummary.Direction.RECEIVED);
        summary.setFee(BigInteger.ONE);
        summary.setTotal(BigInteger.TEN);
        summary.setHash("hash");
        summary.setInputsMap(new HashMap<>());
        summary.setOutputsMap(new HashMap<>());
        summary.setTime(1000000L);
        List<TransactionSummary> transactionSummaries = Collections.singletonList(summary);
        when(payloadManager.getAllTransactions(0, 0)).thenReturn(transactionSummaries);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY);
        when(currencyState.getCryptoCurrency()).thenReturn(CryptoCurrency.BTC);
        // Act
        TestObserver<List<Displayable>> testObserver = subject.fetchTransactions(itemAccount, 0, 0).test();
        // Assert
        verify(payloadManager).getAllTransactions(0, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void fetchTransactionsAccountTagImported() throws Exception {
        // Arrange
        Account account = new Account();
        TransactionSummary summary = new TransactionSummary();
        summary.setConfirmations(3);
        summary.setDirection(TransactionSummary.Direction.RECEIVED);
        summary.setFee(BigInteger.ONE);
        summary.setTotal(BigInteger.TEN);
        summary.setHash("hash");
        summary.setInputsMap(new HashMap<>());
        summary.setOutputsMap(new HashMap<>());
        summary.setTime(1000000L);
        List<TransactionSummary> transactionSummaries = Collections.singletonList(summary);
        when(payloadManager.getImportedAddressesTransactions(0, 0)).thenReturn(transactionSummaries);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_LEGACY);
        when(currencyState.getCryptoCurrency()).thenReturn(CryptoCurrency.BTC);
        // Act
        TestObserver<List<Displayable>> testObserver = subject.fetchTransactions(itemAccount, 0, 0).test();
        // Assert
        verify(payloadManager).getImportedAddressesTransactions(0, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void fetchTransactionsAccount() throws Exception {
        // Arrange
        Account account = new Account();
        String xPub = "xpub6CfLQa8fLgtp8E7tc1khAhrZYPm82okmugxP7TrhMPkPFKANhdCUd4TDJKUYLCxZskG2U7Q689CVBxs2EjJA7dyvjCzN5UYWwZbY2qVpymw";
        TransactionSummary summary = new TransactionSummary();
        summary.setConfirmations(3);
        summary.setDirection(TransactionSummary.Direction.RECEIVED);
        summary.setFee(BigInteger.ONE);
        summary.setTotal(BigInteger.TEN);
        summary.setHash("hash");
        summary.setInputsMap(new HashMap<>());
        summary.setOutputsMap(new HashMap<>());
        summary.setTime(1000000L);
        List<TransactionSummary> transactionSummaries = Collections.singletonList(summary);
        when(payloadManager.getAccountTransactions(xPub, 0, 0)).thenReturn(transactionSummaries);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.SINGLE_ACCOUNT);
        itemAccount.setAddress(xPub);
        when(currencyState.getCryptoCurrency()).thenReturn(CryptoCurrency.BTC);
        // Act
        TestObserver<List<Displayable>> testObserver =
                subject.fetchTransactions(itemAccount, 0, 0).test();
        // Assert
        verify(payloadManager).getAccountTransactions(xPub, 0, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void fetchTransactionsAccountNoXpub() throws Exception {
        // Arrange
        Account account = new Account();
        String xPub = "invalid xpub";
        TransactionSummary summary = new TransactionSummary();
        summary.setConfirmations(3);
        summary.setDirection(TransactionSummary.Direction.RECEIVED);
        summary.setFee(BigInteger.ONE);
        summary.setTotal(BigInteger.TEN);
        summary.setHash("hash");
        summary.setInputsMap(new HashMap<>());
        summary.setOutputsMap(new HashMap<>());
        summary.setTime(1000000L);
        List<TransactionSummary> transactionSummaries = Collections.singletonList(summary);
        when(payloadManager.getImportedAddressesTransactions( 0, 0)).thenReturn(transactionSummaries);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_LEGACY);
        itemAccount.setAddress(xPub);
        when(currencyState.getCryptoCurrency()).thenReturn(CryptoCurrency.BTC);
        // Act
        TestObserver<List<Displayable>> testObserver =
                subject.fetchTransactions(itemAccount, 0, 0).test();
        // Assert
        verify(payloadManager).getImportedAddressesTransactions(0, 0);
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void fetchTransactionsEthereum() throws Exception {
        // Arrange
        EthLatestBlock latestBlock = mock(EthLatestBlock.class);
        EthTransaction transaction = mock(EthTransaction.class);
        when(transaction.getHash()).thenReturn("hash");
        CombinedEthModel ethModel = mock(CombinedEthModel.class);
        when(ethDataManager.getLatestBlock()).thenReturn(Observable.just(latestBlock));
        when(ethDataManager.getEthTransactions()).thenReturn(Observable.just(transaction));
        when(ethDataManager.getEthResponseModel()).thenReturn(ethModel);
        when(currencyState.getCryptoCurrency()).thenReturn(CryptoCurrency.ETHER);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setType(ItemAccount.TYPE.SINGLE_ACCOUNT);
        when(currencyState.getCryptoCurrency()).thenReturn(CryptoCurrency.ETHER);
        // Act
        TestObserver<List<Displayable>> testObserver =
                subject.fetchTransactions(itemAccount, 0, 0).test();
        // Assert
        verify(ethDataManager).getLatestBlock();
        verify(ethDataManager).getEthTransactions();
        verify(ethDataManager).getEthResponseModel();
        testObserver.assertComplete();
        testObserver.assertNoErrors();
    }

    @Test
    public void getTransactionList() throws Exception {
        // Arrange

        // Act
        List<Displayable> value = subject.getTransactionList();
        // Assert
        assertEquals(transactionListStore.getList(), value);
        assertEquals(Collections.emptyList(), value);
    }

    @Test
    public void clearTransactionList() throws Exception {
        // Arrange
        transactionListStore.getList().add(mock(Displayable.class));
        // Act
        subject.clearTransactionList();
        // Assert
        assertEquals(Collections.emptyList(), subject.getTransactionList());
    }

    @Test
    public void insertTransactionIntoListAndReturnSorted() throws Exception {
        // Arrange
        Displayable tx0 = mock(BtcDisplayable.class);
        when(tx0.getTimeStamp()).thenReturn(0L);
        Displayable tx1 = mock(BtcDisplayable.class);
        when(tx1.getTimeStamp()).thenReturn(500L);
        Displayable tx2 = mock(BtcDisplayable.class);
        when(tx2.getTimeStamp()).thenReturn(1000L);
        transactionListStore.insertTransactions(Arrays.asList(tx1, tx0));
        // Act
        List<Displayable> value = subject.insertTransactionIntoListAndReturnSorted(tx2);
        // Assert
        assertNotNull(value);
        assertEquals(tx2, value.get(0));
        assertEquals(tx1, value.get(1));
        assertEquals(tx0, value.get(2));
    }

    @Test
    public void getBtcBalanceAccountTagAll() throws Exception {
        // Arrange
        Account account = new Account();
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getWalletBalance()).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY);
        // Act
        long value = subject.getBtcBalance(itemAccount);
        // Assert
        verify(payloadManager).getWalletBalance();
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBtcBalanceAccountTagImported() throws Exception {
        // Arrange
        Account account = new Account();
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getImportedAddressesBalance()).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_LEGACY);
        // Act
        long value = subject.getBtcBalance(itemAccount);
        // Assert
        verify(payloadManager).getImportedAddressesBalance();
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBtcBalanceAccount() throws Exception {
        // Arrange
        Account account = new Account();
        String xPub = "X_PUB";
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getAddressBalance(xPub)).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setAddress(xPub);
        // Act
        long value = subject.getBtcBalance(itemAccount);
        // Assert
        verify(payloadManager).getAddressBalance(xPub);
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBtcBalanceLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        String address = "ADDRESS";
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(payloadManager.getAddressBalance(address)).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(legacyAddress);
        itemAccount.setAddress(address);
        // Act
        long value = subject.getBtcBalance(itemAccount);
        // Assert
        verify(payloadManager).getAddressBalance(address);
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBchBalanceAccountTagAll() throws Exception {
        // Arrange
        Account account = new Account();
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(bchDataManager.getWalletBalance()).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_ACCOUNTS_AND_LEGACY);
        // Act
        long value = subject.getBchBalance(itemAccount);
        // Assert
        verify(bchDataManager).getWalletBalance();
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBchBalanceAccountTagImported() throws Exception {
        // Arrange
        Account account = new Account();
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(bchDataManager.getImportedAddressBalance()).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setType(ItemAccount.TYPE.ALL_LEGACY);
        // Act
        long value = subject.getBchBalance(itemAccount);
        // Assert
        verify(bchDataManager).getImportedAddressBalance();
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBchBalanceAccount() throws Exception {
        // Arrange
        Account account = new Account();
        String xPub = "X_PUB";
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(bchDataManager.getAddressBalance(xPub)).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(account);
        itemAccount.setAddress(xPub);
        // Act
        long value = subject.getBchBalance(itemAccount);
        // Assert
        verify(bchDataManager).getAddressBalance(xPub);
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getBchBalanceLegacyAddress() throws Exception {
        // Arrange
        LegacyAddress legacyAddress = new LegacyAddress();
        String address = "ADDRESS";
        BigInteger balance = BigInteger.valueOf(1_000_000_000_000L);
        when(bchDataManager.getAddressBalance(address)).thenReturn(balance);
        ItemAccount itemAccount = new ItemAccount();
        itemAccount.setAccountObject(legacyAddress);
        itemAccount.setAddress(address);
        // Act
        long value = subject.getBchBalance(itemAccount);
        // Assert
        verify(bchDataManager).getAddressBalance(address);
        assertEquals(1_000_000_000_000L, value);
    }

    @Test
    public void getTxFromHashFound() {
        // Arrange
        String txHash = "TX_HASH";
        Displayable tx0 = mock(BtcDisplayable.class);
        when(tx0.getHash()).thenReturn("");
        Displayable tx1 = mock(BtcDisplayable.class);
        when(tx1.getHash()).thenReturn("");
        Displayable tx2 = mock(BtcDisplayable.class);
        when(tx2.getHash()).thenReturn(txHash);
        transactionListStore.insertTransactions(Arrays.asList(tx0, tx1, tx2));
        // Act
        TestObserver<Displayable> testObserver = subject.getTxFromHash(txHash).test();
        // Assert
        testObserver.assertComplete();
        testObserver.assertNoErrors();
        testObserver.assertValue(tx2);
    }

    @Test
    public void getTxFromHashNotFound() {
        // Arrange
        String txHash = "TX_HASH";
        Displayable tx0 = mock(BtcDisplayable.class);
        when(tx0.getHash()).thenReturn("");
        Displayable tx1 = mock(BtcDisplayable.class);
        when(tx1.getHash()).thenReturn("");
        Displayable tx2 = mock(BtcDisplayable.class);
        when(tx2.getHash()).thenReturn("");
        transactionListStore.insertTransactions(Arrays.asList(tx0, tx1, tx2));
        // Act
        TestObserver<Displayable> testObserver = subject.getTxFromHash(txHash).test();
        // Assert
        testObserver.assertTerminated();
        testObserver.assertNoValues();
        testObserver.assertError(NoSuchElementException.class);
    }

    @Test
    public void getTxConfirmationsMap() throws Exception {
        // Arrange

        // Act
        HashMap<String, Integer> result = subject.getTxConfirmationsMap();
        // Assert
        assertNotNull(result);
    }

}
package piuk.blockchain.android.ui.transactions;

import android.annotation.SuppressLint;
import android.support.v4.util.Pair;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.transaction.Transaction;
import info.blockchain.wallet.transaction.Tx;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionHelperTest {

    @Mock PayloadManager mPayloadManager;
    @Mock MultiAddrFactory multiAddrFactory;
    private TransactionHelper mSubject;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        mSubject = new TransactionHelper(mPayloadManager, multiAddrFactory);
    }

    @Test
    public void addressToLabelLegacyAddresses() throws Exception {
        // Arrange
        HDWallet hdWallet = new HDWallet();
        hdWallet.getAccounts().add(new Account());
        hdWallet.getAccounts().add(new Account());
        Payload payload = new Payload();
        payload.setHdWallets(hdWallet);
        LegacyAddress address = new LegacyAddress();
        address.setAddress("addr");
        address.setLabel("label");
        ArrayList<LegacyAddress> legacyAddresses = new ArrayList<LegacyAddress>() {{
            add(address);
        }};
        payload.setLegacyAddressList(legacyAddresses);
        when(mPayloadManager.getPayload()).thenReturn(payload);
        // Act
        String value = mSubject.addressToLabel("addr");
        // Assert
        assertEquals("label", value);
    }

    @Test
    @Ignore
    // TODO: 21/10/2016 I broke this test. Needs fixing
    public void addressToLabelIsOwnHd() throws Exception {
        // Arrange
        HDWallet hdWallet = new HDWallet();
        Account account = new Account();
        account.setLabel("label");
        hdWallet.getAccounts().add(account);
        Payload payload = new Payload();
        payload.setHdWallets(hdWallet);
//        payload.getXpub2Account().put("value", 0);
        when(mPayloadManager.getPayload()).thenReturn(payload);
        when(multiAddrFactory.isOwnHDAddress(anyString())).thenReturn(true);
        HashMap<String, String> hashmap = new HashMap<>();
        hashmap.put("addr", "value");
        when(multiAddrFactory.getAddress2Xpub()).thenReturn(hashmap);
        // Act
        String value = mSubject.addressToLabel("addr");
        // Assert
        assertEquals("label", value);
    }

    @Test
    public void addressToLabelNotFound() throws Exception {
        // Arrange
        HDWallet hdWallet = new HDWallet();
        hdWallet.getAccounts().add(new Account());
        hdWallet.getAccounts().add(new Account());
        Payload payload = new Payload();
        payload.setHdWallets(hdWallet);
        when(mPayloadManager.getPayload()).thenReturn(payload);
        // Act
        String value = mSubject.addressToLabel("addr");
        // Assert
        assertEquals("addr", value);
    }

    @Test
    public void filterNonChangeAddressesSingleInput() throws Exception {
        // Arrange
        Transaction transaction = mock(Transaction.class);
        @SuppressLint("UseSparseArrays") Tx tx = new Tx("", "", "", 0D, 0L, new HashMap<>());
        tx.setDirection("RECEIVED");
        ArrayList<Transaction.xPut> inputs = new ArrayList<Transaction.xPut>() {{
            add(mock(Transaction.xPut.class));
        }};
        when(transaction.getInputs()).thenReturn(inputs);

        // Act
        Pair<HashMap<String, Long>, HashMap<String, Long>> value = mSubject.filterNonChangeAddresses(transaction, tx);
        // Assert
        assertEquals(1, value.first.size());
        assertEquals(0, value.second.size());
    }

    @Test
    public void filterNonChangeAddressesMultipleInput() throws Exception {
        // Arrange
        Transaction transaction = mock(Transaction.class);
        @SuppressLint("UseSparseArrays") Tx tx = new Tx("", "", "", 0D, 0L, new HashMap<>());
        tx.setDirection("SENT");
        Transaction.xPut xPut0 = mock(Transaction.xPut.class);
        xPut0.addr = "addr0";
        Transaction.xPut xPut1 = mock(Transaction.xPut.class);
        Transaction.xPut xPut2 = mock(Transaction.xPut.class);
        ArrayList<Transaction.xPut> inputs = new ArrayList<Transaction.xPut>() {{
            add(xPut0);
            add(xPut1);
            add(xPut2);
        }};
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("addr0", "xpub0");
        when(multiAddrFactory.getAddress2Xpub()).thenReturn(hashMap);
        when(transaction.getInputs()).thenReturn(inputs);

        // Act
        Pair<HashMap<String, Long>, HashMap<String, Long>> value = mSubject.filterNonChangeAddresses(transaction, tx);
        // Assert
        assertEquals(2, value.first.size());
        assertEquals(0, value.second.size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputSingleOutput() throws Exception {
        // Arrange
        Transaction transaction = mock(Transaction.class);
        @SuppressLint("UseSparseArrays") Tx tx = new Tx("", "", "", 0D, 0L, new HashMap<>());
        tx.setDirection("SENT");
        ArrayList<Transaction.xPut> inputs = new ArrayList<Transaction.xPut>() {{
            add(mock(Transaction.xPut.class));
        }};
        when(transaction.getInputs()).thenReturn(inputs);
        when(transaction.getOutputs()).thenReturn(inputs);

        Payload payload = new Payload();
        ArrayList<LegacyAddress> legacyAddresses = new ArrayList<>();
        payload.setLegacyAddressList(legacyAddresses);
        when(mPayloadManager.getPayload()).thenReturn(payload);
        // Act
        Pair<HashMap<String, Long>, HashMap<String, Long>> value = mSubject.filterNonChangeAddresses(transaction, tx);
        // Assert
        assertEquals(1, value.first.size());
        assertEquals(1, value.second.size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputMultipleOutput() throws Exception {
        // Arrange
        Transaction transaction = mock(Transaction.class);
        @SuppressLint("UseSparseArrays") Tx tx = new Tx("", "", "", 0D, 0L, new HashMap<>());
        tx.setDirection("SENT");
        Transaction.xPut xPut0 = mock(Transaction.xPut.class);
        xPut0.addr = "addr0";
        xPut0.value = 1L;
        Transaction.xPut xPut1 = mock(Transaction.xPut.class);
        xPut1.addr = "addr1";
        xPut1.value = 1L;
        Transaction.xPut xPut2 = mock(Transaction.xPut.class);
        xPut2.addr = "addr2";
        ArrayList<Transaction.xPut> inputs = new ArrayList<Transaction.xPut>() {{
            add(xPut0);
        }};
        ArrayList<Transaction.xPut> outputs = new ArrayList<Transaction.xPut>() {{
            add(xPut0);
            add(xPut1);
            add(xPut2);
        }};
        when(transaction.getInputs()).thenReturn(inputs);
        when(transaction.getOutputs()).thenReturn(outputs);

        Payload mockPayload = mock(Payload.class);
        List<String> legacyStrings = new ArrayList<String>() {{
            add("addr0");
            add("addr1");
        }};
        List<String> watchOnlyStrings = new ArrayList<String>() {{
            add("addr2");
        }};
        when(mockPayload.getLegacyAddressStringList()).thenReturn(legacyStrings);
        when(mockPayload.getWatchOnlyAddressStringList()).thenReturn(watchOnlyStrings);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        Pair<HashMap<String, Long>, HashMap<String, Long>> value = mSubject.filterNonChangeAddresses(transaction, tx);
        // Assert
        assertEquals(1, value.first.size());
        assertEquals(1, value.second.size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputMultipleOutputHD() throws Exception {
        // Arrange
        Transaction transaction = mock(Transaction.class);
        @SuppressLint("UseSparseArrays") Tx tx = new Tx("", "", "", 0D, 0L, new HashMap<>());
        tx.setDirection("SENT");
        Transaction.xPut xPut0 = mock(Transaction.xPut.class);
        xPut0.addr = "addr0";
        xPut0.value = 1L;
        Transaction.xPut xPut1 = mock(Transaction.xPut.class);
        xPut1.addr = "addr0";
        xPut1.value = 1L;
        Transaction.xPut xPut2 = mock(Transaction.xPut.class);
        xPut2.addr = "addr0";
        ArrayList<Transaction.xPut> inputs = new ArrayList<Transaction.xPut>() {{
            add(xPut0);
        }};
        ArrayList<Transaction.xPut> outputs = new ArrayList<Transaction.xPut>() {{
            add(xPut0);
            add(xPut1);
            add(xPut2);
        }};
        when(transaction.getInputs()).thenReturn(inputs);
        when(transaction.getOutputs()).thenReturn(outputs);

        Payload mockPayload = mock(Payload.class);
        List<String> legacyStrings = new ArrayList<String>() {{
            add("addr0");
            add("addr1");
        }};
        List<String> watchOnlyStrings = new ArrayList<String>() {{
            add("addr2");
        }};
        when(mockPayload.getLegacyAddressStringList()).thenReturn(legacyStrings);
        when(mockPayload.getWatchOnlyAddressStringList()).thenReturn(watchOnlyStrings);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(multiAddrFactory.isOwnHDAddress(anyString())).thenReturn(true);
        // Act
        Pair<HashMap<String, Long>, HashMap<String, Long>> value = mSubject.filterNonChangeAddresses(transaction, tx);
        // Assert
        assertEquals(1, value.first.size());
        assertEquals(1, value.second.size());
    }
}
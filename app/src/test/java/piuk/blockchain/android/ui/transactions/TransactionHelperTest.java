package piuk.blockchain.android.ui.transactions;

import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.payload.data.Wallet;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import piuk.blockchain.androidcore.data.bitcoincash.BchDataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.data.transactions.models.BtcDisplayable;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TransactionHelperTest {

    @Mock private PayloadDataManager payloadDataManager;
    @Mock private BchDataManager bchDataManager;
    private TransactionHelper subject;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        subject = new TransactionHelper(payloadDataManager, bchDataManager);
    }

    @Test
    public void filterNonChangeAddressesSingleInput() throws Exception {
        // Arrange
        TransactionSummary transaction = new TransactionSummary();
        transaction.setDirection(TransactionSummary.Direction.RECEIVED);
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put("key", new BigInteger("1"));
        transaction.setInputsMap(inputs);
        // Act
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> value =
                subject.filterNonChangeAddresses(new BtcDisplayable(transaction));
        // Assert
        assertEquals(1, value.getLeft().size());
        assertEquals(0, value.getRight().size());
    }

    @Test
    public void filterNonChangeReceivedAddressesMultipleInput() throws Exception {
        // Arrange
        TransactionSummary transaction = new TransactionSummary();
        transaction.setDirection(TransactionSummary.Direction.RECEIVED);
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put("key0", new BigInteger("1"));
        inputs.put("key1", new BigInteger("1"));
        transaction.setInputsMap(inputs);
        // Act
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> value =
                subject.filterNonChangeAddresses(new BtcDisplayable(transaction));
        // Assert
        assertEquals(1, value.getLeft().size());
        assertEquals(0, value.getRight().size());
    }

    @Test
    public void filterNonChangeAddressesMultipleInput() throws Exception {
        // Arrange
        TransactionSummary transaction = new TransactionSummary();
        transaction.setDirection(TransactionSummary.Direction.SENT);
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put("key0", new BigInteger("1"));
        inputs.put("key1", new BigInteger("1"));
        inputs.put("key2", new BigInteger("1"));
        transaction.setInputsMap(inputs);
        when(payloadDataManager.getXpubFromAddress("key0")).thenReturn("xpub");
        when(payloadDataManager.getXpubFromAddress("key1")).thenReturn("xpub");
        // Act
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> value =
                subject.filterNonChangeAddresses(new BtcDisplayable(transaction));
        // Assert
        assertEquals(2, value.getLeft().size());
        assertEquals(0, value.getRight().size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputSingleOutput() throws Exception {
        // Arrange
        TransactionSummary transaction = new TransactionSummary();
        transaction.setDirection(TransactionSummary.Direction.SENT);
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put("key", new BigInteger("1"));
        transaction.setInputsMap(inputs);
        transaction.setOutputsMap(inputs);

        Wallet payload = mock(Wallet.class);
        when(payload.getLegacyAddressStringList()).thenReturn(Collections.emptyList());
        when(payloadDataManager.getWallet()).thenReturn(payload);
        // Act
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> value =
                subject.filterNonChangeAddresses(new BtcDisplayable(transaction));
        // Assert
        assertEquals(1, value.getLeft().size());
        assertEquals(1, value.getRight().size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputMultipleOutput() throws Exception {
        // Arrange
        TransactionSummary transaction = new TransactionSummary();
        transaction.setTotal(BigInteger.TEN);
        transaction.setDirection(TransactionSummary.Direction.SENT);
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put("key0", new BigInteger("1"));
        transaction.setInputsMap(inputs);
        HashMap<String, BigInteger> outputs = new HashMap<>();
        inputs.put("key0", new BigInteger("1"));
        outputs.put("key1", new BigInteger("1"));
        outputs.put("key2", new BigInteger("15"));
        transaction.setOutputsMap(outputs);

        Wallet payload = mock(Wallet.class);
        when(payload.getLegacyAddressStringList()).thenReturn(Collections.emptyList());
        when(payloadDataManager.getWallet()).thenReturn(payload);

        Wallet mockPayload = mock(Wallet.class);
        List<String> legacyStrings = Arrays.asList("key0", "key1");
        List<String> watchOnlyStrings = Collections.singletonList("key2");
        when(mockPayload.getLegacyAddressStringList()).thenReturn(legacyStrings);
        when(mockPayload.getWatchOnlyAddressStringList()).thenReturn(watchOnlyStrings);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        // Act
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> value =
                subject.filterNonChangeAddresses(new BtcDisplayable(transaction));
        // Assert
        assertEquals(1, value.getLeft().size());
        assertEquals(1, value.getRight().size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputSingleOutputHD() throws Exception {
        // Arrange
        TransactionSummary transaction = new TransactionSummary();
        transaction.setTotal(BigInteger.TEN);
        transaction.setDirection(TransactionSummary.Direction.SENT);
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put("key0", new BigInteger("1"));
        transaction.setInputsMap(inputs);
        HashMap<String, BigInteger> outputs = new HashMap<>();
        outputs.put("key0", new BigInteger("1"));
        transaction.setOutputsMap(outputs);

        Wallet mockPayload = mock(Wallet.class);
        List<String> legacyStrings = Arrays.asList("key0", "key1");
        List<String> watchOnlyStrings = Collections.singletonList("key2");
        when(mockPayload.getLegacyAddressStringList()).thenReturn(legacyStrings);
        when(mockPayload.getWatchOnlyAddressStringList()).thenReturn(watchOnlyStrings);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(payloadDataManager.isOwnHDAddress(anyString())).thenReturn(true);
        // Act
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> value =
                subject.filterNonChangeAddresses(new BtcDisplayable(transaction));
        // Assert
        assertEquals(1, value.getLeft().size());
        assertEquals(1, value.getRight().size());
    }

    @Test
    public void filterNonChangeAddressesMultipleInputBch() throws Exception {
        // Arrange
        TransactionSummary transaction = new TransactionSummary();
        transaction.setDirection(TransactionSummary.Direction.SENT);
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put("key0", new BigInteger("1"));
        inputs.put("key1", new BigInteger("1"));
        inputs.put("key2", new BigInteger("1"));
        transaction.setInputsMap(inputs);
        when(bchDataManager.getXpubFromAddress("key0")).thenReturn("xpub");
        when(bchDataManager.getXpubFromAddress("key1")).thenReturn("xpub");
        // Act
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> value =
                subject.filterNonChangeAddressesBch(new BtcDisplayable(transaction));
        // Assert
        assertEquals(2, value.getLeft().size());
        assertEquals(0, value.getRight().size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputSingleOutputBch() throws Exception {
        // Arrange
        TransactionSummary transaction = new TransactionSummary();
        transaction.setDirection(TransactionSummary.Direction.SENT);
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put("key", new BigInteger("1"));
        transaction.setInputsMap(inputs);
        transaction.setOutputsMap(inputs);

        when(bchDataManager.getLegacyAddressStringList()).thenReturn(Collections.emptyList());
        // Act
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> value =
                subject.filterNonChangeAddressesBch(new BtcDisplayable(transaction));
        // Assert
        assertEquals(1, value.getLeft().size());
        assertEquals(1, value.getRight().size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputMultipleOutputBch() throws Exception {
        // Arrange
        TransactionSummary transaction = new TransactionSummary();
        transaction.setTotal(BigInteger.TEN);
        transaction.setDirection(TransactionSummary.Direction.SENT);
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put("key0", new BigInteger("1"));
        transaction.setInputsMap(inputs);
        HashMap<String, BigInteger> outputs = new HashMap<>();
        inputs.put("key0", new BigInteger("1"));
        outputs.put("key1", new BigInteger("1"));
        outputs.put("key2", new BigInteger("15"));
        transaction.setOutputsMap(outputs);

        List<String> legacyStrings = Arrays.asList("key0", "key1");
        List<String> watchOnlyStrings = Collections.singletonList("key2");
        when(bchDataManager.getLegacyAddressStringList()).thenReturn(legacyStrings);
        when(bchDataManager.getWatchOnlyAddressStringList()).thenReturn(watchOnlyStrings);
        // Act
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> value =
                subject.filterNonChangeAddressesBch(new BtcDisplayable(transaction));
        // Assert
        assertEquals(1, value.getLeft().size());
        assertEquals(1, value.getRight().size());
    }

    @Test
    public void filterNonChangeAddressesSingleInputSingleOutputHDBch() throws Exception {
        // Arrange
        TransactionSummary transaction = new TransactionSummary();
        transaction.setTotal(BigInteger.TEN);
        transaction.setDirection(TransactionSummary.Direction.SENT);
        HashMap<String, BigInteger> inputs = new HashMap<>();
        inputs.put("key0", new BigInteger("1"));
        transaction.setInputsMap(inputs);
        HashMap<String, BigInteger> outputs = new HashMap<>();
        outputs.put("key0", new BigInteger("1"));
        transaction.setOutputsMap(outputs);

        List<String> legacyStrings = Arrays.asList("key0", "key1");
        List<String> watchOnlyStrings = Collections.singletonList("key2");
        when(bchDataManager.getLegacyAddressStringList()).thenReturn(legacyStrings);
        when(bchDataManager.getWatchOnlyAddressStringList()).thenReturn(watchOnlyStrings);
        when(bchDataManager.isOwnAddress(anyString())).thenReturn(true);
        // Act
        Pair<HashMap<String, BigInteger>, HashMap<String, BigInteger>> value =
                subject.filterNonChangeAddressesBch(new BtcDisplayable(transaction));
        // Assert
        assertEquals(1, value.getLeft().size());
        assertEquals(1, value.getRight().size());
    }
}
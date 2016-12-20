package piuk.blockchain.android.ui.swipetoreceive;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.util.PrefsUtil;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.KEY_SWIPE_RECEIVE_ACCOUNT_NAME;
import static piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper.KEY_SWIPE_RECEIVE_ADDRESSES;

public class SwipeToReceiveHelperTest extends RxTest {

    private SwipeToReceiveHelper subject;
    @Mock PayloadManager payloadManager;
    @Mock MultiAddrFactory multiAddrFactory;
    @Mock PrefsUtil prefsUtil;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new SwipeToReceiveHelper(payloadManager, multiAddrFactory, prefsUtil);
    }

    @Test
    public void updateAndStoreAddresses() throws Exception {
        // Arrange
        when(prefsUtil.getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true)).thenReturn(true);
        Payload mockPayload = mock(Payload.class);
        HDWallet mockHdWallet = mock(HDWallet.class);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(mockHdWallet.getDefaultIndex()).thenReturn(0);
        List<Account> mockList = mock(List.class);
        when(mockHdWallet.getAccounts()).thenReturn(mockList);
        Account mockAccount = mock(Account.class);
        when(mockList.get(anyInt())).thenReturn(mockAccount);
        when(mockAccount.getLabel()).thenReturn("Account");
        when(payloadManager.getReceiveAddressAtPosition(anyInt(), anyInt())).thenReturn("address");
        // Act
        subject.updateAndStoreAddresses();
        // Assert
        verify(prefsUtil).getValue(PrefsUtil.KEY_SWIPE_TO_RECEIVE_ENABLED, true);
        verify(payloadManager, times(5)).getReceiveAddressAtPosition(anyInt(), anyInt());
        verify(prefsUtil).setValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, "Account");
        verify(prefsUtil).setValue(KEY_SWIPE_RECEIVE_ADDRESSES, "address,address,address,address,address,");
    }

    @Test
    public void getNextAvailableAddressValid() throws Exception {
        // Arrange
        LinkedHashMap<String, Long> map = new LinkedHashMap<>();
        map.put("addr0", 1000L);
        map.put("addr1", 5L);
        map.put("addr2", -10L);
        map.put("addr3", 0L);
        map.put("addr4", 0L);
        when(multiAddrFactory.getAddressBalanceFromApi(anyListOf(String.class))).thenReturn(map);
        when(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "")).thenReturn("addr0, addr1, addr2, addr3, addr4");
        // Act
        TestObserver<String> test = subject.getNextAvailableAddress().test();
        // Assert
        test.assertComplete();
        test.assertNoErrors();
        assertEquals("addr3", test.values().get(0));
    }

    @Test
    public void getNextAvailableAddressAllUsed() throws Exception {
        // Arrange
        LinkedHashMap<String, Long> map = new LinkedHashMap<>();
        map.put("addr0", 1000L);
        map.put("addr1", 5L);
        map.put("addr2", -10L);
        map.put("addr3", 1L);
        map.put("addr4", 1_000_000_000_000L);
        when(multiAddrFactory.getAddressBalanceFromApi(anyListOf(String.class))).thenReturn(map);
        when(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "")).thenReturn("addr0, addr1, addr2, addr3, addr4");
        // Act
        TestObserver<String> test = subject.getNextAvailableAddress().test();
        // Assert
        test.assertComplete();
        test.assertNoErrors();
        assertEquals("", test.values().get(0));
    }

    @Test
    public void getReceiveAddresses() throws Exception {
        // Arrange
        when(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "")).thenReturn("addr0, addr1, addr2, addr3, addr4");
        // Act
        List<String> result = subject.getReceiveAddresses();
        // Assert
        assertEquals(5, result.size());
    }

    @Test
    public void getReceiveAddressesEmptyList() throws Exception {
        // Arrange
        when(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ADDRESSES, "")).thenReturn("");
        // Act
        List<String> result = subject.getReceiveAddresses();
        // Assert
        assertEquals(Collections.emptyList(), result);
    }

    @Test
    public void getAccountName() throws Exception {
        // Arrange
        when(prefsUtil.getValue(KEY_SWIPE_RECEIVE_ACCOUNT_NAME, "")).thenReturn("Account");
        // Act
        String result = subject.getAccountName();
        // Assert
        assertEquals("Account", result);
    }

}
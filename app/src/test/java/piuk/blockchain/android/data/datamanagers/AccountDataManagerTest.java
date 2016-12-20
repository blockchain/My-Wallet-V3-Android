package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.services.AddressInfoService;

import static junit.framework.TestCase.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AccountDataManagerTest extends RxTest {

    private AccountDataManager subject;
    @Mock PayloadManager payloadManager;
    @Mock MultiAddrFactory multiAddrFactory;
    @Mock AddressInfoService addressInfoService;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        subject = new AccountDataManager(payloadManager, multiAddrFactory, addressInfoService);
    }

    @Test
    public void createNewAccountSuccess() throws Exception {
        // Arrange
        Account mockAccount = mock(Account.class);
        when(payloadManager.addAccount(anyString(), anyString())).thenReturn(mockAccount);
        // Act
        TestObserver<Account> observer = subject.createNewAccount("", null).test();
        // Assert
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(mockAccount, observer.values().get(0));
    }

    @Test
    public void createNewAccountDecryptionFailure() throws Exception {
        // Arrange
        when(payloadManager.addAccount(anyString(), anyString())).thenThrow(new DecryptionException());
        // Act
        TestObserver<Account> observer = subject.createNewAccount("", new CharSequenceX("password")).test();
        // Assert
        observer.assertError(DecryptionException.class);
        observer.assertNotComplete();
        observer.assertNoValues();
    }

    @Test
    public void createNewAccountFatalException() throws Exception {
        // Arrange
        when(payloadManager.addAccount(anyString(), anyString())).thenThrow(new Exception());
        // Act
        TestObserver<Account> observer = subject.createNewAccount("", new CharSequenceX("password")).test();
        // Assert
        observer.assertError(Exception.class);
        observer.assertNotComplete();
        observer.assertNoValues();
    }

    @Test
    public void createNewAccountThrowsException() throws Exception {
        // Arrange
        doThrow(new Exception())
                .when(payloadManager).addAccount(
                anyString(), anyString());
        // Act
        TestObserver<Account> observer = subject.createNewAccount("", new CharSequenceX("password")).test();
        // Assert
        observer.assertError(Exception.class);
        observer.assertNotComplete();
        observer.assertNoValues();
    }

    @Test
    public void setPrivateKeySuccessNoDoubleEncryption() throws Exception {
        // Arrange
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStringList().indexOf(any())).thenReturn(0);
        when(mockPayload.isDoubleEncrypted()).thenReturn(false);
        when(mockPayload.getLegacyAddressList().get(anyInt())).thenReturn(mock(LegacyAddress.class));
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(payloadManager.savePayloadToServer()).thenReturn(true);
        ECKey mockECKey = mock(ECKey.class);
        when(mockECKey.toAddress(any(NetworkParameters.class))).thenReturn(mock(Address.class));
        // Act
        TestObserver<Boolean> observer = subject.setPrivateKey(mockECKey, null).test();
        // Assert
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(true, observer.values().get(0).booleanValue());
    }

    @Test
    public void setPrivateKeySuccessWithDoubleEncryption() throws Exception {
        // Arrange
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStringList().indexOf(any())).thenReturn(0);
        when(mockPayload.isDoubleEncrypted()).thenReturn(true);
        when(mockPayload.getLegacyAddressList().get(anyInt())).thenReturn(mock(LegacyAddress.class));
        when(mockPayload.getSharedKey()).thenReturn("shared key");
        when(mockPayload.getOptions().getIterations()).thenReturn(10);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(payloadManager.savePayloadToServer()).thenReturn(true);
        ECKey mockECKey = mock(ECKey.class);
        when(mockECKey.toAddress(any(NetworkParameters.class))).thenReturn(mock(Address.class));
        when(mockECKey.getPrivKeyBytes()).thenReturn(new byte[0]);
        // Act
        TestObserver<Boolean> observer = subject.setPrivateKey(mockECKey, new CharSequenceX("password")).test();
        // Assert
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(true, observer.values().get(0).booleanValue());
    }

    @Test
    public void updateLegacyAddressSuccess() throws Exception {
        // Arrange
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getLegacyAddressStringList()).thenReturn(new ArrayList<>());
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(payloadManager.addLegacyAddress(mockLegacyAddress)).thenReturn(true);
        when(addressInfoService.getAddressBalance(any(LegacyAddress.class), anyString())).thenReturn(Observable.just(0L));
        // Act
        TestObserver<Boolean> observer = subject.updateLegacyAddress(mockLegacyAddress).test();
        // Assert
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(true, observer.values().get(0).booleanValue());
    }

    @Test
    public void updateLegacyAddressFailure() throws Exception {
        // Arrange
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getLegacyAddressStringList()).thenReturn(new ArrayList<>());
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(payloadManager.addLegacyAddress(mockLegacyAddress)).thenReturn(false);
        // Act
        TestObserver<Boolean> observer = subject.updateLegacyAddress(mockLegacyAddress).test();
        // Assert
        observer.assertNoErrors();
        observer.assertComplete();
        assertEquals(false, observer.values().get(0).booleanValue());
    }

    @Test
    public void updateLegacyAddressSuccessThrowsException() throws Exception {
        // Arrange
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getLegacyAddressStringList()).thenReturn(new ArrayList<>());
        when(payloadManager.addLegacyAddress(mockLegacyAddress)).thenReturn(true);
        // Act
        TestObserver<Boolean> observer = subject.updateLegacyAddress(mockLegacyAddress).test();
        // Assert
        observer.assertError(Throwable.class);
        observer.assertNotComplete();
        observer.assertNoValues();
    }

}
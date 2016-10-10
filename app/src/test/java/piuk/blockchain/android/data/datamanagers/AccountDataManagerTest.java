package piuk.blockchain.android.data.datamanagers;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.PayloadException;
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

import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.services.AddressInfoService;
import rx.Observable;
import rx.observers.TestSubscriber;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doAnswer;
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
        TestSubscriber<Account> subscriber = new TestSubscriber<>();
        Account mockAccount = mock(Account.class);
        doAnswer(invocation -> {
            ((PayloadManager.AccountAddListener) invocation.getArguments()[2]).onAccountAddSuccess(mockAccount);
            return null;
        }).when(payloadManager).addAccount(
                anyString(), anyString(), any(PayloadManager.AccountAddListener.class));
        // Act
        subject.createNewAccount("", null).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        assertEquals(mockAccount, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void createNewAccountDecryptionFailure() throws Exception {
        // Arrange
        TestSubscriber<Account> subscriber = new TestSubscriber<>();
        doAnswer(invocation -> {
            ((PayloadManager.AccountAddListener) invocation.getArguments()[2]).onSecondPasswordFail();
            return null;
        }).when(payloadManager).addAccount(
                anyString(), anyString(), any(PayloadManager.AccountAddListener.class));
        // Act
        subject.createNewAccount("", new CharSequenceX("password")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertError(DecryptionException.class);
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void createNewAccountPayloadException() throws Exception {
        // Arrange
        TestSubscriber<Account> subscriber = new TestSubscriber<>();
        doAnswer(invocation -> {
            ((PayloadManager.AccountAddListener) invocation.getArguments()[2]).onPayloadSaveFail();
            return null;
        }).when(payloadManager).addAccount(
                anyString(), anyString(), any(PayloadManager.AccountAddListener.class));
        // Act
        subject.createNewAccount("", new CharSequenceX("password")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertError(PayloadException.class);
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void createNewAccountThrowsException() throws Exception {
        // Arrange
        TestSubscriber<Account> subscriber = new TestSubscriber<>();
        doThrow(new Exception())
                .when(payloadManager).addAccount(
                anyString(), anyString(), any(PayloadManager.AccountAddListener.class));
        // Act
        subject.createNewAccount("", new CharSequenceX("password")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertError(Exception.class);
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
    }

    @Test
    public void setPrivateKeySuccessNoDoubleEncryption() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStrings().indexOf(any())).thenReturn(0);
        when(mockPayload.isDoubleEncrypted()).thenReturn(false);
        when(mockPayload.getLegacyAddresses().get(anyInt())).thenReturn(mock(LegacyAddress.class));
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(payloadManager.savePayloadToServer()).thenReturn(true);
        ECKey mockECKey = mock(ECKey.class);
        when(mockECKey.toAddress(any(NetworkParameters.class))).thenReturn(mock(Address.class));
        // Act
        subject.setPrivateKey(mockECKey, null).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void setPrivateKeySuccessWithDoubleEncryption() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStrings().indexOf(any())).thenReturn(0);
        when(mockPayload.isDoubleEncrypted()).thenReturn(true);
        when(mockPayload.getLegacyAddresses().get(anyInt())).thenReturn(mock(LegacyAddress.class));
        when(mockPayload.getSharedKey()).thenReturn("shared key");
        when(mockPayload.getOptions().getIterations()).thenReturn(10);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(payloadManager.savePayloadToServer()).thenReturn(true);
        ECKey mockECKey = mock(ECKey.class);
        when(mockECKey.toAddress(any(NetworkParameters.class))).thenReturn(mock(Address.class));
        when(mockECKey.getPrivKeyBytes()).thenReturn(new byte[0]);
        // Act
        subject.setPrivateKey(mockECKey, new CharSequenceX("password")).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updateLegacyAddressSuccess() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getLegacyAddressStrings()).thenReturn(new ArrayList<>());
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(payloadManager.addLegacyAddress(mockLegacyAddress)).thenReturn(true);
        when(addressInfoService.getAddressBalance(any(LegacyAddress.class), anyString())).thenReturn(Observable.just(0L));
        // Act
        subject.updateLegacyAddress(mockLegacyAddress).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        assertEquals(true, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updateLegacyAddressFailure() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getLegacyAddressStrings()).thenReturn(new ArrayList<>());
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(payloadManager.addLegacyAddress(mockLegacyAddress)).thenReturn(false);
        // Act
        subject.updateLegacyAddress(mockLegacyAddress).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertNoErrors();
        subscriber.assertCompleted();
        assertEquals(false, subscriber.getOnNextEvents().get(0));
    }

    @Test
    public void updateLegacyAddressSuccessThrowsException() throws Exception {
        // Arrange
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        LegacyAddress mockLegacyAddress = mock(LegacyAddress.class);
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.getLegacyAddressStrings()).thenReturn(new ArrayList<>());
        when(payloadManager.addLegacyAddress(mockLegacyAddress)).thenReturn(true);
        // Act
        subject.updateLegacyAddress(mockLegacyAddress).toBlocking().subscribe(subscriber);
        // Assert
        subscriber.assertError(Throwable.class);
        subscriber.assertNotCompleted();
        subscriber.assertNoValues();
    }

}
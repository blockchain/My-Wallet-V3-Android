package piuk.blockchain.android.ui.account;

import android.app.Application;
import android.content.Intent;

import info.blockchain.wallet.exceptions.DecryptionException;
import info.blockchain.wallet.exceptions.PayloadException;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.util.CharSequenceX;

import org.apache.commons.lang3.tuple.Triple;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.datamanagers.AccountDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.PrefsUtil;
import io.reactivex.Observable;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.account.AccountViewModel.KEY_WARN_TRANSFER_ALL;

@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class AccountViewModelTest {

    private AccountViewModel subject;
    @Mock AccountViewModel.DataListener activity;
    @Mock PayloadManager payloadManager;
    @Mock AccountDataManager accountDataManager;
    @Mock TransferFundsDataManager fundsDataManager;
    @Mock PrefsUtil prefsUtil;
    @Mock AppUtil appUtil;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new MockDataManagerModule());

        subject = new AccountViewModel(activity);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange

        // Act
        subject.onViewReady();
        // Assert
        assertTrue(true);
    }

    @Test
    public void setDoubleEncryptionPassword() throws Exception {
        // Arrange
        CharSequenceX password = new CharSequenceX("password");
        // Act
        subject.setDoubleEncryptionPassword(password);
        // Assert
        assertEquals(password, subject.doubleEncryptionPassword);
    }

    @Test
    public void checkTransferableLegacyFundsWarnTransferAllTrue() throws Exception {
        // Arrange
        Triple triple = Triple.of(new ArrayList<PendingTransaction>() {{
            add(new PendingTransaction());
        }}, 1L, 2L);
        when(fundsDataManager.getTransferableFundTransactionListForDefaultAccount()).thenReturn(Observable.just(triple));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(prefsUtil.getValue(KEY_WARN_TRANSFER_ALL, true)).thenReturn(true);
        // Act
        subject.checkTransferableLegacyFunds(false);
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(true);
        verify(activity).onShowTransferableLegacyFundsWarning(false);
        verify(activity).dismissProgressDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void checkTransferableLegacyFundsNoFundsAvailable() throws Exception {
        // Arrange
        Triple triple = Triple.of(new ArrayList<PendingTransaction>(), 1L, 2L);
        when(fundsDataManager.getTransferableFundTransactionListForDefaultAccount()).thenReturn(Observable.just(triple));
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.checkTransferableLegacyFunds(true);
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(false);
        verify(activity).dismissProgressDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void checkTransferableLegacyFundsThrowsException() throws Exception {
        // Arrange
        when(fundsDataManager.getTransferableFundTransactionListForDefaultAccount()).thenReturn(Observable.error(new Throwable()));
        // Act
        subject.checkTransferableLegacyFunds(true);
        // Assert
        verify(activity).onSetTransferLegacyFundsMenuItemVisible(false);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void createNewAccountSuccessful() throws Exception {
        // Arrange
        when(accountDataManager.createNewAccount(anyString(), any(CharSequenceX.class))).thenReturn(Observable.just(new Account()));
        // Act
        subject.createNewAccount("");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(activity).broadcastIntent(any(Intent.class));
        verify(activity).onUpdateAccountsList();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void createNewAccountDecryptionException() throws Exception {
        // Arrange
        when(accountDataManager.createNewAccount(anyString(), any(CharSequenceX.class))).thenReturn(Observable.error(new DecryptionException()));
        // Act
        subject.createNewAccount("");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void createNewAccountPayloadException() throws Exception {
        // Arrange
        when(accountDataManager.createNewAccount(anyString(), any(CharSequenceX.class))).thenReturn(Observable.error(new PayloadException()));
        // Act
        subject.createNewAccount("");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void createNewAccountUnknownException() throws Exception {
        // Arrange
        when(accountDataManager.createNewAccount(anyString(), any(CharSequenceX.class))).thenReturn(Observable.error(new Exception()));
        // Act
        subject.createNewAccount("");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateLegacyAddressSuccessful() throws Exception {
        // Arrange
        when(accountDataManager.updateLegacyAddress(any(LegacyAddress.class))).thenReturn(Observable.just(true));
        // Act
        subject.updateLegacyAddress(new LegacyAddress());
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(activity).broadcastIntent(any(Intent.class));
        verify(activity).onUpdateAccountsList();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void updateLegacyAddressFailed() throws Exception {
        // Arrange
        when(accountDataManager.updateLegacyAddress(any(LegacyAddress.class))).thenReturn(Observable.just(false));
        // Act
        subject.updateLegacyAddress(new LegacyAddress());
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onScanButtonClickedCameraInUse() throws Exception {
        // Arrange
        when(appUtil.isCameraOpen()).thenReturn(true);
        // Act
        subject.onScanButtonClicked();
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onScanButtonClickedCameraAvailable() throws Exception {
        // Arrange
        when(appUtil.isCameraOpen()).thenReturn(false);
        // Act
        subject.onScanButtonClicked();
        // Assert
        //noinspection WrongConstant
        verify(activity).startScanForResult();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void importBip38AddressWithValidPassword() throws Exception {
        // Arrange

        // Act
        subject.importBip38Address("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS", new CharSequenceX("password"));
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
    }

    @Test
    public void importBip38AddressWithIncorrectPassword() throws Exception {
        // Arrange

        // Act
        subject.importBip38Address("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS", new CharSequenceX("notthepassword"));
        // Assert
        verify(activity).showProgressDialog(anyInt());
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(activity).dismissProgressDialog();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onAddressScannedBip38() throws Exception {
        // Arrange

        // Act
        subject.onAddressScanned("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS");
        // Assert
        verify(activity).showBip38PasswordDialog("6PRJmkckxBct8jUwn6UcJbickdrnXBiPP9JkNW83g4VyFNsfEuxas39pSS");
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onAddressScannedNonBip38() throws Exception {
        // Arrange

        // Act
        subject.onAddressScanned("L1FQxC7wmmRNNe2YFPNXscPq3kaheiA4T7SnTr7vYSBW7Jw1A7PD");
        // Assert
        verify(activity).showProgressDialog(anyInt());
        verify(activity).dismissProgressDialog();
    }

    @Test
    public void onAddressScannedWatchOnlyInvalidAddress() throws Exception {
        // Arrange

        // Act
        subject.onAddressScanned("test");
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onAddressScannedWatchOnlyNullAddress() throws Exception {
        // Arrange

        // Act
        subject.onAddressScanned(null);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onAddressScannedWatchAddressAlreadyInWallet() throws Exception {
        // Arrange
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStringList().contains(any())).thenReturn(true);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.onAddressScanned("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7");
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onAddressScannedWatchAddressNotInWallet() throws Exception {
        // Arrange
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStringList().contains(any())).thenReturn(false);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.onAddressScanned("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7");
        // Assert
        //noinspection WrongConstant
        verify(activity).showWatchOnlyWarningDialog("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7");
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void confirmImportWatchOnly() throws Exception {
        // Arrange

        // Act
        subject.confirmImportWatchOnly("17UovdU9ZvepPe75igTQwxqNME1HbnvMB7");
        // Assert
        //noinspection WrongConstant
        verify(activity).showRenameImportedAddressDialog(any(LegacyAddress.class));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void handlePrivateKeyWhenKeyIsNull() throws Exception {
        // Arrange

        // Act
        subject.handlePrivateKey(null, null);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void handlePrivateKeyExistingAddressSuccess() throws Exception {
        // Arrange
        ECKey mockECKey = mock(ECKey.class);
        when(mockECKey.hasPrivKey()).thenReturn(true);
        when(mockECKey.toAddress(any(NetworkParameters.class))).thenReturn(mock(Address.class));
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStringList().contains(any())).thenReturn(true);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(accountDataManager.setPrivateKey(mockECKey, null)).thenReturn(Observable.just(true));
        // Act
        subject.handlePrivateKey(mockECKey, null);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(activity).onUpdateAccountsList();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void handlePrivateKeyExistingAddressFailure() throws Exception {
        // Arrange
        ECKey mockECKey = mock(ECKey.class);
        when(mockECKey.hasPrivKey()).thenReturn(true);
        when(mockECKey.toAddress(any(NetworkParameters.class))).thenReturn(mock(Address.class));
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStringList().contains(any())).thenReturn(true);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        when(accountDataManager.setPrivateKey(mockECKey, null)).thenReturn(Observable.just(false));
        // Act
        subject.handlePrivateKey(mockECKey, null);
        // Assert
        //noinspection WrongConstant
        verify(activity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void handlePrivateKeyNewAddress() throws Exception {
        // Arrange
        ECKey mockECKey = mock(ECKey.class);
        when(mockECKey.hasPrivKey()).thenReturn(true);
        when(mockECKey.toAddress(any(NetworkParameters.class))).thenReturn(mock(Address.class));
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        //noinspection SuspiciousMethodCalls
        when(mockPayload.getLegacyAddressStringList().contains(any())).thenReturn(false);
        when(payloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        subject.handlePrivateKey(mockECKey, null);
        // Assert
        //noinspection WrongConstant
        verify(activity).showRenameImportedAddressDialog(any(LegacyAddress.class));
        verifyNoMoreInteractions(activity);
    }

    private class MockApplicationModule extends ApplicationModule {
        public MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return prefsUtil;
        }

        @Override
        protected AppUtil provideAppUtil() {
            return appUtil;
        }
    }

    private class MockDataManagerModule extends DataManagerModule {
        @Override
        protected TransferFundsDataManager provideTransferFundsDataManager(PayloadManager payloadManager) {
            return fundsDataManager;
        }

        @Override
        protected AccountDataManager provideAccountDataManager(PayloadManager payloadManager, MultiAddrFactory multiAddrFactory) {
            return accountDataManager;
        }
    }

    private class MockApiModule extends ApiModule {
        @Override
        protected PayloadManager providePayloadManager() {
            return payloadManager;
        }
    }
}
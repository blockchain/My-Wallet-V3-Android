package piuk.blockchain.android.ui.receive;

import android.annotation.SuppressLint;
import android.app.Application;
import android.graphics.Bitmap;

import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.HDWallet;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.datamanagers.ReceiveDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;
import rx.Observable;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.receive.ReceiveViewModel.KEY_WARN_WATCH_ONLY_SPEND;

@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class ReceiveViewModelTest {

    private ReceiveViewModel mSubject;
    @Mock PayloadManager mPayloadManager;
    @Mock AppUtil mAppUtil;
    @Mock PrefsUtil mPrefsUtil;
    @Mock StringUtils mStringUtils;
    @Mock ReceiveDataManager mDataManager;
    @Mock ExchangeRateFactory mExchangeRateFactory;
    @Mock WalletAccountHelper mWalletAccountHelper;
    @Mock private ReceiveActivity mActivity;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new MockDataManagerModule()
        );

        mSubject = new ReceiveViewModel(mActivity, Locale.UK);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange
        Payload mockPayload = mock(Payload.class);
        HDWallet mockHdWallet = mock(HDWallet.class);

        List<Account> accounts = new ArrayList<>();
        Account account0 = new Account();
        account0.setArchived(true);
        Account account1 = new Account();
        Account account2 = new Account();
        accounts.addAll(Arrays.asList(account0, account1, account2));

        List<LegacyAddress> legacyAddresses = new ArrayList<>();
        LegacyAddress legacy0 = new LegacyAddress();
        legacy0.setTag(LegacyAddress.ARCHIVED_ADDRESS);
        LegacyAddress legacy1 = new LegacyAddress();
        legacy1.setWatchOnly(true);
        LegacyAddress legacy2 = new LegacyAddress();
        legacy2.setLabel(null);
        LegacyAddress legacy3 = new LegacyAddress();
        legacy3.setLabel("Label");
        legacyAddresses.addAll(Arrays.asList(legacy0, legacy1, legacy2, legacy3));

        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(mockHdWallet.getAccounts()).thenReturn(accounts);
        when(mockPayload.getLegacyAddressList()).thenReturn(legacyAddresses);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mActivity).onSpinnerDataChanged();
        assertEquals(5, mSubject.mAccountMap.size());
        assertEquals(2, mSubject.mSpinnerIndexMap.size());
    }

    @Test
    public void getReceiveToList() throws Exception {
        // Arrange
        when(mWalletAccountHelper.getAccountItems(anyBoolean())).thenReturn(Collections.emptyList());
        when(mWalletAccountHelper.getAddressBookEntries()).thenReturn(Collections.emptyList());
        // Act
        List<ItemAccount> values = mSubject.getReceiveToList();
        // Assert
        assertNotNull(values);
        assertTrue(values.isEmpty());
        verify(mWalletAccountHelper).getAccountItems(true);
        verify(mWalletAccountHelper).getAddressBookEntries();
    }

    @Test
    public void getCurrencyHelper() throws Exception {
        // Arrange

        // Act
        ReceiveCurrencyHelper value = mSubject.getCurrencyHelper();
        // Assert
        assertNotNull(value);
    }

    @Test
    public void generateQrCodeSuccessful() throws Exception {
        // Arrange
        when(mDataManager.generateQrCode(anyString(), anyInt())).thenReturn(Observable.just(mock(Bitmap.class)));
        // Act
        mSubject.generateQrCode("test uri");
        // Assert
        verify(mActivity).showQrLoading();
        verify(mActivity).showQrCode(any(Bitmap.class));
    }

    @Test
    public void generateQrCodeFailure() throws Exception {
        // Arrange
        when(mDataManager.generateQrCode(anyString(), anyInt())).thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.generateQrCode("test uri");
        // Assert
        verify(mActivity).showQrLoading();
        verify(mActivity).showQrCode(null);
    }

    @SuppressLint("NewApi")
    @Test
    public void getDefaultSpinnerPosition() throws Exception {
        // Arrange
        Payload mockPayload = mock(Payload.class);
        HDWallet mockHdWallet = mock(HDWallet.class);

        List<Account> accounts = new ArrayList<>();
        Account account0 = new Account();
        Account account1 = new Account();
        Account account2 = new Account();
        accounts.addAll(Arrays.asList(account0, account1, account2));

        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mockHdWallet.getDefaultIndex()).thenReturn(2);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(mockHdWallet.getAccounts()).thenReturn(accounts);

        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.onViewReady(); // Update account list first
        Integer value = mSubject.getDefaultSpinnerPosition();
        // Assert
        assertEquals(2, Math.toIntExact(value));
    }

    @SuppressLint("NewApi")
    @Test
    public void getDefaultSpinnerPositionNotUpgraded() throws Exception {
        // Arrange
        Payload mockPayload = mock(Payload.class);
        when(mockPayload.isUpgraded()).thenReturn(false);

        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        Integer value = mSubject.getDefaultSpinnerPosition();
        // Assert
        assertEquals(0, Math.toIntExact(value));
    }

    @Test
    public void getAccountItemForPosition() throws Exception {
        // Arrange
        Payload mockPayload = mock(Payload.class);
        HDWallet mockHdWallet = mock(HDWallet.class);

        List<Account> accounts = new ArrayList<>();
        Account account0 = new Account();
        Account account1 = new Account();
        Account account2 = new Account();
        accounts.addAll(Arrays.asList(account0, account1, account2));

        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(mockHdWallet.getAccounts()).thenReturn(accounts);

        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        // Act
        mSubject.onViewReady(); // Update account list first
        Object value = mSubject.getAccountItemForPosition(2);
        // Assert
        assertEquals(account2, value);
    }

    @Test
    public void isUpgraded() throws Exception {
        // Arrange
        Payload mockPayload = mock(Payload.class);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        // Act
        Boolean value = mSubject.isUpgraded();
        // Assert
        assertTrue(value);
    }

    @Test
    public void warnWatchOnlySpend() throws Exception {
        // Arrange
        when(mPrefsUtil.getValue(anyString(), anyBoolean())).thenReturn(true);
        // Act
        Boolean value = mSubject.warnWatchOnlySpend();
        // Assert
        assertTrue(value);
    }

    @Test
    public void setWarnWatchOnlySpend() throws Exception {
        // Arrange

        // Act
        mSubject.setWarnWatchOnlySpend(true);
        // Assert
        verify(mPrefsUtil).setValue(KEY_WARN_WATCH_ONLY_SPEND, true);
    }

    @Test
    public void updateFiatTextField() throws Exception {
        // Arrange
        /**
         * This isn't reasonably testable in it's current form. The method relies on the
         * {@link ReceiveCurrencyHelper}, which needs to be injected. This is simple, but
         * would be best done after a large package refactor so that it can be scoped correctly.
         * This won't happen for a little bit.
         */
        // Act
        // TODO: 25/08/2016 Test me
        // Assert

    }

    @Test
    public void updateBtcTextField() throws Exception {
        // Arrange
        // See above
        // Act
        // TODO: 25/08/2016 Test me
        // Assert

    }

    @Test
    public void getV3ReceiveAddress() throws Exception {
        Payload mockPayload = mock(Payload.class);
        HDWallet mockHdWallet = mock(HDWallet.class);

        List<Account> accounts = new ArrayList<>();
        Account account0 = new Account();
        Account account1 = new Account();
        Account account2 = new Account();
        accounts.addAll(Arrays.asList(account0, account1, account2));

        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(mockHdWallet.getAccounts()).thenReturn(accounts);

        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mPayloadManager.getNextReceiveAddress(anyInt())).thenReturn("test address");
        // Act
        mSubject.onViewReady(); // Update account list first
        String value = mSubject.getV3ReceiveAddress(account2);
        // Assert
        assertEquals("test address", value);
    }

    @Test
    public void getV3ReceiveAddressException() throws Exception {
        Payload mockPayload = mock(Payload.class);
        HDWallet mockHdWallet = mock(HDWallet.class);

        List<Account> accounts = new ArrayList<>();
        Account account0 = new Account();
        Account account1 = new Account();
        Account account2 = new Account();
        accounts.addAll(Arrays.asList(account0, account1, account2));

        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.isUpgraded()).thenReturn(true);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(mockPayload.getHdWallet()).thenReturn(mockHdWallet);
        when(mockHdWallet.getAccounts()).thenReturn(accounts);

        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mPayloadManager.getNextReceiveAddress(anyInt())).thenThrow(new RuntimeException());
        // Act
        mSubject.onViewReady(); // Update account list first
        String value = mSubject.getV3ReceiveAddress(account2);
        // Assert
        assertNull(value);
    }

    @Test
    public void getIntentDataList() throws Exception {
        // Arrange
        // This isn't reasonably testable in it's current form
        // TODO: 25/08/2016 More refactoring of this method
        // Act

        // Assert

    }

    @Test
    public void getAppUtil() throws Exception {
        // Arrange

        // Act
        AppUtil util = mSubject.getAppUtil();
        // Assert
        Assert.assertEquals(util, mAppUtil);
    }

    @Test
    public void destroy() throws Exception {
        // Arrange

        // Act
        mSubject.destroy();
        // Assert
        assertFalse(mSubject.mCompositeSubscription.hasSubscriptions());
    }

    private class MockApplicationModule extends ApplicationModule {

        MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return mPrefsUtil;
        }

        @Override
        protected AppUtil provideAppUtil() {
            return mAppUtil;
        }

        @Override
        protected StringUtils provideStringUtils() {
            return mStringUtils;
        }

        @Override
        protected ExchangeRateFactory provideExchangeRateFactory() {
            return mExchangeRateFactory;
        }
    }

    private class MockApiModule extends ApiModule {

        @Override
        protected PayloadManager providePayloadManager() {
            return mPayloadManager;
        }
    }

    private class MockDataManagerModule extends DataManagerModule {

        @Override
        protected ReceiveDataManager provideReceiveDataManager() {
            return mDataManager;
        }

        @Override
        protected WalletAccountHelper provideWalletAccountHelper(PayloadManager payloadManager, PrefsUtil prefsUtil, StringUtils stringUtils, ExchangeRateFactory exchangeRateFactory) {
            return mWalletAccountHelper;
        }
    }
}
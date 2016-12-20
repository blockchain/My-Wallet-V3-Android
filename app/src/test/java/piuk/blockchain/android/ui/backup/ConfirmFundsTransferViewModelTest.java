package piuk.blockchain.android.ui.backup;

import android.app.Application;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Account;
import info.blockchain.wallet.payload.LegacyAddress;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.util.CharSequenceX;

import org.apache.commons.lang3.tuple.Triple;
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
import java.util.List;

import io.reactivex.Observable;
import piuk.blockchain.android.BlockchainTestApplication;
import piuk.blockchain.android.BuildConfig;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.account.ItemAccount;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.ui.send.PendingTransaction;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
@Config(sdk = 23, constants = BuildConfig.class, application = BlockchainTestApplication.class)
@RunWith(RobolectricTestRunner.class)
public class ConfirmFundsTransferViewModelTest {

    private ConfirmFundsTransferViewModel mSubject;
    @Mock ConfirmFundsTransferViewModel.DataListener mActivity;
    @Mock WalletAccountHelper mWalletAccountHelper;
    @Mock TransferFundsDataManager mFundsDataManager;
    @Mock PayloadManager mPayloadManager;
    @Mock PrefsUtil mPrefsUtil;
    @Mock StringUtils mStringUtils;
    @Mock ExchangeRateFactory mExchangeRateFactory;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new MockDataManagerModule());

        mSubject = new ConfirmFundsTransferViewModel(mActivity);
    }

    @Test
    public void onViewReady() throws Exception {
        // Arrange
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mockPayload.getHdWallet().getDefaultIndex()).thenReturn(0);
        PendingTransaction transaction = new PendingTransaction();
        List<PendingTransaction> transactions = new ArrayList<PendingTransaction>() {{
           add(transaction);
           add(transaction);
        }};
        Triple triple = Triple.of(transactions, 100000000L, 10000L);
        when(mFundsDataManager.getTransferableFundTransactionList(0)).thenReturn(Observable.just(triple));
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mActivity).setPaymentButtonEnabled(false);
        assertEquals(2, mSubject.mPendingTransactions.size());
    }

    @Test
    public void accountSelectedError() throws Exception {
        // Arrange
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        Account account1 = new Account();
        account1.setArchived(true);
        Account account2 = new Account();
        when(mockPayload.getHdWallet().getAccounts()).thenReturn(new ArrayList<Account>() {{
            add(account1);
            add(account2);
        }});
        when(mFundsDataManager.getTransferableFundTransactionList(1)).thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.accountSelected(0);
        // Assert
        verify(mActivity).setPaymentButtonEnabled(false);
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(mActivity).dismissDialog();
    }

    @Test
    public void updateUi() throws Exception {
        // Arrange
        when(mStringUtils.getQuantityString(anyInt(), anyInt())).thenReturn("test string");
        when(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(MonetaryUtil.UNIT_BTC);
        when(mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)).thenReturn("USD");
        when(mExchangeRateFactory.getLastPrice(anyString())).thenReturn(100.0D);
        when(mExchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        mSubject.mPendingTransactions = new ArrayList<>();
        // Act
        mSubject.updateUi(100000000L, 10000L);
        // Assert
        verify(mActivity).updateFromLabel("test string");
        verify(mActivity).updateTransferAmountBtc("1.0 BTC");
        verify(mActivity).updateTransferAmountFiat("$100.00");
        verify(mActivity).updateFeeAmountBtc("0.0001 BTC");
        verify(mActivity).updateFeeAmountFiat("$0.01");
        verify(mActivity).setPaymentButtonEnabled(true);
        verify(mActivity).onUiUpdated();
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void sendPaymentAndArchive() throws Exception {
        // Arrange
        when(mFundsDataManager.sendPayment(any(Payment.class), anyListOf(PendingTransaction.class), any(CharSequenceX.class))).thenReturn(Observable.just("hash"));
        when(mActivity.getIfArchiveChecked()).thenReturn(true);
        PendingTransaction transaction = new PendingTransaction();
        transaction.sendingObject = new ItemAccount("", "", null, null, null);
        transaction.sendingObject.accountObject = new LegacyAddress();
        mSubject.mPendingTransactions = new ArrayList<PendingTransaction>() {{
            add(transaction);
        }};
        when(mFundsDataManager.savePayloadToServer()).thenReturn(Observable.just(true));
        // Act
        mSubject.sendPayment(new CharSequenceX("password"));
        // Assert
        verify(mActivity).getIfArchiveChecked();
        verify(mActivity).setPaymentButtonEnabled(false);
        verify(mActivity, times(2)).showProgressDialog();
        verify(mActivity, times(2)).hideProgressDialog();
        //noinspection WrongConstant
        verify(mActivity, times(2)).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(mActivity).dismissDialog();
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void sendPaymentNoArchive() throws Exception {
        // Arrange
        when(mFundsDataManager.sendPayment(any(Payment.class), anyListOf(PendingTransaction.class), any(CharSequenceX.class))).thenReturn(Observable.just("hash"));
        when(mActivity.getIfArchiveChecked()).thenReturn(false);
        // Act
        mSubject.sendPayment(new CharSequenceX("password"));
        // Assert
        verify(mActivity).getIfArchiveChecked();
        verify(mActivity).setPaymentButtonEnabled(false);
        verify(mActivity).showProgressDialog();
        verify(mActivity).hideProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(mActivity).dismissDialog();
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void sendPaymentError() throws Exception {
        // Arrange
        when(mFundsDataManager.sendPayment(any(Payment.class), anyListOf(PendingTransaction.class), any(CharSequenceX.class))).thenReturn(Observable.error(new Throwable()));
        when(mActivity.getIfArchiveChecked()).thenReturn(false);
        // Act
        mSubject.sendPayment(new CharSequenceX("password"));
        // Assert
        verify(mActivity).getIfArchiveChecked();
        verify(mActivity).setPaymentButtonEnabled(false);
        verify(mActivity).showProgressDialog();
        verify(mActivity).hideProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(mActivity).dismissDialog();
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void getReceiveToList() throws Exception {
        // Arrange
        when(mWalletAccountHelper.getAccountItems(anyBoolean())).thenReturn(new ArrayList<>());
        // Act
        List<ItemAccount> value = mSubject.getReceiveToList();
        // Assert
        assertNotNull(value);
        assertTrue(value.isEmpty());
    }

    @Test
    public void getDefaultAccount() throws Exception {
        // Arrange
        Payload mockPayload = Mockito.mock(Payload.class, RETURNS_DEEP_STUBS);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mPayloadManager.getPayload().getHdWallet().getDefaultIndex()).thenReturn(1);
        Account account1 = new Account();
        account1.setArchived(true);
        Account account2 = new Account();
        when(mockPayload.getHdWallet().getAccounts()).thenReturn(new ArrayList<Account>() {{
            add(account1);
            add(account2);
        }});
        // Act
        int value = mSubject.getDefaultAccount();
        // Assert
        assertEquals(0, value);
    }

    @Test
    public void archiveAllSuccessful() throws Exception {
        // Arrange
        PendingTransaction transaction = new PendingTransaction();
        transaction.sendingObject = new ItemAccount("", "", null, null, null);
        transaction.sendingObject.accountObject = new LegacyAddress();
        mSubject.mPendingTransactions = new ArrayList<PendingTransaction>() {{
            add(transaction);
        }};
        when(mFundsDataManager.savePayloadToServer()).thenReturn(Observable.just(true));
        // Act
        mSubject.archiveAll();
        // Assert
        verify(mActivity).showProgressDialog();
        verify(mActivity).hideProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), eq(ToastCustom.TYPE_OK));
        verify(mActivity).dismissDialog();
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void archiveAllUnsuccessful() throws Exception {
        // Arrange
        PendingTransaction transaction = new PendingTransaction();
        transaction.sendingObject = new ItemAccount("", "", null, null, null);
        transaction.sendingObject.accountObject = new LegacyAddress();
        mSubject.mPendingTransactions = new ArrayList<PendingTransaction>() {{
            add(transaction);
        }};
        when(mFundsDataManager.savePayloadToServer()).thenReturn(Observable.just(false));
        // Act
        mSubject.archiveAll();
        // Assert
        verify(mActivity).showProgressDialog();
        verify(mActivity).hideProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(mActivity).dismissDialog();
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void archiveAllThrowsException() throws Exception {
        // Arrange
        PendingTransaction transaction = new PendingTransaction();
        transaction.sendingObject = new ItemAccount("", "", null, null, null);
        transaction.sendingObject.accountObject = new LegacyAddress();
        mSubject.mPendingTransactions = new ArrayList<PendingTransaction>() {{
            add(transaction);
        }};
        when(mFundsDataManager.savePayloadToServer()).thenReturn(Observable.error(new Throwable()));
        // Act
        mSubject.archiveAll();
        // Assert
        verify(mActivity).showProgressDialog();
        verify(mActivity).hideProgressDialog();
        //noinspection WrongConstant
        verify(mActivity).showToast(anyInt(), eq(ToastCustom.TYPE_ERROR));
        verify(mActivity).dismissDialog();
        verifyNoMoreInteractions(mActivity);
    }

    private class MockApplicationModule extends ApplicationModule {
        public MockApplicationModule(Application application) {
            super(application);
        }

        @Override
        protected PrefsUtil providePrefsUtil() {
            return mPrefsUtil;
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

    private class MockDataManagerModule extends DataManagerModule {

        @Override
        protected TransferFundsDataManager provideTransferFundsDataManager(PayloadManager payloadManager) {
            return mFundsDataManager;
        }

        @Override
        protected WalletAccountHelper provideWalletAccountHelper(PayloadManager payloadManager, PrefsUtil prefsUtil, StringUtils stringUtils, ExchangeRateFactory exchangeRateFactory, MultiAddrFactory multiAddrFactory) {
            return mWalletAccountHelper;
        }
    }

    private class MockApiModule extends ApiModule {
        @Override
        protected PayloadManager providePayloadManager() {
            return mPayloadManager;
        }
    }
}
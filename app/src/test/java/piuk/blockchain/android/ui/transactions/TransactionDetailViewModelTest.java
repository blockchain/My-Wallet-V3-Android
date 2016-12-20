package piuk.blockchain.android.ui.transactions;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Intent;
import android.support.v4.util.Pair;

import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.Payload;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.transaction.Transaction;
import info.blockchain.wallet.transaction.Tx;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import io.reactivex.Observable;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.R;
import piuk.blockchain.android.RxTest;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.stores.TransactionListStore;
import piuk.blockchain.android.injection.ApiModule;
import piuk.blockchain.android.injection.ApplicationModule;
import piuk.blockchain.android.injection.DataManagerModule;
import piuk.blockchain.android.injection.Injector;
import piuk.blockchain.android.injection.InjectorTestUtils;
import piuk.blockchain.android.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.MonetaryUtil;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_LIST_POSITION;

@SuppressLint("UseSparseArrays")
@SuppressWarnings("PrivateMemberAccessBetweenOuterAndInnerClass")
public class TransactionDetailViewModelTest extends RxTest {

    @Mock PrefsUtil mPrefsUtil;
    @Mock PayloadManager mPayloadManager;
    @Mock StringUtils mStringUtils;
    @Mock TransactionListDataManager mTransactionListDataManager;
    @Mock TransactionDetailViewModel.DataListener mActivity;
    @Mock ExchangeRateFactory mExchangeRateFactory;
    @Mock TransactionHelper mTransactionHelper;
    private TransactionDetailViewModel mSubject;

    // Transactions
    private Tx mTxMoved = new Tx("hash", "note", "MOVED", 1.0D, 0L, new HashMap<>());
    private Tx mTxSent = new Tx("hash", "note", "SENT", -1.0D, 0L, new HashMap<>());
    private Tx mTxReceived = new Tx("hash", "note", "RECEIVED", 2.0D, 0L, new HashMap<>());
    private List<Tx> mTxList;

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        when(mPrefsUtil.getValue(PrefsUtil.KEY_BTC_UNITS, MonetaryUtil.UNIT_BTC)).thenReturn(MonetaryUtil.UNIT_BTC);
        when(mPrefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)).thenReturn(PrefsUtil.DEFAULT_CURRENCY);

        InjectorTestUtils.initApplicationComponent(
                Injector.getInstance(),
                new MockApplicationModule(RuntimeEnvironment.application),
                new MockApiModule(),
                new MockDataManagerModule());

        mTxMoved.setIsMove(true);
        mTxList = new ArrayList<Tx>() {{
            add(mTxMoved);
            add(mTxSent);
            add(mTxReceived);
        }};
        Locale.setDefault(new Locale("EN", "US"));
        mSubject = new TransactionDetailViewModel(mActivity);
    }

    @Test
    public void onViewReadyNoIntent() throws Exception {
        // Arrange
        when(mActivity.getPageIntent()).thenReturn(null);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mActivity).getPageIntent();
        verify(mActivity).pageFinish();
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void onViewReadyNoKey() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)).thenReturn(false);
        when(mActivity.getPageIntent()).thenReturn(mockIntent);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mActivity, times(2)).getPageIntent();
        verify(mActivity).pageFinish();
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void onViewReadyKeyOutOfBounds() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)).thenReturn(true);
        when(mockIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1)).thenReturn(-1);
        when(mActivity.getPageIntent()).thenReturn(mockIntent);
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mActivity, times(3)).getPageIntent();
        verify(mActivity).pageFinish();
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void onViewReadyKeyValidTransactionNotFound() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        Payload mockPayload = mock(Payload.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)).thenReturn(true);
        when(mockIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1)).thenReturn(0);
        when(mockPayload.getTransactionNotesMap()).thenReturn(new HashMap<>());
        when(mActivity.getPageIntent()).thenReturn(mockIntent);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mTransactionListDataManager.getTransactionList()).thenReturn(mTxList);
        when(mStringUtils.getString(R.string.transaction_detail_pending)).thenReturn("Pending (%1$s/%2$s Confirmations)");
        when(mTransactionListDataManager.getTransactionFromHash(anyString())).thenReturn(Observable.error(new Throwable()));
        double price = 1000.00D;
        when(mExchangeRateFactory.getHistoricPrice(anyLong(), anyString(), anyLong())).thenReturn(Observable.just(price));
        when(mStringUtils.getString(R.string.transaction_detail_value_at_time_transferred)).thenReturn("Value when moved: ");
        when(mExchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mActivity, times(3)).getPageIntent();
        verify(mActivity).setStatus("Pending (0/3 Confirmations)", "hash");
        verify(mActivity).setTransactionType("MOVED");
        verify(mActivity).setTransactionColour(R.color.blockchain_transfer_blue_50);
        verify(mActivity).setDescription(null);
        verify(mActivity).setDate(anyString());
        verify(mActivity).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
        verify(mActivity).setToAddresses(any());
        verify(mActivity).setTransactionValueFiat(anyString());
        verify(mActivity).setFromAddress(anyString());
        verify(mActivity).onDataLoaded();
    }

    @Test
    public void onViewReadyKeyValidTransactionFound() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        Payload mockPayload = mock(Payload.class);
        Transaction mockTransaction = mock(Transaction.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)).thenReturn(true);
        when(mockIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1)).thenReturn(0);
        when(mockPayload.getTransactionNotesMap()).thenReturn(new HashMap<>());
        when(mActivity.getPageIntent()).thenReturn(mockIntent);
        when(mPayloadManager.getPayload()).thenReturn(mockPayload);
        when(mTransactionListDataManager.getTransactionList()).thenReturn(mTxList);
        when(mStringUtils.getString(R.string.transaction_detail_pending)).thenReturn("Pending (%1$s/%2$s Confirmations)");
        when(mTransactionListDataManager.getTransactionFromHash(anyString())).thenReturn(Observable.just(mockTransaction));
        HashMap<String, Long> inputs = new HashMap<>();
        HashMap<String, Long> outputs = new HashMap<>();
        inputs.put("addr1", 1000L);
        outputs.put("addr2", 2000L);
        Pair pair = new Pair<>(inputs, outputs);
        when(mTransactionHelper.filterNonChangeAddresses(any(Transaction.class), any(Tx.class))).thenReturn(pair);
        when(mTransactionHelper.addressToLabel("addr1")).thenReturn("account1");
        when(mTransactionHelper.addressToLabel("addr2")).thenReturn("account2");
        double price = 1000.00D;
        when(mExchangeRateFactory.getHistoricPrice(anyLong(), anyString(), anyLong())).thenReturn(Observable.just(price));
        when(mStringUtils.getString(R.string.transaction_detail_value_at_time_transferred)).thenReturn("Value when moved: ");
        when(mExchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        // Act
        mSubject.onViewReady();
        // Assert
        verify(mActivity, times(3)).getPageIntent();
        verify(mActivity).setStatus("Pending (0/3 Confirmations)", "hash");
        verify(mActivity).setTransactionType("MOVED");
        verify(mActivity).setTransactionColour(R.color.blockchain_transfer_blue_50);
        verify(mActivity).setDescription(null);
        verify(mActivity).setDate(anyString());
        verify(mActivity).setToAddresses(any());
        verify(mActivity).setFromAddress(any());
        verify(mActivity).setFee(anyString());
        verify(mActivity).setTransactionValueBtc(anyString());
        verify(mActivity).setTransactionValueFiat(anyString());
        verify(mActivity).onDataLoaded();
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void getTransactionValueStringUsd() {
        // Arrange
        double price = 1000.00D;
        when(mExchangeRateFactory.getHistoricPrice(anyLong(), anyString(), anyLong())).thenReturn(Observable.just(price));
        when(mStringUtils.getString(anyInt())).thenReturn("Value when sent: ");
        when(mExchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        // Act
        TestObserver<String> observer = mSubject.getTransactionValueString("USD", mTxSent).test();
        // Assert
        assertEquals("Value when sent: $1 000.00", observer.values().get(0));
        observer.onComplete();
        observer.assertNoErrors();
    }

    @Test
    public void getTransactionValueStringNonUsd() {
        // Arrange

        // Act
        TestObserver<String> observer = mSubject.getTransactionValueString("GBP", mTxReceived).test();
        // Assert
        assertNotNull(observer.values().get(0));
        observer.onComplete();
        observer.assertNoErrors();
    }

    @Test
    public void getTransactionValueStringReceived() {
        // Arrange
        double price = 1000.00D;
        when(mExchangeRateFactory.getHistoricPrice(anyLong(), anyString(), anyLong())).thenReturn(Observable.just(price));
        when(mStringUtils.getString(anyInt())).thenReturn("Value when received: ");
        when(mExchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        // Act
        TestObserver<String> observer = mSubject.getTransactionValueString("USD", mTxReceived).test();
        // Assert
        assertEquals("Value when received: $1 000.00", observer.values().get(0));
        observer.onComplete();
        observer.assertNoErrors();
    }

    @Test
    public void getTransactionValueStringTransferred() {
        // Arrange
        double price = 1000.00D;
        when(mExchangeRateFactory.getHistoricPrice(anyLong(), anyString(), anyLong())).thenReturn(Observable.just(price));
        when(mStringUtils.getString(anyInt())).thenReturn("Value when transferred: ");
        when(mExchangeRateFactory.getSymbol(anyString())).thenReturn("$");
        // Act
        TestObserver<String> observer = mSubject.getTransactionValueString("USD", mTxSent).test();
        // Assert
        assertEquals("Value when transferred: $1 000.00", observer.values().get(0));
        observer.onComplete();
        observer.assertNoErrors();
    }

    @Test
    public void updateTransactionNoteSuccess() throws Exception {
        // Arrange
        when(mTransactionListDataManager.updateTransactionNotes(anyString(), anyString())).thenReturn(Observable.just(true));
        mSubject.mTransaction = mTxMoved;
        // Act
        mSubject.updateTransactionNote("note");
        // Assert
        verify(mTransactionListDataManager).updateTransactionNotes("hash", "note");
        //noinspection WrongConstant
        verify(mActivity).showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
        verify(mActivity).setDescription("note");
    }

    @Test
    public void updateTransactionNoteFailure() throws Exception {
        // Arrange
        when(mTransactionListDataManager.updateTransactionNotes(anyString(), anyString())).thenReturn(Observable.just(false));
        mSubject.mTransaction = mTxMoved;
        // Act
        mSubject.updateTransactionNote("note");
        // Assert
        verify(mTransactionListDataManager).updateTransactionNotes("hash", "note");
        //noinspection WrongConstant
        verify(mActivity).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
    }

    @Test
    public void updateTransactionNoteException() throws Exception {
        // Arrange
        when(mTransactionListDataManager.updateTransactionNotes(anyString(), anyString())).thenReturn(Observable.error(new Throwable()));
        mSubject.mTransaction = mTxMoved;
        // Act
        mSubject.updateTransactionNote("note");
        // Assert
        verify(mTransactionListDataManager).updateTransactionNotes("hash", "note");
        //noinspection WrongConstant
        verify(mActivity).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
    }

    @Test
    public void setTransactionStatusNoConfirmations() {
        // Arrange
        when(mStringUtils.getString(R.string.transaction_detail_pending)).thenReturn("Pending (%1$s/%2$s Confirmations)");
        // Act
        mSubject.setConfirmationStatus(mTxMoved);
        // Assert
        verify(mActivity).setStatus("Pending (0/3 Confirmations)", "hash");
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void setTransactionStatusConfirmed() {
        // Arrange
        when(mStringUtils.getString(R.string.transaction_detail_confirmed)).thenReturn("Confirmed");
        mTxMoved.setConfirmations(3);
        // Act
        mSubject.setConfirmationStatus(mTxMoved);
        // Assert
        verify(mActivity).setStatus("Confirmed", "hash");
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void setTransactionColorMove() {
        // Arrange

        // Act
        mSubject.setTransactionColor(mTxMoved);
        // Assert
        verify(mActivity).setTransactionColour(R.color.blockchain_transfer_blue_50);
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void setTransactionColorMoveConfirmed() {
        // Arrange
        mTxMoved.setConfirmations(3);
        // Act
        mSubject.setTransactionColor(mTxMoved);
        // Assert
        verify(mActivity).setTransactionColour(R.color.blockchain_transfer_blue);
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void setTransactionColorSent() {
        // Arrange

        // Act
        mSubject.setTransactionColor(mTxSent);
        // Assert
        verify(mActivity).setTransactionColour(R.color.blockchain_red_50);
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void setTransactionColorSentConfirmed() {
        // Arrange
        mTxSent.setConfirmations(3);
        // Act
        mSubject.setTransactionColor(mTxSent);
        // Assert
        verify(mActivity).setTransactionColour(R.color.blockchain_send_red);
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void setTransactionColorReceived() {
        // Arrange

        // Act
        mSubject.setTransactionColor(mTxReceived);
        // Assert
        verify(mActivity).setTransactionColour(R.color.blockchain_green_50);
        verifyNoMoreInteractions(mActivity);
    }

    @Test
    public void setTransactionColorReceivedConfirmed() {
        // Arrange
        mTxReceived.setConfirmations(3);
        // Act
        mSubject.setTransactionColor(mTxReceived);
        // Assert
        verify(mActivity).setTransactionColour(R.color.blockchain_receive_green);
        verifyNoMoreInteractions(mActivity);
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
        protected TransactionListDataManager provideTransactionListDataManager(PayloadManager payloadManager, TransactionListStore transactionListStore) {
            return mTransactionListDataManager;
        }

        @Override
        protected TransactionHelper provideTransactionHelper(PayloadManager payloadManager, MultiAddrFactory multiAddrFactory) {
            return mTransactionHelper;
        }
    }
}
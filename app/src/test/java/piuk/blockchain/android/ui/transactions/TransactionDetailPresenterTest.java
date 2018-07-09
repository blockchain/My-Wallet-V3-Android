package piuk.blockchain.android.ui.transactions;

import android.content.Intent;

import info.blockchain.wallet.multiaddress.TransactionSummary;
import info.blockchain.wallet.payload.data.Wallet;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import piuk.blockchain.android.R;
import piuk.blockchain.android.testutils.RxTest;
import piuk.blockchain.android.data.bitcoincash.BchDataManager;
import piuk.blockchain.android.data.contacts.models.ContactTransactionDisplayModel;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.ethereum.EthDataManager;
import piuk.blockchain.androidcore.data.api.EnvironmentConfig;
import piuk.blockchain.androidcore.data.transactions.models.BchDisplayable;
import piuk.blockchain.androidcore.data.transactions.models.BtcDisplayable;
import piuk.blockchain.androidcore.data.transactions.models.Displayable;
import piuk.blockchain.androidcore.data.transactions.models.EthDisplayable;
import piuk.blockchain.androidcoreui.ui.customviews.ToastCustom;
import piuk.blockchain.android.util.StringUtils;
import piuk.blockchain.androidcore.data.contacts.ContactsDataManager;
import piuk.blockchain.androidcore.data.currency.CryptoCurrencies;
import piuk.blockchain.androidcore.data.currency.CurrencyFormatManager;
import piuk.blockchain.androidcore.data.currency.CurrencyState;
import piuk.blockchain.androidcore.data.exchangerate.ExchangeRateDataManager;
import piuk.blockchain.androidcore.data.payload.PayloadDataManager;
import piuk.blockchain.androidcore.utils.PrefsUtil;

import static junit.framework.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_HASH;
import static piuk.blockchain.android.ui.balance.BalanceFragment.KEY_TRANSACTION_LIST_POSITION;

@SuppressWarnings("WeakerAccess")
public class TransactionDetailPresenterTest extends RxTest {

    private TransactionDetailPresenter subject;
    @Mock TransactionHelper transactionHelper;
    @Mock PrefsUtil prefsUtil;
    @Mock PayloadDataManager payloadDataManager;
    @Mock StringUtils stringUtils;
    @Mock TransactionListDataManager transactionListDataManager;
    @Mock TransactionDetailView activity;
    @Mock ExchangeRateDataManager exchangeRateFactory;
    @Mock ContactsDataManager contactsDataManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) EthDataManager ethDataManager;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) BchDataManager bchDataManager;
    @Mock EnvironmentConfig environmentSettings;
    @Mock CurrencyState currencyState;
    @Mock CurrencyFormatManager currencyFormatManager;

    DecimalFormat dm = new DecimalFormat();

    @Before
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        dm.setMinimumFractionDigits(2);
        dm.setMaximumFractionDigits(2);

        when(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY)).thenReturn(PrefsUtil.DEFAULT_CURRENCY);

        Locale.setDefault(new Locale("EN", "US"));
        subject = new TransactionDetailPresenter(transactionHelper,
                prefsUtil,
                payloadDataManager,
                stringUtils,
                transactionListDataManager,
                exchangeRateFactory,
                contactsDataManager,
                ethDataManager,
                bchDataManager,
                environmentSettings,
                currencyState,
                currencyFormatManager);
        subject.initView(activity);
    }

    @Test
    public void onViewReadyNoIntent() throws Exception {
        // Arrange
        when(activity.getPageIntent()).thenReturn(null);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyNoKey() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)).thenReturn(false);
        when(activity.getPageIntent()).thenReturn(mockIntent);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyKeyOutOfBounds() throws Exception {
        // Arrange
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)).thenReturn(true);
        when(mockIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1)).thenReturn(-1);
        when(activity.getPageIntent()).thenReturn(mockIntent);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyNullIntent() throws Exception {
        // Arrange
        when(activity.getPageIntent()).thenReturn(null);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyIntentPositionInvalid() throws Exception {
        // Arrange
        Intent intent = mock(Intent.class);
        when(intent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1)).thenReturn(-1);
        when(activity.getPageIntent()).thenReturn(intent);
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void onViewReadyIntentHashNotFound() throws Exception {
        // Arrange
        Intent intent = mock(Intent.class);
        String txHash = "TX_HASH";
        when(intent.getStringExtra(KEY_TRANSACTION_HASH)).thenReturn(txHash);
        when(activity.getPageIntent()).thenReturn(intent);
        when(transactionListDataManager.getTxFromHash(txHash))
                .thenReturn(Single.error(new Throwable()));
        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).pageFinish();
        verifyNoMoreInteractions(activity);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onViewReadyTransactionFoundInList() throws Exception {
        // Arrange
        Displayable displayableToFind = mock(BtcDisplayable.class);
        when(displayableToFind.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        when(displayableToFind.getDirection()).thenReturn(TransactionSummary.Direction.TRANSFERRED);
        when(displayableToFind.getHash()).thenReturn("txMoved_hash");
        when(displayableToFind.getTotal()).thenReturn(BigInteger.valueOf(1_000L));
        when(displayableToFind.getFee()).thenReturn(BigInteger.valueOf(1L));

        Displayable displayable2 = mock(BtcDisplayable.class);
        when(displayable2.getHash()).thenReturn("");

        Displayable displayable3 = mock(BtcDisplayable.class);
        when(displayable3.getHash()).thenReturn("");

        Intent mockIntent = mock(Intent.class);
        Wallet mockPayload = mock(Wallet.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_LIST_POSITION)).thenReturn(true);
        when(mockIntent.getIntExtra(KEY_TRANSACTION_LIST_POSITION, -1)).thenReturn(0);
        when(mockPayload.getTxNotes()).thenReturn(new HashMap<>());
        when(activity.getPageIntent()).thenReturn(mockIntent);
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(transactionListDataManager.getTransactionList())
                .thenReturn(Arrays.asList(displayableToFind, displayable2, displayable3));
        when(stringUtils.getString(R.string.transaction_detail_pending))
                .thenReturn("Pending (%1$s/%2$s Confirmations)");
        HashMap<String, BigInteger> inputs = new HashMap<>();
        HashMap<String, BigInteger> outputs = new HashMap<>();
        inputs.put("addr1", BigInteger.valueOf(1000L));
        outputs.put("addr2", BigInteger.valueOf(2000L));
        Pair pair = Pair.of(inputs, outputs);
        when(transactionHelper.filterNonChangeAddresses(any(Displayable.class))).thenReturn(pair);
        when(payloadDataManager.addressToLabel("addr1")).thenReturn("account1");
        when(payloadDataManager.addressToLabel("addr2")).thenReturn("account2");
        BigDecimal price = BigDecimal.valueOf(1000.00);
        when(exchangeRateFactory.getBtcHistoricPrice(anyLong(), anyString(), anyLong()))
                .thenReturn(Observable.just(price));
        when(stringUtils.getString(R.string.transaction_detail_value_at_time_transferred))
                .thenReturn("Value when moved: ");
        when(currencyState.getCurrencySymbol(anyString(), any())).thenReturn("$");
        HashMap<String, ContactTransactionDisplayModel> notesMap = new HashMap<>();
        notesMap.put("txMoved_hash", new ContactTransactionDisplayModel(
                "",
                "",
                "transaction_note",
                ""
        ));
        when(contactsDataManager.getTransactionDisplayMap()).thenReturn(notesMap);
        when(currencyFormatManager.getFormattedBtcValueWithUnit(
                any(),
                any())).thenReturn("0.1");
        when(currencyFormatManager.getFiatFormat(
                any())).thenReturn(dm);

        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).setStatus(CryptoCurrencies.BTC, "Pending (0/3 Confirmations)", "txMoved_hash");
        verify(activity).setTransactionType(TransactionSummary.Direction.TRANSFERRED);
        verify(activity).setTransactionColour(R.color.product_gray_transferred_50);
        verify(activity).setDescription(null);
        verify(activity).setDate(anyString());
        verify(activity).setToAddresses(any());
        verify(activity).setFromAddress(any());
        verify(activity).setFee(anyString());
        verify(activity).setTransactionValueBtc(anyString());
        verify(activity).setTransactionValueFiat(anyString());
        verify(activity).setTransactionNote(anyString());
        verify(activity).onDataLoaded();
        verify(activity).setIsDoubleSpend(anyBoolean());
        verifyNoMoreInteractions(activity);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void onViewReadyTransactionFoundViaHash() throws Exception {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        when(displayable.getDirection()).thenReturn(TransactionSummary.Direction.TRANSFERRED);
        when(displayable.getHash()).thenReturn("txMoved_hash");
        when(displayable.getTotal()).thenReturn(BigInteger.valueOf(1_000L));
        when(displayable.getFee()).thenReturn(BigInteger.valueOf(1L));
        Intent mockIntent = mock(Intent.class);
        Wallet mockPayload = mock(Wallet.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_HASH)).thenReturn(true);
        when(mockIntent.getStringExtra(KEY_TRANSACTION_HASH)).thenReturn("txMoved_hash");
        when(activity.getPageIntent()).thenReturn(mockIntent);
        when(mockPayload.getTxNotes()).thenReturn(new HashMap<>());
        when(payloadDataManager.getWallet()).thenReturn(mockPayload);
        when(activity.getPageIntent()).thenReturn(mockIntent);
        when(transactionListDataManager.getTxFromHash("txMoved_hash"))
                .thenReturn(Single.just(displayable));
        when(stringUtils.getString(R.string.transaction_detail_pending))
                .thenReturn("Pending (%1$s/%2$s Confirmations)");
        HashMap<String, BigInteger> inputs = new HashMap<>();
        HashMap<String, BigInteger> outputs = new HashMap<>();
        inputs.put("addr1", BigInteger.valueOf(1000L));
        outputs.put("addr2", BigInteger.valueOf(2000L));
        Pair pair = Pair.of(inputs, outputs);
        when(transactionHelper.filterNonChangeAddresses(any(Displayable.class))).thenReturn(pair);
        when(payloadDataManager.addressToLabel("addr1")).thenReturn("account1");
        when(payloadDataManager.addressToLabel("addr2")).thenReturn("account2");
        BigDecimal price = BigDecimal.valueOf(1000.00);
        when(exchangeRateFactory.getBtcHistoricPrice(anyLong(), anyString(), anyLong()))
                .thenReturn(Observable.just(price));
        when(stringUtils.getString(R.string.transaction_detail_value_at_time_transferred))
                .thenReturn("Value when moved: ");
        when(currencyState.getCurrencySymbol(anyString(), any())).thenReturn("$");
        HashMap contactsMap = new HashMap<String, ContactTransactionDisplayModel>();
        contactsMap.put("txMoved_hash", new ContactTransactionDisplayModel(
                "",
                "",
                "",
                "Adam"
        ));
        when(contactsDataManager.getTransactionDisplayMap()).thenReturn(contactsMap);

        when(currencyFormatManager.getFormattedBtcValueWithUnit(
                any(),
                any())).thenReturn("0.1");
        when(currencyFormatManager.getFiatFormat(
                any())).thenReturn(dm);

        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).setStatus(CryptoCurrencies.BTC, "Pending (0/3 Confirmations)", "txMoved_hash");
        verify(activity).setTransactionType(TransactionSummary.Direction.TRANSFERRED);
        verify(activity).setTransactionColour(R.color.product_gray_transferred_50);
        verify(activity).setDescription(null);
        verify(activity).setDate(anyString());
        verify(activity).setToAddresses(any());
        verify(activity).setFromAddress(any());
        verify(activity).setFee(anyString());
        verify(activity).setTransactionValueBtc(anyString());
        verify(activity).setTransactionValueFiat(anyString());
        verify(activity).onDataLoaded();
        verify(activity).setIsDoubleSpend(anyBoolean());
        verify(activity).setTransactionNote(anyString());
        verifyNoMoreInteractions(activity);
    }

    @SuppressWarnings({"ConstantConditions", "unchecked"})
    @Test
    public void onViewReadyTransactionFoundViaHashEthereum() throws Exception {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.ETHER);
        when(displayable.getDirection()).thenReturn(TransactionSummary.Direction.SENT);
        when(displayable.getHash()).thenReturn("hash");
        when(displayable.getTotal()).thenReturn(BigInteger.valueOf(1_000L));
        when(displayable.getFee()).thenReturn(BigInteger.valueOf(1L));
        HashMap<String, BigInteger> maps = new HashMap<>();
        maps.put("", BigInteger.TEN);
        when(displayable.getInputsMap()).thenReturn(maps);
        when(displayable.getOutputsMap()).thenReturn(maps);
        Intent mockIntent = mock(Intent.class);
        when(mockIntent.hasExtra(KEY_TRANSACTION_HASH)).thenReturn(true);
        when(mockIntent.getStringExtra(KEY_TRANSACTION_HASH)).thenReturn("hash");
        when(activity.getPageIntent()).thenReturn(mockIntent);
        when(transactionListDataManager.getTxFromHash("hash"))
                .thenReturn(Single.just(displayable));
        when(stringUtils.getString(R.string.transaction_detail_pending))
                .thenReturn("Pending (%1$s/%2$s Confirmations)");
        when(stringUtils.getString(R.string.eth_default_account_label))
                .thenReturn("My Ethereum Wallet");
        HashMap<String, BigInteger> inputs = new HashMap<>();
        HashMap<String, BigInteger> outputs = new HashMap<>();
        inputs.put("addr1", BigInteger.valueOf(1000L));
        outputs.put("addr2", BigInteger.valueOf(2000L));
        Pair pair = Pair.of(inputs, outputs);
        when(transactionHelper.filterNonChangeAddresses(any(Displayable.class))).thenReturn(pair);
        BigDecimal price = BigDecimal.valueOf(1000.00);
        when(exchangeRateFactory.getEthHistoricPrice(any(), anyString(), anyLong()))
                .thenReturn(Observable.just(price));
        when(stringUtils.getString(R.string.transaction_detail_value_at_time_sent))
                .thenReturn("Value when sent: ");
        when(currencyState.getCurrencySymbol(anyString(), any())).thenReturn("$");
        HashMap contactsMap = new HashMap<String, ContactTransactionDisplayModel>();
        contactsMap.put("hash", new ContactTransactionDisplayModel(
                "",
                "",
                "",
                "Adam"
        ));
        when(contactsDataManager.getTransactionDisplayMap()).thenReturn(contactsMap);
        when(ethDataManager.getEthResponseModel().getAddressResponse().getAccount()).thenReturn("");
        when(ethDataManager.getTransactionNotes("hash")).thenReturn("note");

        when(currencyFormatManager.getFormattedEthShortValueWithUnit(
                any(),
                any())).thenReturn("0.1");
        when(currencyFormatManager.getFiatFormat(
                any())).thenReturn(dm);

        // Act
        subject.onViewReady();
        // Assert
        verify(activity).getPageIntent();
        verify(activity).setStatus(CryptoCurrencies.ETHER, "Pending (0/12 Confirmations)", "hash");
        verify(activity).setTransactionType(TransactionSummary.Direction.SENT);
        verify(activity).setTransactionColour(R.color.product_red_sent_50);
        verify(activity).setDescription(anyString());
        verify(activity).setDate(anyString());
        verify(activity).setToAddresses(any());
        verify(activity).setFromAddress(any());
        verify(activity).setFee(anyString());
        verify(activity).setTransactionValueBtc(anyString());
        verify(activity).setTransactionValueFiat(anyString());
        verify(activity).onDataLoaded();
        verify(activity).setIsDoubleSpend(anyBoolean());
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void getTransactionValueStringUsd() {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        when(displayable.getDirection()).thenReturn(TransactionSummary.Direction.SENT);
        when(displayable.getTotal()).thenReturn(BigInteger.valueOf(1_000L));
        BigDecimal price = BigDecimal.valueOf(1000.00);
        when(exchangeRateFactory.getBtcHistoricPrice(anyLong(), anyString(), anyLong()))
                .thenReturn(Observable.just(price));
        when(stringUtils.getString(anyInt())).thenReturn("Value when sent: ");
        when(currencyState.getCurrencySymbol(anyString(), any())).thenReturn("$");
        when(currencyFormatManager.getFiatFormat(
                any())).thenReturn(dm);
        when(currencyFormatManager.getFormattedBtcValueWithUnit(
                any(),
                any())).thenReturn("0.1");

        // Act
        TestObserver<String> observer =
                subject.getTransactionValueString("USD", displayable).test();

        // Assert
        verify(exchangeRateFactory).getBtcHistoricPrice(anyLong(), anyString(), anyLong());
        assertEquals("Value when sent: $1,000.00", observer.values().get(0));
        observer.onComplete();
        observer.assertNoErrors();
    }

    @Test
    public void getTransactionValueStringReceivedEth() {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.ETHER);
        when(displayable.getDirection()).thenReturn(TransactionSummary.Direction.RECEIVED);
        when(displayable.getTotal()).thenReturn(BigInteger.valueOf(1_000L));
        BigDecimal price = BigDecimal.valueOf(1000.00);
        when(exchangeRateFactory.getEthHistoricPrice(any(), anyString(), anyLong()))
                .thenReturn(Observable.just(price));
        when(stringUtils.getString(anyInt())).thenReturn("Value when received: ");
        when(currencyState.getCurrencySymbol(anyString(), any())).thenReturn("$");

        when(currencyFormatManager.getFormattedEthShortValueWithUnit(
                any(),
                any())).thenReturn("0.1");
        when(currencyFormatManager.getFiatFormat(
                any())).thenReturn(dm);

        // Act
        TestObserver<String> observer = subject.getTransactionValueString("USD", displayable).test();
        // Assert
        verify(exchangeRateFactory).getEthHistoricPrice(any(), anyString(), anyLong());
        assertEquals("Value when received: $1,000.00", observer.values().get(0));
        observer.onComplete();
        observer.assertNoErrors();
    }

    @Test
    public void getTransactionValueStringTransferred() {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        when(displayable.getDirection()).thenReturn(TransactionSummary.Direction.SENT);
        when(displayable.getTotal()).thenReturn(BigInteger.valueOf(1_000L));
        BigDecimal price = BigDecimal.valueOf(1000.00);
        when(exchangeRateFactory.getBtcHistoricPrice(anyLong(), anyString(), anyLong()))
                .thenReturn(Observable.just(price));
        when(stringUtils.getString(anyInt())).thenReturn("Value when transferred: ");
        when(currencyState.getCurrencySymbol(anyString(), any())).thenReturn("$");
        when(prefsUtil.getValue(PrefsUtil.KEY_SELECTED_FIAT, PrefsUtil.DEFAULT_CURRENCY))
                .thenReturn("USD");
        when(currencyFormatManager.getFiatFormat(
                any())).thenReturn(dm);
        // Act
        TestObserver<String> observer = subject.getTransactionValueString("USD", displayable).test();
        // Assert
        verify(exchangeRateFactory).getBtcHistoricPrice(anyLong(), anyString(), anyLong());
        assertEquals("Value when transferred: $1,000.00", observer.values().get(0));
        observer.onComplete();
        observer.assertNoErrors();
    }

    @Test
    public void updateTransactionNoteBtcSuccess() throws Exception {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getHash()).thenReturn("hash");
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        subject.displayable = displayable;
        when(payloadDataManager.updateTransactionNotes(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        subject.updateTransactionNote("note");
        // Assert
        verify(payloadDataManager).updateTransactionNotes("hash", "note");
        //noinspection WrongConstant
        verify(activity).showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
        verify(activity).setDescription("note");
    }

    @Test
    public void updateTransactionNoteEthSuccess() throws Exception {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getHash()).thenReturn("hash");
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.ETHER);
        subject.displayable = displayable;
        when(ethDataManager.updateTransactionNotes(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        subject.updateTransactionNote("note");
        // Assert
        verify(ethDataManager).updateTransactionNotes("hash", "note");
        //noinspection WrongConstant
        verify(activity).showToast(R.string.remote_save_ok, ToastCustom.TYPE_OK);
        verify(activity).setDescription("note");
    }

    @Test
    public void updateTransactionNoteFailure() throws Exception {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getHash()).thenReturn("hash");
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        subject.displayable = displayable;
        when(payloadDataManager.updateTransactionNotes(anyString(), anyString()))
                .thenReturn(Completable.error(new Throwable()));
        // Act
        subject.updateTransactionNote("note");
        // Assert
        verify(payloadDataManager).updateTransactionNotes("hash", "note");
        //noinspection WrongConstant
        verify(activity).showToast(R.string.unexpected_error, ToastCustom.TYPE_ERROR);
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateTransactionNoteBchSuccess() throws Exception {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getHash()).thenReturn("hash");
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BCH);
        subject.displayable = displayable;
        when(ethDataManager.updateTransactionNotes(anyString(), anyString()))
                .thenReturn(Completable.complete());
        // Act
        subject.updateTransactionNote("note");
        // Assert
    }

    @Test
    public void getTransactionNoteBtc() throws Exception {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getHash()).thenReturn("hash");
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        subject.displayable = displayable;
        when(payloadDataManager.getTransactionNotes("hash")).thenReturn("note");
        // Act
        String value = subject.getTransactionNote();
        // Assert
        assertEquals("note", value);
        verify(payloadDataManager).getTransactionNotes("hash");
    }

    @Test
    public void getTransactionNoteEth() throws Exception {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getHash()).thenReturn("hash");
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.ETHER);
        subject.displayable = displayable;
        when(ethDataManager.getTransactionNotes("hash")).thenReturn("note");
        // Act
        String value = subject.getTransactionNote();
        // Assert
        assertEquals("note", value);
        verify(ethDataManager).getTransactionNotes("hash");
    }

    @Test
    public void getTransactionNoteBch() throws Exception {
        // Arrange
        Displayable displayable = mock(BchDisplayable.class);
        when(displayable.getHash()).thenReturn("hash");
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BCH);
        subject.displayable = displayable;
        // Act
        String value = subject.getTransactionNote();
        // Assert
        assertEquals("", value);
    }

    @Test
    public void getTransactionHash() throws Exception {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getHash()).thenReturn("hash");
        subject.displayable = displayable;
        // Act
        String value = subject.getTransactionHash();
        // Assert
        assertEquals("hash", value);
    }

    @Test
    public void setTransactionStatusNoConfirmations() {
        // Arrange
        when(stringUtils.getString(R.string.transaction_detail_pending))
                .thenReturn("Pending (%1$s/%2$s Confirmations)");
        // Act
        subject.setConfirmationStatus(CryptoCurrencies.ETHER, "hash", 0);
        // Assert
        verify(activity).setStatus(CryptoCurrencies.ETHER, "Pending (0/12 Confirmations)", "hash");
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void setTransactionStatusConfirmed() {
        // Arrange
        when(stringUtils.getString(R.string.transaction_detail_confirmed)).thenReturn("Confirmed");
        // Act
        subject.setConfirmationStatus(CryptoCurrencies.BTC, "hash", 3);
        // Assert
        verify(activity).setStatus(CryptoCurrencies.BTC, "Confirmed", "hash");
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void setTransactionColorMove() {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getConfirmations()).thenReturn(0);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        when(displayable.getDirection()).thenReturn(TransactionSummary.Direction.TRANSFERRED);
        // Act
        subject.setTransactionColor(displayable);
        // Assert
        verify(activity).setTransactionColour(R.color.product_gray_transferred_50);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void setTransactionColorMoveConfirmed() {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getConfirmations()).thenReturn(3);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        when(displayable.getDirection()).thenReturn(TransactionSummary.Direction.TRANSFERRED);
        // Act
        subject.setTransactionColor(displayable);
        // Assert
        verify(activity).setTransactionColour(R.color.product_gray_transferred);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void setTransactionColorSent() {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getConfirmations()).thenReturn(2);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        when(displayable.getDirection()).thenReturn(TransactionSummary.Direction.SENT);
        // Act
        subject.setTransactionColor(displayable);
        // Assert
        verify(activity).setTransactionColour(R.color.product_red_sent_50);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void setTransactionColorSentConfirmed() {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getConfirmations()).thenReturn(3);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        when(displayable.getDirection()).thenReturn(TransactionSummary.Direction.SENT);
        // Act
        subject.setTransactionColor(displayable);
        // Assert
        verify(activity).setTransactionColour(R.color.product_red_sent);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void setTransactionColorReceived() {
        // Arrange
        Displayable displayable = mock(EthDisplayable.class);
        when(displayable.getConfirmations()).thenReturn(7);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.ETHER);
        when(displayable.getDirection()).thenReturn(TransactionSummary.Direction.RECEIVED);
        // Act
        subject.setTransactionColor(displayable);
        // Assert
        verify(activity).setTransactionColour(R.color.product_green_received_50);
        verifyNoMoreInteractions(activity);
    }

    @Test
    public void setTransactionColorReceivedConfirmed() {
        // Arrange
        Displayable displayable = mock(BtcDisplayable.class);
        when(displayable.getConfirmations()).thenReturn(3);
        when(displayable.getCryptoCurrency()).thenReturn(CryptoCurrencies.BTC);
        // Act
        subject.setTransactionColor(displayable);
        // Assert
        verify(activity).setTransactionColour(R.color.product_green_received);
        verifyNoMoreInteractions(activity);
    }

}
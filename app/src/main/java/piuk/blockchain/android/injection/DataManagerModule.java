package piuk.blockchain.android.injection;

import android.content.Context;

import info.blockchain.api.statistics.Statistics;
import info.blockchain.wallet.BlockchainFramework;
import info.blockchain.wallet.api.FeeApi;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.ethereum.EthAccountApi;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.util.PrivateKeyFactory;

import dagger.Module;
import dagger.Provides;
import io.reactivex.subjects.ReplaySubject;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.auth.AuthDataManager;
import piuk.blockchain.android.data.auth.AuthService;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.charts.ChartsDataManager;
import piuk.blockchain.android.data.contacts.ContactsDataManager;
import piuk.blockchain.android.data.contacts.ContactsService;
import piuk.blockchain.android.data.contacts.datastore.ContactsMapStore;
import piuk.blockchain.android.data.currency.CurrencyState;
import piuk.blockchain.android.data.datamanagers.FeeDataManager;
import piuk.blockchain.android.data.datamanagers.PromptManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.data.ethereum.EthDataManager;
import piuk.blockchain.android.data.ethereum.EthDataStore;
import piuk.blockchain.android.data.exchange.BuyConditions;
import piuk.blockchain.android.data.exchange.BuyDataManager;
import piuk.blockchain.android.data.exchange.ExchangeService;
import piuk.blockchain.android.data.fingerprint.FingerprintAuthImpl;
import piuk.blockchain.android.data.payload.PayloadDataManager;
import piuk.blockchain.android.data.payload.PayloadService;
import piuk.blockchain.android.data.payments.PaymentService;
import piuk.blockchain.android.data.payments.SendDataManager;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.settings.SettingsDataManager;
import piuk.blockchain.android.data.settings.SettingsService;
import piuk.blockchain.android.data.settings.datastore.SettingsDataStore;
import piuk.blockchain.android.data.stores.PendingTransactionListStore;
import piuk.blockchain.android.data.stores.TransactionListStore;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.ui.transactions.TransactionHelper;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.BackupWalletUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

@SuppressWarnings("WeakerAccess")
@Module
public class DataManagerModule {

    @Provides
    @PresenterScope
    protected AuthDataManager provideAuthDataManager(PrefsUtil prefsUtil,
                                                     AppUtil appUtil,
                                                     AccessState accessState,
                                                     AESUtilWrapper aesUtilWrapper,
                                                     RxBus rxBus) {
        return new AuthDataManager(
                prefsUtil,
                new AuthService(new WalletApi()),
                appUtil,
                accessState,
                aesUtilWrapper,
                rxBus);
    }

    @Provides
    @PresenterScope
    protected QrCodeDataManager provideQrDataManager() {
        return new QrCodeDataManager();
    }

    @Provides
    @PresenterScope
    protected WalletAccountHelper provideWalletAccountHelper(PayloadManager payloadManager,
                                                             PrefsUtil prefsUtil,
                                                             StringUtils stringUtils,
                                                             ExchangeRateFactory exchangeRateFactory,
                                                             CurrencyState currencyState,
                                                             EthDataStore ethDataStore) {
        return new WalletAccountHelper(payloadManager,
                stringUtils,
                prefsUtil,
                exchangeRateFactory,
                currencyState,
                ethDataStore);
    }

    @Provides
    @PresenterScope
    protected TransactionListDataManager provideTransactionListDataManager(PayloadManager payloadManager,
                                                                           EthDataManager ethDataManager,
                                                                           TransactionListStore transactionListStore) {
        return new TransactionListDataManager(
                payloadManager,
                ethDataManager,
                transactionListStore);
    }

    @Provides
    @PresenterScope
    protected TransferFundsDataManager provideTransferFundsDataManager(PayloadDataManager payloadDataManager,
                                                                       SendDataManager sendDataManager,
                                                                       DynamicFeeCache dynamicFeeCache) {
        return new TransferFundsDataManager(payloadDataManager, sendDataManager, dynamicFeeCache);
    }

    @Provides
    @PresenterScope
    protected PayloadDataManager providePayloadDataManager(PayloadManager payloadManager,
                                                           PrivateKeyFactory privateKeyFactory,
                                                           RxBus rxBus) {
        return new PayloadDataManager(new PayloadService(payloadManager), privateKeyFactory, payloadManager, rxBus);
    }

    @Provides
    @PresenterScope
    protected FingerprintHelper provideFingerprintHelper(Context applicationContext,
                                                         PrefsUtil prefsUtil) {
        return new FingerprintHelper(applicationContext, prefsUtil, new FingerprintAuthImpl());
    }

    @Provides
    @PresenterScope
    protected SettingsDataManager provideSettingsDataManager(SettingsService settingsService,
                                                             SettingsDataStore settingsDataStore,
                                                             RxBus rxBus) {
        return new SettingsDataManager(settingsService, settingsDataStore, rxBus);
    }

    @Provides
    @PresenterScope
    protected EthDataManager provideEthDataManager(PayloadManager payloadManager,
                                                   EthDataStore ethDataStore,
                                                   RxBus rxBus) {
        return new EthDataManager(payloadManager, new EthAccountApi(), ethDataStore, rxBus);
    }

    @Provides
    @PresenterScope
    protected SwipeToReceiveHelper provideSwipeToReceiveHelper(PayloadDataManager payloadDataManager,
                                                               PrefsUtil prefsUtil) {
        return new SwipeToReceiveHelper(payloadDataManager, prefsUtil);
    }

    @Provides
    @PresenterScope
    protected SendDataManager provideSendDataManager(RxBus rxBus) {
        return new SendDataManager(new PaymentService(new Payment()), rxBus);
    }

    @Provides
    @PresenterScope
    protected TransactionHelper provideTransactionHelper(PayloadDataManager payloadDataManager) {
        return new TransactionHelper(payloadDataManager);
    }

    @Provides
    @PresenterScope
    protected BuyDataManager provideBuyDataManager(SettingsDataManager settingsDataManager,
                                                   AuthDataManager authDataManager,
                                                   PayloadDataManager payloadDataManager,
                                                   ExchangeService exchangeService) {
        return new BuyDataManager(settingsDataManager,
                authDataManager,
                payloadDataManager,
                BuyConditions.getInstance(
                        ReplaySubject.create(1),
                        ReplaySubject.create(1),
                        ReplaySubject.create(1)),
                exchangeService);
    }

    @Provides
    @PresenterScope
    protected FeeDataManager provideFeeDataManager(RxBus rxBus) {
        return new FeeDataManager(new FeeApi(), rxBus);
    }

    @Provides
    @PresenterScope
    protected PromptManager providePromptManager(PrefsUtil prefsUtil,
                                                 PayloadDataManager payloadDataManager,
                                                 TransactionListDataManager transactionListDataManager) {
        return new PromptManager(prefsUtil, payloadDataManager, transactionListDataManager);
    }

    @Provides
    @PresenterScope
    protected BackupWalletUtil provideBackupWalletUtil(PayloadDataManager payloadDataManager) {
        return new BackupWalletUtil(payloadDataManager);
    }

    @Provides
    @PresenterScope
    protected ContactsDataManager provideContactsManager(ContactsService contactsService,
                                                         ContactsMapStore contactsMapStore,
                                                         PendingTransactionListStore pendingTransactionListStore,
                                                         RxBus rxBus) {
        return new ContactsDataManager(
                contactsService,
                contactsMapStore,
                pendingTransactionListStore,
                rxBus);
    }

    @Provides
    @PresenterScope
    protected ChartsDataManager provideChartsDataManager(RxBus rxBus) {
        return new ChartsDataManager(new Statistics(
                BlockchainFramework.getRetrofitApiInstance(),
                BlockchainFramework.getApiCode()),
                rxBus);
    }

}
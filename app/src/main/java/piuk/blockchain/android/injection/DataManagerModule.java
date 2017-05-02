package piuk.blockchain.android.injection;

import android.content.Context;

import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.settings.SettingsManager;
import info.blockchain.wallet.util.PrivateKeyFactory;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.access.AccessState;
import piuk.blockchain.android.data.cache.DynamicFeeCache;
import piuk.blockchain.android.data.datamanagers.AccountDataManager;
import piuk.blockchain.android.data.datamanagers.AccountEditDataManager;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.OnboardingDataManager;
import piuk.blockchain.android.data.datamanagers.PayloadDataManager;
import piuk.blockchain.android.data.datamanagers.QrCodeDataManager;
import piuk.blockchain.android.data.datamanagers.SendDataManager;
import piuk.blockchain.android.data.datamanagers.SettingsDataManager;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.data.datamanagers.BuyDataManager;
import piuk.blockchain.android.data.fingerprint.FingerprintAuthImpl;
import piuk.blockchain.android.data.rxjava.RxBus;
import piuk.blockchain.android.data.services.PayloadService;
import piuk.blockchain.android.data.services.PaymentService;
import piuk.blockchain.android.data.services.SettingsService;
import piuk.blockchain.android.data.services.WalletService;
import piuk.blockchain.android.data.stores.TransactionListStore;
import piuk.blockchain.android.ui.fingerprint.FingerprintHelper;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.ui.swipetoreceive.SwipeToReceiveHelper;
import piuk.blockchain.android.ui.transactions.TransactionHelper;
import piuk.blockchain.android.util.AESUtilWrapper;
import piuk.blockchain.android.util.AppUtil;
import piuk.blockchain.android.util.ExchangeRateFactory;
import piuk.blockchain.android.util.PrefsUtil;
import piuk.blockchain.android.util.StringUtils;

@SuppressWarnings("WeakerAccess")
@Module
public class DataManagerModule {

    @Provides
    @ViewModelScope
    protected AuthDataManager provideAuthDataManager(PayloadDataManager payloadDataManager,
                                                     PrefsUtil prefsUtil,
                                                     AppUtil appUtil,
                                                     AccessState accessState,
                                                     StringUtils stringUtils,
                                                     AESUtilWrapper aesUtilWrapper,
                                                     RxBus rxBus) {
        return new AuthDataManager(
                payloadDataManager,
                prefsUtil,
                new WalletService(new WalletApi()),
                appUtil,
                accessState,
                stringUtils,
                aesUtilWrapper,
                rxBus);
    }

    @Provides
    @ViewModelScope
    protected QrCodeDataManager provideQrDataManager() {
        return new QrCodeDataManager();
    }

    @Provides
    @ViewModelScope
    protected WalletAccountHelper provideWalletAccountHelper(PayloadManager payloadManager,
                                                             PrefsUtil prefsUtil,
                                                             StringUtils stringUtils,
                                                             ExchangeRateFactory exchangeRateFactory) {
        return new WalletAccountHelper(payloadManager, prefsUtil, stringUtils, exchangeRateFactory);
    }

    @Provides
    @ViewModelScope
    protected TransactionListDataManager provideTransactionListDataManager(PayloadManager payloadManager,
                                                                           TransactionListStore transactionListStore,
                                                                           RxBus rxBus) {
        return new TransactionListDataManager(
                payloadManager,
                transactionListStore,
                rxBus);
    }

    @Provides
    @ViewModelScope
    protected TransferFundsDataManager provideTransferFundsDataManager(PayloadDataManager payloadDataManager,
                                                                       SendDataManager sendDataManager,
                                                                       DynamicFeeCache dynamicFeeCache) {
        return new TransferFundsDataManager(payloadDataManager, sendDataManager, dynamicFeeCache);
    }

    @Provides
    @ViewModelScope
    protected PayloadDataManager providePayloadDataManager(PayloadManager payloadManager, RxBus rxBus) {
        return new PayloadDataManager(new PayloadService(payloadManager), payloadManager, rxBus);
    }

    @Provides
    @ViewModelScope
    protected AccountDataManager provideAccountDataManager(PayloadManager payloadManager,
                                                           PrivateKeyFactory privateKeyFactory,
                                                           RxBus rxBus) {
        return new AccountDataManager(new PayloadService(payloadManager), privateKeyFactory, rxBus);
    }

    @Provides
    @ViewModelScope
    protected FingerprintHelper provideFingerprintHelper(Context applicationContext,
                                                         PrefsUtil prefsUtil) {
        return new FingerprintHelper(applicationContext, prefsUtil, new FingerprintAuthImpl());
    }

    @Provides
    @ViewModelScope
    protected SettingsDataManager provideSettingsDataManager(RxBus rxBus) {
        return new SettingsDataManager(new SettingsService(new SettingsManager()), rxBus);
    }

    @Provides
    @ViewModelScope
    protected AccountEditDataManager provideAccountEditDataManager(PayloadDataManager payloadDataManager,
                                                                   SendDataManager sendDataManager,
                                                                   DynamicFeeCache dynamicFeeCache) {
        return new AccountEditDataManager(
                payloadDataManager,
                sendDataManager,
                dynamicFeeCache);
    }

    @Provides
    @ViewModelScope
    protected SwipeToReceiveHelper provideSwipeToReceiveHelper(PayloadDataManager payloadDataManager,
                                                               PrefsUtil prefsUtil) {
        return new SwipeToReceiveHelper(payloadDataManager, prefsUtil);
    }

    @Provides
    @ViewModelScope
    protected SendDataManager provideSendDataManager(RxBus rxBus) {
        return new SendDataManager(new PaymentService(new Payment()), rxBus);
    }

    @Provides
    @ViewModelScope
    protected TransactionHelper provideTransactionHelper(PayloadDataManager payloadDataManager) {
        return new TransactionHelper(payloadDataManager);
    }

    @Provides
    @ViewModelScope
    protected OnboardingDataManager provideOnboardingDataManager(SettingsDataManager settingsDataManager,
                                                                 AuthDataManager authDataManager,
                                                                 PayloadDataManager payloadDataManager,
                                                                 AccessState accessState) {
        return new OnboardingDataManager(
                settingsDataManager,
                authDataManager,
                payloadDataManager,
                accessState);
    }

    @Provides
    @ViewModelScope
    protected BuyDataManager provideBuyDataManager(OnboardingDataManager onboardingDataManager,
                                                   SettingsDataManager settingsDataManager,
                                                   PayloadDataManager payloadDataManager) {
        return BuyDataManager.getInstance(
                onboardingDataManager,
                settingsDataManager,
                payloadDataManager);
    }
}

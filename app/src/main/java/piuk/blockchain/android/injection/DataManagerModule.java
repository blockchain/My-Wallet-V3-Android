package piuk.blockchain.android.injection;

import info.blockchain.api.AddressInfo;
import info.blockchain.api.Unspent;
import info.blockchain.wallet.multiaddr.MultiAddrFactory;
import info.blockchain.wallet.payload.PayloadManager;
import info.blockchain.wallet.payment.Payment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.data.datamanagers.AccountDataManager;
import piuk.blockchain.android.data.datamanagers.AuthDataManager;
import piuk.blockchain.android.data.datamanagers.ReceiveDataManager;
import piuk.blockchain.android.data.datamanagers.TransactionListDataManager;
import piuk.blockchain.android.data.datamanagers.TransferFundsDataManager;
import piuk.blockchain.android.data.services.AddressInfoService;
import piuk.blockchain.android.ui.receive.WalletAccountHelper;
import piuk.blockchain.android.ui.transactions.TransactionHelper;

/**
 * Created by adambennett on 12/08/2016.
 */

@Module
public class DataManagerModule {

    @Provides
    @Singleton
    protected AuthDataManager provideAuthDataManager() {
        return new AuthDataManager();
    }

    @Provides
    @Singleton
    protected ReceiveDataManager provideReceiveDataManager() {
        return new ReceiveDataManager();
    }

    // TODO: 01/09/2016 This needs to move to a more appropriate place once we've restructured the app
    @Provides
    protected WalletAccountHelper provideWalletAccountHelper() {
        return new WalletAccountHelper();
    }

    @Provides
    @Singleton
    protected TransactionListDataManager provideTransactionListDataManager(PayloadManager payloadManager) {
        return new TransactionListDataManager(payloadManager);
    }

    @Provides
    @Singleton
    protected TransferFundsDataManager provideTransferFundsDataManager(PayloadManager payloadManager) {
        return new TransferFundsDataManager(payloadManager, new Unspent(), new Payment());
    }

    @Provides
    protected TransactionHelper provideTransactionHelper(PayloadManager payloadManager) {
        return new TransactionHelper(payloadManager);
    }

    @Provides
    @Singleton
    protected AccountDataManager provideAccountDataManager(PayloadManager payloadManager, MultiAddrFactory multiAddrFactory) {
        return new AccountDataManager(payloadManager, multiAddrFactory, new AddressInfoService(new AddressInfo()));
    }
}

package piuk.blockchain.android.injection;

import com.blockchain.koin.KoinDaggerModule;
import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.ui.chooser.AccountListing;
import piuk.blockchain.androidcore.data.bitcoincash.BchDataStore;
import piuk.blockchain.androidcore.data.contacts.datastore.ContactsMapStore;
import piuk.blockchain.androidcore.data.contacts.datastore.PendingTransactionListStore;
import piuk.blockchain.androidcore.data.ethereum.datastores.EthDataStore;
import piuk.blockchain.androidcore.data.exchangerate.datastore.ExchangeRateDataStore;
import piuk.blockchain.androidcore.data.rxjava.RxBus;
import piuk.blockchain.androidcore.data.walletoptions.WalletOptionsState;

@Module
public class PresenterModule extends KoinDaggerModule {

    @Provides
    protected AccountListing provideAccountListing() {
        return get(AccountListing.class);
    }

}

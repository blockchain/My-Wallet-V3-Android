package piuk.blockchain.android.injection;

import dagger.Module;
import dagger.Provides;
import piuk.blockchain.android.ui.chooser.AccountListing;
import piuk.blockchain.android.ui.chooser.WalletAccountHelperAccountListingAdapter;

@Module
public class PresenterModule {

    @Provides
    protected AccountListing provideAccountListing(WalletAccountHelperAccountListingAdapter walletAccount) {
        return walletAccount;
    }

}

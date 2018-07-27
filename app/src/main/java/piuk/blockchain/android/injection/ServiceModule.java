package piuk.blockchain.android.injection;

import dagger.Module;
import dagger.Provides;
import info.blockchain.wallet.api.FeeApi;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.WalletExplorerEndpoints;
import info.blockchain.wallet.contacts.Contacts;
import info.blockchain.wallet.ethereum.EthAccountApi;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.prices.PriceApi;
import info.blockchain.wallet.settings.SettingsManager;
import info.blockchain.wallet.shapeshift.ShapeShiftApi;
import info.blockchain.wallet.shapeshift.ShapeShiftEndpoints;
import piuk.blockchain.android.data.fingerprint.FingerprintAuth;
import piuk.blockchain.android.data.fingerprint.FingerprintAuthImpl;
import retrofit2.Retrofit;

import javax.inject.Named;
import javax.inject.Singleton;

@Module
class ServiceModule {

    @Provides
    @Singleton
    SettingsManager provideSettingsManager(WalletApi walletApi) {
        return new SettingsManager(walletApi);
    }

    @Provides
    @Singleton
    Contacts provideContacts() {
        return new Contacts();
    }

    @Provides
    @Singleton
    WalletExplorerEndpoints provideWalletExplorerEndpoints(@Named("explorer") Retrofit retrofit) {
        return retrofit.create(WalletExplorerEndpoints.class);
    }

    @Provides
    @Singleton
    ShapeShiftEndpoints provideShapeShiftEndpoints(@Named("shapeshift") Retrofit retrofit) {
        return retrofit.create(ShapeShiftEndpoints.class);
    }

    @Provides
    WalletApi provideWalletApi(WalletExplorerEndpoints walletExplorerEndpoints) {
        return new WalletApi(walletExplorerEndpoints);
    }

    @Provides
    Payment providePayment() {
        return new Payment();
    }

    @Provides
    FeeApi provideFeeApi() {
        return new FeeApi();
    }

    @Provides
    PriceApi providePriceApi() {
        return new PriceApi();
    }

    @Provides
    ShapeShiftApi provideShapeShiftApi(ShapeShiftEndpoints shapeShiftEndpoints) {
        return new ShapeShiftApi(shapeShiftEndpoints);
    }

    @Provides
    FingerprintAuth provideFingerprintAuth() {
        return new FingerprintAuthImpl();
    }

    @Provides
    EthAccountApi provideEthAccountApi() {
        return new EthAccountApi();
    }

}

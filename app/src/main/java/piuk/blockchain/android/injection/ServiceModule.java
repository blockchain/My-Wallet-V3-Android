package piuk.blockchain.android.injection;

import com.blockchain.koin.KoinDaggerModule;
import dagger.Module;
import dagger.Provides;
import info.blockchain.wallet.ApiCode;
import info.blockchain.wallet.api.FeeApi;
import info.blockchain.wallet.api.WalletApi;
import info.blockchain.wallet.api.WalletExplorerEndpoints;
import info.blockchain.wallet.contacts.Contacts;
import info.blockchain.wallet.ethereum.EthAccountApi;
import info.blockchain.wallet.payment.Payment;
import info.blockchain.wallet.prices.PriceApi;
import info.blockchain.wallet.prices.PriceEndpoints;
import info.blockchain.wallet.settings.SettingsManager;
import info.blockchain.wallet.shapeshift.ShapeShiftApi;
import info.blockchain.wallet.shapeshift.ShapeShiftEndpoints;
import piuk.blockchain.android.data.fingerprint.FingerprintAuth;

@Module
class ServiceModule extends KoinDaggerModule {

    @Provides
    SettingsManager provideSettingsManager() {
        return get(SettingsManager.class);
    }

    @Provides
    Contacts provideContacts() {
        return get(Contacts.class);
    }

    @Provides
    WalletExplorerEndpoints provideWalletExplorerEndpoints() {
        return get(WalletExplorerEndpoints.class);
    }

    @Provides
    ShapeShiftEndpoints provideShapeShiftEndpoints() {
        return get(ShapeShiftEndpoints.class);
    }

    @Provides
    PriceEndpoints providePriceEndpoints() {
        return get(PriceEndpoints.class);
    }

    @Provides
    WalletApi provideWalletApi() {
        return get(WalletApi.class);
    }

    @Provides
    Payment providePayment() {
        return get(Payment.class);
    }

    @Provides
    FeeApi provideFeeApi() {
        return get(FeeApi.class);
    }

    @Provides
    ApiCode provideApiCode() {
        return get(ApiCode.class);
    }

    @Provides
    PriceApi providePriceApi() {
        return get(PriceApi.class);
    }

    @Provides
    ShapeShiftApi provideShapeShiftApi() {
        return get(ShapeShiftApi.class);
    }

    @Provides
    FingerprintAuth provideFingerprintAuth() {
        return get(FingerprintAuth.class);
    }

    @Provides
    EthAccountApi provideEthAccountApi() {
        return get(EthAccountApi.class);
    }
}

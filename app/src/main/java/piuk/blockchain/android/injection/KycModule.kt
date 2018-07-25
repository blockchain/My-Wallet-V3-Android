package piuk.blockchain.android.injection

import com.blockchain.kyc.DisabledFeatureFlag
import com.blockchain.kyc.FeatureFlag
import dagger.Module
import dagger.Provides
import info.blockchain.wallet.api.WalletApi
import piuk.blockchain.android.BuildConfig
import piuk.blockchain.android.kyc.KycServerSideFeatureFlag
import timber.log.Timber

@Module
class KycModule {

    @Provides
    fun provideFeatureFlag(walletApi: dagger.Lazy<WalletApi>): FeatureFlag =
        if (BuildConfig.HOMEBREW_DISABLED) {
            Timber.w("Homebrew is disabled in build")
            DisabledFeatureFlag()
        } else {
            KycServerSideFeatureFlag(walletApi.get())
        }
}
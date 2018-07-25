package piuk.blockchain.android.kyc

import com.blockchain.kyc.FeatureFlag
import info.blockchain.wallet.api.WalletApi
import io.reactivex.Single

class KycServerSideFeatureFlag(private val walletApi: WalletApi) : FeatureFlag {
    override val enabled: Single<Boolean>
        get() =
            walletApi
                .walletOptions
                .singleOrError()
                .map {
                    it.androidFlags["homebrew"] ?: false
                }
}
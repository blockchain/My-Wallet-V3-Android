package piuk.blockchain.android.kyc.morphmethod

import com.blockchain.koin.modules.MorphMethodType
import com.blockchain.koin.modules.MorphMethodTypeSelector
import com.blockchain.kyc.datamanagers.nabu.NabuDataUserProvider
import com.blockchain.kycui.settings.KycStatusHelper
import com.blockchain.kycui.settings.SettingsKycState
import io.reactivex.Single

internal class TiersMorphMethodTypeSelectorSelector(
    private val kycStatusHelper: KycStatusHelper,
    private val nabuDataUserProvider: NabuDataUserProvider
) : MorphMethodTypeSelector {

    override fun getMorphMethod(): Single<MorphMethodType> {
        return kycStatusHelper.getSettingsKycState()
            .doOnSuccess {
                if (it == SettingsKycState.Hidden) {
                    throw IllegalStateException("Morph method fetched but KYC state is hidden")
                }
            }
            .flatMap {
                nabuDataUserProvider.getUser()
            }
            .map {
                val current = it.tiers?.current
                if (current != null && current > 0) {
                    MorphMethodType.HomeBrew
                } else {
                    MorphMethodType.Kyc
                }
            }
    }
}

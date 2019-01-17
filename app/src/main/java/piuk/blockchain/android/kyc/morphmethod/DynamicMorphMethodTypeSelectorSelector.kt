package piuk.blockchain.android.kyc.morphmethod

import com.blockchain.koin.modules.MorphMethodType
import com.blockchain.koin.modules.MorphMethodTypeSelector
import com.blockchain.kycui.settings.KycStatusHelper
import com.blockchain.kycui.settings.SettingsKycState
import io.reactivex.Single

internal class DynamicMorphMethodTypeSelectorSelector(
    private val kycStatusHelper: KycStatusHelper
) : MorphMethodTypeSelector {

    override fun getMorphMethod(): Single<MorphMethodType> {
        return kycStatusHelper.getSettingsKycState()
            .map {
                when (it) {
                    SettingsKycState.Hidden ->
                        throw IllegalStateException("Morph method fetched but KYC state is hidden")
                    SettingsKycState.Verified -> return@map MorphMethodType.HomeBrew
                    else -> return@map MorphMethodType.Kyc
                }
            }
    }
}

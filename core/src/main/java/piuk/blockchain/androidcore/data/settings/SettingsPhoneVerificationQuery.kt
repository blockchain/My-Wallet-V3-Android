package piuk.blockchain.androidcore.data.settings

import io.reactivex.Single

internal class SettingsPhoneVerificationQuery(
    private val settingsDataManager: SettingsDataManager
) : PhoneVerificationQuery {

    override fun isPhoneNumberVerified(): Single<Boolean> =
        settingsDataManager.fetchSettings()
            .map { it.isSmsVerified }
            .single(false)
}

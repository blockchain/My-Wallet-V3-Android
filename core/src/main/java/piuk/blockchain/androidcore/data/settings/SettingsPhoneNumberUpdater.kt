package piuk.blockchain.androidcore.data.settings

import info.blockchain.wallet.api.data.Settings
import io.reactivex.Observable
import io.reactivex.Single

internal class SettingsPhoneNumberUpdater(
    private val settingsDataManager: SettingsDataManager
) : PhoneNumberUpdater {

    override fun smsNumber(): Single<String> {
        return settingsDataManager.fetchSettings()
            .toJustNumber()
    }

    override fun updateSms(phoneNumber: PhoneNumber): Single<String> {
        return settingsDataManager
            .updateSms(phoneNumber.sanitized)
            .toJustNumber()
    }

    override fun verifySms(code: String): Single<String> {
        return settingsDataManager.verifySms(code)
            .toJustNumber()
    }
}

private fun Observable<Settings>.toJustNumber() =
    map { it.smsNumber ?: "" }
        .single("")
